package esiot.backend;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.eclipse.paho.client.mqttv3.*;

public class CUS {

    static final int PORT = 8080;
    static final String BROKER = "tcp://broker.mqtt-dashboard.com";
    static final String TOPIC = "esiot-2025"; // ← stesso topic del tuo ESP32

    public static void main(String[] args) throws Exception {

        // 1. Avvia Vert.x e il DataService HTTP
        Vertx vertx = Vertx.vertx();
        DataService service = new DataService(PORT);
        vertx.deployVerticle(service);

        // 2. WebClient per fare POST interna a DataService
        WebClient client = WebClient.create(vertx);

        // 3. Avvia il subscriber MQTT
        String clientId = "cus-" + System.currentTimeMillis();
        MqttClient mqtt = new MqttClient(BROKER, clientId);

        mqtt.setCallback(new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String payload = new String(message.getPayload());
                System.out.println("[MQTT] Ricevuto: " + payload);

                try {
                    float distanza = Float.parseFloat(payload.trim());

                    // POST a DataService (tutto in locale)
                    JsonObject item = new JsonObject();
                    item.put("value", distanza);
                    item.put("place", "tank");

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
    }
}