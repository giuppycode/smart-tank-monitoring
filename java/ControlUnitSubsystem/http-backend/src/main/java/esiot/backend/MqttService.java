package esiot.backend;

import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;

public class MqttService {
    static final String BROKER = "tcp://broker.mqtt-dashboard.com";
    static final String TOPIC = "esiot-2025";
    static final long TIMEOUT_MS = 3000;

    private final Vertx vertx;
    private final WebClient client;
    private final int httpPort;
    private final AtomicLong lastReceived;
    private final java.util.function.Consumer<Float> onDataReceived;

    public MqttService(Vertx vertx, WebClient client, int httpPort, java.util.function.Consumer<Float> onDataReceived) {
        this.vertx = vertx;
        this.client = client;
        this.httpPort = httpPort;
        this.lastReceived = new AtomicLong(System.currentTimeMillis());
        this.onDataReceived = onDataReceived;
    }

    public void start() throws Exception {
        String clientId = "cus-" + System.currentTimeMillis();
        MqttClient mqtt = new MqttClient(BROKER, clientId);

        mqtt.setCallback(new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) {
                lastReceived.set(System.currentTimeMillis());

                String payload = new String(message.getPayload());
                System.out.println("[MQTT] Ricevuto: " + payload);

                try {
                    float distanza = Float.parseFloat(payload.trim());

                    if (onDataReceived != null) {
                        onDataReceived.accept(distanza);
                    }

                    JsonObject item = new JsonObject();
                    item.put("value", distanza);
                    item.put("place", "tank");

                    client.post(httpPort, "localhost", "/api/data")
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

        vertx.setPeriodic(1000, id -> {
            long elapsed = System.currentTimeMillis() - lastReceived.get();
            if (elapsed > TIMEOUT_MS) {
                System.out.println("[MQTT] Status: unreachable (nessun dato da " + elapsed / 1000 + "s)");
            } else {
                System.out.println("[MQTT] Status: connected");
            }
        });
    }
}