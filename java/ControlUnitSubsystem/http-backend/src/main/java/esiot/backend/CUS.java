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

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

/**
 * Classe principale del Control Unit Subsystem
 * Gestisce la comunicazione MQTT, HTTP e Serial con i vari sottosistemi
 */
public class CUS {
    /** Porta HTTP su cui il server Vert.x ascolta */
    static final int PORT = 8080;

    /**
     * Metodo principale di avvio dell'applicazione
     * 
     * @param args argomenti da linea di comando (non utilizzati)
     * @throws Exception se si verifica un errore durante l'avvio
     */
    public static void main(String[] args) throws Exception {
        Vertx vertx = Vertx.vertx();

        DataService dataService = new DataService(PORT);

        WCSService wcsService = new WCSService(message -> {
            System.out.println("[CUS] Received from WCS: " + message);
            if (message != null && message.startsWith("MODE:")) {
                String mode = message.substring(5);
                dataService.setMode(mode);
            } else if (message != null && message.startsWith("VALVE:")) {
                String val = message.substring(6);
                try {
                    int percent = Integer.parseInt(val);
                    dataService.setValvePercent(percent);
                } catch (NumberFormatException e) {
                    System.out.println("[CUS] Invalid valve value: " + val);
                }
            }
        });
        wcsService.start();

        dataService.setWCSService(wcsService);
        vertx.deployVerticle(dataService);

        WebClient client = WebClient.create(vertx);

        MqttService mqttService = new MqttService(vertx, client, PORT, null);
        mqttService.start();
    }
}