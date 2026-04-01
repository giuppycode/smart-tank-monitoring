/**
 * CUS (Control Unit Subsystem) - Componente principale del backend IoT
 *
 * @version 1.3 - serial based on aricci's SerialCommChannel pattern
 */
package esiot.backend;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

public class CUS {

    static final int PORT = 8080;
    static final String BROKER = "tcp://broker.mqtt-dashboard.com";
    static final String TOPIC = "esiot-2025";
    static final long TIMEOUT_MS = 3000;

    // Serial state — same pattern as SerialCommChannel
    private static SerialPort serialPort;
    private static BlockingQueue<String> serialQueue = new ArrayBlockingQueue<>(100);
    private static StringBuffer currentMsg = new StringBuffer("");

    /**
     * Send a message to Arduino — exact same impl as SerialCommChannel.sendMsg()
     */
    private static void serialSend(String msg) {
        char[] array = (msg + "\n").toCharArray();
        byte[] bytes = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            bytes[i] = (byte) array[i];
        }
        try {
            synchronized (serialPort) {
                serialPort.writeBytes(bytes);
            }
            System.out.println("[SERIAL] Sent: '" + msg + "'");
        } catch (Exception ex) {
            System.out.println("[SERIAL] Send error: " + ex.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        Vertx vertx = Vertx.vertx();
        DataService service = new DataService(PORT);
        vertx.deployVerticle(service);

        WebClient client = WebClient.create(vertx);
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
        //  SERIAL – port discovery
        // ------------------------------------------------------------------ //
        String[] portNames = SerialPortList.getPortNames();

        if (portNames.length == 0) {
            System.out.println("[SERIAL] ERRORE: nessuna porta seriale trovata.");
            return;
        }

        String chosenPort = null;
        for (String name : portNames) {
            System.out.println("[SERIAL] Trovata porta: " + name);
            if (name.contains("USB") || name.contains("ACM") || name.contains("ttyUSB") || name.contains("ttyACM")) {
                chosenPort = name;
                break;
            }
        }
        if (chosenPort == null) {
            chosenPort = portNames[0];
            System.out.println("[SERIAL] Nessuna porta USB/ACM trovata, uso: " + chosenPort);
        }

        // Open port — exact same sequence as SerialCommChannel constructor
        serialPort = new SerialPort(chosenPort);
        serialPort.openPort();
        serialPort.setParams(
            SerialPort.BAUDRATE_9600,
            SerialPort.DATABITS_8,
            SerialPort.STOPBITS_1,
            SerialPort.PARITY_NONE
        );
        serialPort.setFlowControlMode(
            SerialPort.FLOWCONTROL_RTSCTS_IN |
            SerialPort.FLOWCONTROL_RTSCTS_OUT
        );
        // addEventListener without mask — same as working project
        serialPort.addEventListener(new SerialPortEventListener() {
            @Override
            public void serialEvent(SerialPortEvent event) {
                if (!event.isRXCHAR()) return;
                try {
                    String msg = serialPort.readString(event.getEventValue());

                    // Strip \r — same as SerialCommChannel
                    msg = msg.replaceAll("\r", "");
                    currentMsg.append(msg);

                    boolean goAhead = true;
                    while (goAhead) {
                        String buf = currentMsg.toString();
                        int index = buf.indexOf("\n");
                        if (index >= 0) {
                            serialQueue.put(buf.substring(0, index));
                            currentMsg = new StringBuffer("");
                            if (index + 1 < buf.length()) {
                                currentMsg.append(buf.substring(index + 1));
                            }
                        } else {
                            goAhead = false;
                        }
                    }
                } catch (Exception ex) {
                    System.out.println("[SERIAL] Read error: " + ex.getMessage());
                }
            }
        });

        System.out.println("[SERIAL] Connesso a: " + chosenPort);

        final String[] currentMode = {"AUTOMATIC"};

        // ------------------------------------------------------------------ //
        //  Serial reader thread — drains the queue and processes lines,
        //  same blocking pattern as SerialCommChannel.receiveMsg()
        // ------------------------------------------------------------------ //
        Thread serialReader = new Thread(() -> {
            while (true) {
                try {
                    String line = serialQueue.take(); // blocks until a line arrives
                    if (line.isEmpty()) continue;
                    System.out.println("[SERIAL] Ricevuto: " + line);

                    // Expected format: "MANUAL,VALVE:75"
                    if (!line.contains(",")) continue;
                    String[] parts = line.split(",", 2);
                    if (parts.length < 2 || !parts[1].startsWith("VALVE:")) continue;

                    String arduinoMode = parts[0].trim();
                    String valveStr = parts[1].substring(6).trim();

                    int valve;
                    try {
                        valve = Integer.parseInt(valveStr);
                    } catch (NumberFormatException e) {
                        System.out.println("[SERIAL] Valore valvola non valido: " + valveStr);
                        continue;
                    }

                    // Propagate Arduino mode change → DataService → frontend
                    if (!arduinoMode.equals(currentMode[0])) {
                        System.out.println("[SERIAL] Mode changed by Arduino: "
                                + currentMode[0] + " → " + arduinoMode);
                        currentMode[0] = arduinoMode;

                        JsonObject modeBody = new JsonObject().put("mode", arduinoMode);
                        client.post(PORT, "localhost", "/api/status")
                                .sendJson(modeBody)
                                .onSuccess(r -> System.out.println("[HTTP] Mode synced: " + arduinoMode))
                                .onFailure(err -> System.out.println("[HTTP] Mode sync error: " + err.getMessage()));
                    }

                    // Update valve only when Arduino is in MANUAL
                    if ("MANUAL".equals(arduinoMode)) {
                        JsonObject valveBody = new JsonObject().put("percent", valve);
                        client.post(PORT, "localhost", "/api/valve")
                                .sendJson(valveBody)
                                .onSuccess(r -> System.out.println("[HTTP] Valve (MANUAL) " + valve + "% saved"))
                                .onFailure(err -> System.out.println("[HTTP] Valve save error: " + err.getMessage()));
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        serialReader.setDaemon(true);
        serialReader.start();

        // Send initial mode
        serialSend("AUTOMATIC");

        // ------------------------------------------------------------------ //
        //  Periodic timer
        // ------------------------------------------------------------------ //
        vertx.setPeriodic(1000, id -> {
            long elapsed = System.currentTimeMillis() - lastReceived.get();

            client.get(PORT, "localhost", "/api/status")
                .send()
                .onSuccess(res -> {
                    String apiMode = res.bodyAsJsonObject().getString("mode");

                    boolean timedOut = elapsed > TIMEOUT_MS;
                    String targetMode = (timedOut || "UNCONNECTED".equals(apiMode))
                            ? "UNCONNECTED" : apiMode;

                    if (timedOut) {
                        System.out.println("[MQTT] Unreachable (" + elapsed / 1000 + "s without data)");
                    } else {
                        System.out.println("[MQTT] Connected, mode=" + targetMode);
                    }

                    serialSend(targetMode);

                    if (!currentMode[0].equals(targetMode)) {
                        System.out.println("[SERIAL] Pushed mode to Arduino: "
                                + currentMode[0] + " → " + targetMode);
                        currentMode[0] = targetMode;
                    }

                    // In AUTOMATIC, CUS computes and owns the valve value
                    if ("AUTOMATIC".equals(targetMode)) {
                        client.get(PORT, "localhost", "/api/data")
                            .send()
                            .onSuccess(dataRes -> {
                                io.vertx.core.json.JsonArray arr = dataRes.bodyAsJsonArray();
                                if (arr == null || arr.isEmpty()) return;

                                double level = arr.getJsonObject(0).getDouble("value");

                                // Distance from top: smaller = tank fuller
                                final double L1 = 40.0;
                                final double L2 = 20.0;

                                int autoValve;
                                if (level <= L2)       autoValve = 100;
                                else if (level <= L1)  autoValve = 50;
                                else                   autoValve = 0;

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