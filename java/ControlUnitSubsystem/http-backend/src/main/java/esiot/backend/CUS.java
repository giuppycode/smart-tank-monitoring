/**
 * CUS (Control Unit Subsystem) - Componente principale del backend IoT
 * 
 * Questa classe implementa un server HTTP che:
 * - Si connette a un broker MQTT per ricevere dati dai sensori
 * - Espone un'API REST per la memorizzazione dei dati
 * - Monitora la connettività MQTT e segnala se i messaggi mancano per troppo tempo
 * 
 * @author Laboratorio IoT
 * @version 1.0
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

/**
 * Classe principale del Control Unit Subsystem
 * Gestisce la comunicazione MQTT e l'API HTTP per i dati dei sensori
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
    
    /** Serial service for communicating with WCS */
    static SerialService serialService;
    
    /** Current mode for serial communication */
    static String currentMode = "AUTOMATIC";

    /**
     * Metodo principale di avvio dell'applicazione
     * 
     * @param args argomenti da linea di comando (non utilizzati)
     * @throws Exception se si verifica un errore durante l'avvio
     */
    public static void main(String[] args) throws Exception {
        serialService = new SerialService();
        serialService.connect();
        
        Vertx vertx = Vertx.vertx();
        DataService service = new DataService(PORT);
        vertx.deployVerticle(service);

        EventBus eb = vertx.eventBus();
        eb.consumer("serial.commands", message -> {
            JsonObject msg = (JsonObject) message.body();
            String type = msg.getString("type");
            String value = msg.getString("value");
            
            if ("mode".equals(type)) {
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

        // Timestamp dell'ultimo messaggio ricevuto
        AtomicLong lastReceived = new AtomicLong(System.currentTimeMillis());
        AtomicBoolean isUnconnected = new AtomicBoolean(false);

        String clientId = "cus-" + System.currentTimeMillis();
        MqttClient mqtt = new MqttClient(BROKER, clientId);

        mqtt.setCallback(new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) {
                // Aggiorna il timestamp ad ogni messaggio ricevuto
                lastReceived.set(System.currentTimeMillis());

                String payload = new String(message.getPayload());
                System.out.println("[MQTT] Ricevuto: " + payload);

                try {
                    float distanza = Float.parseFloat(payload.trim());

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

        // Controlla ogni secondo se sono passati più di 3s senza messaggi
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