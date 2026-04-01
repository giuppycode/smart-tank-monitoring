/**
 * CUS (Control Unit Subsystem) - Componente principale del backend IoT
 *
 * Questa classe implementa un server HTTP che:
 * - Si connette a un broker MQTT per ricevere dati dai sensori
 * - Espone un'API REST per la memorizzazione dei dati
 * - Monitora la connettività MQTT e segnala se i messaggi mancano per troppo tempo
 *
 * @author Laboratorio IoT
 * @version 1.2
 */
package esiot.backend;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.core.eventbus.EventBus;

public class CUS {
    static final int PORT = 8080;
    static final String BROKER = "tcp://broker.mqtt-dashboard.com";
    static final String TOPIC = "esiot-2025";
    static final long TIMEOUT_MS = 3000;
    
    static SerialService serialService;
    static String currentMode = "AUTOMATIC";

    public static void main(String[] args) throws Exception {
        serialService = new SerialService();
        if (!serialService.connect()) {
            System.out.println("[SERIAL] Failed to connect, exiting.");
            return;
        }
        
        Vertx vertx = Vertx.vertx();
        DataService service = new DataService(PORT);
        vertx.deployVerticle(service);

        EventBus eb = vertx.eventBus();
        eb.consumer("serial.commands", message -> {
            JsonObject msg = (JsonObject) message.body();
            String type = msg.getString("type");
            
            if ("mode".equals(type)) {
                String value = msg.getString("value");
                serialService.sendMode(value);
                currentMode = value;
                System.out.println("[EVENTBUS] Mode command: " + value);
            } else if ("valve".equals(type)) {
                int valve = msg.getInteger("value");
                serialService.sendValve(valve);
                System.out.println("[EVENTBUS] Valve command: " + valve);
            }
        });

        WebClient client = WebClient.create(vertx);

        AtomicLong lastReceived = new AtomicLong(System.currentTimeMillis());
        AtomicBoolean isUnconnected = new AtomicBoolean(false);

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

        serialService.sendMode("AUTOMATIC");
        System.out.println("[SERIAL] Stato iniziale inviato: AUTOMATIC");

        // ------------------------------------------------------------------ //
        //  Serial message polling - processes STATE:MODE,PERCENT messages
        // ------------------------------------------------------------------ //
        vertx.setPeriodic(100, id -> {
            while (serialService.hasMessages()) {
                String msg = serialService.pollMessage();
                if (msg != null && msg.startsWith("STATE:")) {
                    String payload = msg.substring(6);
                    String[] parts = payload.split(",");
                    if (parts.length >= 2) {
                        String arduinoMode = parts[0].trim();
                        String valveStr = parts[1].trim();

                        int valve;
                        try {
                            valve = Integer.parseInt(valveStr);
                        } catch (NumberFormatException e) {
                            continue;
                        }

                        // Mode propagation: Arduino button toggles MANUAL on/off
                        if (!arduinoMode.equals(currentMode)) {
                            System.out.println("[SERIAL] Mode changed by Arduino: " + currentMode + " → " + arduinoMode);
                            currentMode = arduinoMode;

                            JsonObject modeBody = new JsonObject().put("mode", arduinoMode);
                            client.post(PORT, "localhost", "/api/status")
                                    .sendJson(modeBody)
                                    .onSuccess(r -> System.out.println("[HTTP] Mode synced to DataService: " + arduinoMode))
                                    .onFailure(err -> System.out.println("[HTTP] Mode sync error: " + err.getMessage()));
                        }

                        // Valve update: only propagate in MANUAL mode
                        if ("MANUAL".equals(arduinoMode)) {
                            JsonObject valveBody = new JsonObject().put("percent", valve);
                            client.post(PORT, "localhost", "/api/valve")
                                    .sendJson(valveBody)
                                    .onSuccess(r -> System.out.println("[HTTP] Valve (MANUAL) " + valve + "% saved"))
                                    .onFailure(err -> System.out.println("[HTTP] Valve save error: " + err.getMessage()));
                        }
                    }
                }
            }
        });

        // ------------------------------------------------------------------ //
        //  Periodic timer - MQTT connectivity check
        // ------------------------------------------------------------------ //
        vertx.setPeriodic(1000, id -> {
            long elapsed = System.currentTimeMillis() - lastReceived.get();
            if (elapsed > TIMEOUT_MS) {
                System.out.println("[MQTT] Status: unreachable (nessun dato da " + elapsed / 1000 + "s)");
                if (isUnconnected.compareAndSet(false, true)) {
                    serialService.sendMode("UNCONNECTED");
                    currentMode = "UNCONNECTED";
                }
            } else {
                System.out.println("[MQTT] Status: connected");
                if (isUnconnected.compareAndSet(true, false)) {
                    serialService.sendMode(currentMode);
                }
            }
        });
    }
}
