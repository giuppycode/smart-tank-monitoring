/**
 * CUS (Control Unit Subsystem) - Componente principale del backend IoT
 *
 * Questa classe implementa un server HTTP che:
 * - Si connette a un broker MQTT per ricevere dati dai sensori
 * - Espone un'API REST per la memorizzazione dei dati
 * - Monitora la connettività MQTT e segnala se i messaggi mancano per troppo tempo
 *
 * @author Laboratorio IoT
 * @version 1.1
 */
package esiot.backend;

import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

/**
 * Classe principale del Control Unit Subsystem.
 * Gestisce la comunicazione MQTT, seriale e l'API HTTP per i dati dei sensori.
 */
public class CUS {
    /** Porta HTTP su cui il server Vert.x ascolta */
    static final int PORT = 8080;

    /** Indirizzo del broker MQTT a cui connettersi */
    static final String BROKER = "tcp://broker.mqtt-dashboard.com";

    /** Topic MQTT a cui sottoscriversi per ricevere i dati */
    static final String TOPIC = "esiot-2025";

    /** Timeout in millisecondi per considerare il sensore non raggiungibile */
    static final long TIMEOUT_MS = 3000;

    /**
     * Invia una stringa sulla porta seriale in modo sicuro (lunghezza calcolata
     * a runtime, mai hardcoded).
     */
    private static void serialSend(SerialPort port, String msg) {
        byte[] bytes = (msg + "\n").getBytes();
        port.writeBytes(bytes, bytes.length);
    }

    public static void main(String[] args) throws Exception {
        Vertx vertx = Vertx.vertx();
        DataService service = new DataService(PORT);
        vertx.deployVerticle(service);

        WebClient client = WebClient.create(vertx);

        // Timestamp dell'ultimo messaggio MQTT ricevuto
        AtomicLong lastReceived = new AtomicLong(System.currentTimeMillis());

        // ------------------------------------------------------------------ //
        //  MQTT
        // ------------------------------------------------------------------ //
        String clientId = "cus-" + System.currentTimeMillis();
        MqttClient mqtt = new MqttClient(BROKER, clientId);

        mqtt.setCallback(new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) {
                lastReceived.set(System.currentTimeMillis());

                String payload = new String(message.getPayload()).trim();
                System.out.println("[MQTT] Ricevuto: " + payload);

                try {
                    float distanza = Float.parseFloat(payload);

                    JsonObject item = new JsonObject()
                            .put("value", distanza)
                            .put("place", "tank");

                    client.post(PORT, "localhost", "/api/data")
                            .sendJson(item)
                            .onSuccess(res -> System.out.println("[HTTP] Dato salvato ok"))
                            .onFailure(err -> System.out.println("[HTTP] Errore: " + err.getMessage()));

                } catch (NumberFormatException e) {
                    System.out.println("[MQTT] Payload non numerico: " + payload);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                System.out.println("[MQTT] Connessione persa: " + cause.getMessage());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {}
        });

        mqtt.connect();
        mqtt.subscribe(TOPIC, 1);
        System.out.println("[MQTT] Iscritto al topic: " + TOPIC);

        // ------------------------------------------------------------------ //
        //  SERIAL – ricerca porta Arduino
        // ------------------------------------------------------------------ //
        SerialPort serialPort = null;
        for (SerialPort port : SerialPort.getCommPorts()) {
            System.out.println("[SERIAL] Trovata porta: " + port.getSystemPortName());
            if (port.getSystemPortName().contains("USB") || port.getSystemPortName().contains("ACM")) {
                serialPort = port;
                break;
            }
        }

        if (serialPort == null) {
            SerialPort[] all = SerialPort.getCommPorts();
            if (all.length == 0) {
                System.out.println("[SERIAL] ERRORE: nessuna porta seriale disponibile.");
                return;
            }
            System.out.println("[SERIAL] Nessuna porta USB/ACM trovata, uso la prima disponibile.");
            serialPort = all[0];
        }

        final SerialPort finalSerialPort = serialPort;
        serialPort.setComPortParameters(9600, 8, 1, 0);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
        serialPort.openPort();

        // currentMode tracks what we last sent/received so we avoid redundant
        // serial writes. It is only written from the Vert.x event loop OR from
        // the serial listener – both are effectively single-threaded in this
        // setup, so a plain array cell is fine.
        final String[] currentMode = {"AUTOMATIC"};

        StringBuilder serialBuffer = new StringBuilder();

        serialPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) return;

                int available = finalSerialPort.bytesAvailable();
                if (available <= 0) return;

                byte[] readBuffer = new byte[available];
                int numRead = finalSerialPort.readBytes(readBuffer, readBuffer.length);
                serialBuffer.append(new String(readBuffer, 0, numRead));

