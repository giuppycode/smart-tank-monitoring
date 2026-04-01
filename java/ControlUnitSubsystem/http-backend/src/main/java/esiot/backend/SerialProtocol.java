package esiot.backend;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

public class SerialProtocol {
    private SerialPort serialPort;
    private final String[] currentMode = {"AUTOMATIC"};
    private final StringBuilder serialBuffer = new StringBuilder();

    public SerialProtocol() {}

    public void initialize(SerialCallback callback) {
        final SerialPort[] serialPortHolder = new SerialPort[1];
        for (SerialPort port : SerialPort.getCommPorts()) {
            System.out.println("[SERIAL] Trovata porta: " + port.getSystemPortName());
            if (port.getSystemPortName().contains("USB") || port.getSystemPortName().contains("ACM")) {
                serialPortHolder[0] = port;
                break;
            }
        }

        if (serialPortHolder[0] == null) {
            SerialPort[] all = SerialPort.getCommPorts();
            if (all.length == 0) {
                System.out.println("[SERIAL] ERRORE: nessuna porta seriale disponibile.");
                return;
            }
            System.out.println("[SERIAL] Nessuna porta USB/ACM trovata, uso la prima disponibile.");
            serialPortHolder[0] = all[0];
        }

        this.serialPort = serialPortHolder[0];
        serialPort.setComPortParameters(9600, 8, 1, 0);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
        serialPort.openPort();

        final SerialPort finalPort = serialPort;
        serialPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) return;

                int available = finalPort.bytesAvailable();
                if (available <= 0) return;

                byte[] readBuffer = new byte[available];
                int numRead = finalPort.readBytes(readBuffer, readBuffer.length);
                serialBuffer.append(new String(readBuffer, 0, numRead));

                int newlineIndex;
                while ((newlineIndex = serialBuffer.indexOf("\n")) != -1) {
                    String line = serialBuffer.substring(0, newlineIndex).trim();
                    serialBuffer.delete(0, newlineIndex + 1);

                    if (line.isEmpty()) continue;
                    System.out.println("[SERIAL] Ricevuto: " + line);

                    if (!line.contains(",")) continue;

                    String[] parts = line.split(",", 2);
                    if (parts.length < 2 || !parts[1].startsWith("VALVE:")) continue;

                    String arduinoMode = parts[0].trim();
                    String valveStr   = parts[1].substring(6).trim();

                    int valve;
                    try {
                        valve = Integer.parseInt(valveStr);
                    } catch (NumberFormatException e) {
                        System.out.println("[SERIAL] Valore valvola non valido: " + valveStr);
                        continue;
                    }

                    if (!arduinoMode.equals(currentMode[0])) {
                        System.out.println("[SERIAL] Mode changed by Arduino: "
                                + currentMode[0] + " → " + arduinoMode);
                        currentMode[0] = arduinoMode;
                        callback.onModeChanged(arduinoMode);
                    }

                    if ("MANUAL".equals(arduinoMode)) {
                        callback.onValveChanged(valve);
                    }
                }
            }
        });

        System.out.println("[SERIAL] Connesso a: " + serialPort.getSystemPortName());
    }

    public void sendMode(String mode) {
        if (serialPort == null || !serialPort.isOpen()) return;
        byte[] bytes = (mode + "\n").getBytes();
        serialPort.writeBytes(bytes, bytes.length);
        System.out.println("[SERIAL] Stato iniziale inviato: " + mode);
    }

    public String getCurrentMode() {
        return currentMode[0];
    }

    public interface SerialCallback {
        void onModeChanged(String mode);
        void onValveChanged(int valve);
    }
}