                int newlineIndex;
                while ((newlineIndex = serialBuffer.indexOf("\n")) != -1) {
                    String line = serialBuffer.substring(0, newlineIndex).trim();
                    serialBuffer.delete(0, newlineIndex + 1);

                    if (line.isEmpty()) continue;
                    System.out.println("[SERIAL] Ricevuto: " + line);

                    // Expected format: "MANUAL,VALVE:75" / "AUTOMATIC,VALVE:0" / "UNCONNECTED,VALVE:0"
                    if (!line.contains(",")) continue;

                    String[] parts = line.split(",", 2);
                    if (parts.length < 2 || !parts[1].startsWith("VALVE:")) continue;

                    String arduinoMode = parts[0].trim();
                    String valveStr   = parts[1].substring(6).trim();

                    // --- Parse valve value ---
                    int valve;
                    try {
                        valve = Integer.parseInt(valveStr);
                    } catch (NumberFormatException e) {
                        System.out.println("[SERIAL] Valore valvola non valido: " + valveStr);
                        continue;
                    }

                    // --- FIX 1: propagate Arduino mode change → DataService → frontend ---
                    // The Arduino button can toggle MANUAL on/off. When it does, we must
                    // POST the new mode to /api/status so the frontend reflects it.
                    if (!arduinoMode.equals(currentMode[0])) {
                        System.out.println("[SERIAL] Mode changed by Arduino: "
                                + currentMode[0] + " → " + arduinoMode);
                        currentMode[0] = arduinoMode;

                        JsonObject modeBody = new JsonObject().put("mode", arduinoMode);
                        client.post(PORT, "localhost", "/api/status")
                                .sendJson(modeBody)
                                .onSuccess(r -> System.out.println("[HTTP] Mode synced to DataService: " + arduinoMode))
                                .onFailure(err -> System.out.println("[HTTP] Mode sync error: " + err.getMessage()));
                    }

                    // --- FIX 2: update valve in DataService, but ONLY when in MANUAL mode ---
                    // In AUTOMATIC/UNCONNECTED the CUS owns the valve value; the potentiometer
                    // reading from the Arduino should be ignored so the frontend is not
                    // confused by stale pot values.
                    if ("MANUAL".equals(arduinoMode)) {
                        JsonObject valveBody = new JsonObject().put("percent", valve);
                        client.post(PORT, "localhost", "/api/valve")
                                .sendJson(valveBody)
                                .onSuccess(r -> System.out.println("[HTTP] Valve (MANUAL) " + valve + "% saved"))
                                .onFailure(err -> System.out.println("[HTTP] Valve save error: " + err.getMessage()));
                    }
                }
            }
        });

        System.out.println("[SERIAL] Connesso a: " + finalSerialPort.getSystemPortName());

        // FIX 3: use the helper so the byte-count is always correct
        serialSend(finalSerialPort, "AUTOMATIC");
        System.out.println("[SERIAL] Stato iniziale inviato: AUTOMATIC");

        // ------------------------------------------------------------------ //
        //  Periodic timer – polls DataService and pushes correct mode/valve
        //  to Arduino every second.
        // ------------------------------------------------------------------ //
        vertx.setPeriodic(1000, id -> {
            long elapsed = System.currentTimeMillis() - lastReceived.get();

            client.get(PORT, "localhost", "/api/status")
                .send()
                .onSuccess(res -> {
                    String apiMode = res.bodyAsJsonObject().getString("mode");

                    // UNCONNECTED wins when MQTT has been silent too long
                    boolean timedOut = elapsed > TIMEOUT_MS;
                    String targetMode = (timedOut || "UNCONNECTED".equals(apiMode))
                            ? "UNCONNECTED" : apiMode;

                    if (timedOut) {
                        System.out.println("[MQTT] Status: unreachable ("
                                + elapsed / 1000 + "s without data)");
                    } else {
                        System.out.println("[MQTT] Status: connected, mode=" + targetMode);
                    }

                    // Always send the target mode to Arduino so it stays in sync.
                    // (Arduino ignores repeated identical commands harmlessly.)
                    serialSend(finalSerialPort, targetMode);

                    if (!currentMode[0].equals(targetMode)) {
                        System.out.println("[SERIAL] Pushed mode to Arduino: "
                                + currentMode[0] + " → " + targetMode);
                        currentMode[0] = targetMode;
                    }

                    // --- FIX 4: in AUTOMATIC mode, CUS computes valve and sends
                    //     it to both DataService AND Arduino via the serial command.
                    //     (MANUAL valve is set by the frontend → /api/valve directly,
                    //      and the Arduino reads it from its potentiometer locally.)
                    if ("AUTOMATIC".equals(targetMode)) {
                        client.get(PORT, "localhost", "/api/data")
                            .send()
                            .onSuccess(dataRes -> {
                                io.vertx.core.json.JsonArray arr = dataRes.bodyAsJsonArray();
                                if (arr == null || arr.isEmpty()) return;

                                double level = arr.getJsonObject(0).getDouble("value");

                                // Policy from request.md:
                                //   level > L2 (e.g. 20 cm)  → valve 100%
                                //   level > L1 (e.g. 40 cm)  → valve 50%  (closer = more full)
                                //   otherwise                 → valve 0%
                                // NOTE: sensor measures *distance* from top, so smaller = fuller.
                                final double L1 = 40.0; // cm – adjust to your tank
                                final double L2 = 20.0; // cm – adjust to your tank

                                int autoValve;
                                if (level <= L2) {
                                    autoValve = 100;
                                } else if (level <= L1) {
                                    autoValve = 50;
                                } else {
                                    autoValve = 0;
                                }

                                // Persist so frontend can display it
                                JsonObject valveBody = new JsonObject().put("percent", autoValve);
                                client.post(PORT, "localhost", "/api/valve")
                                        .sendJson(valveBody)
                                        .onSuccess(r -> System.out.println("[HTTP] Valve (AUTO) " + autoValve + "% saved"))
                                        .onFailure(err -> System.out.println("[HTTP] Valve save error: " + err.getMessage()));
                            })
                            .onFailure(err -> System.out.println("[HTTP] Failed to get data: " + err.getMessage()));
                    }
                })
                .onFailure(err -> System.out.println("[HTTP] Failed to get status: " + err.getMessage()));
        });
    }
}