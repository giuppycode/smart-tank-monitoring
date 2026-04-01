package esiot.backend;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SerialProtocol implements SerialPortEventListener {
    private SerialPort serialPort;
    private final String[] currentMode = {"AUTOMATIC"};
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private SerialCallback callback;
    private boolean initialized = false;

    public SerialProtocol() {}

    public void initialize(SerialCallback callback) {
        this.callback = callback;

        String[] ports = SerialPortList.getPortNames();
        String selectedPort = null;

        for (String portName : ports) {
            System.out.println("[SERIAL] Found port: " + portName);
            if (portName.contains("USB") || portName.contains("ACM") || portName.contains("cu.")) {
                selectedPort = portName;
                break;
            }
        }

        if (selectedPort == null) {
            if (ports.length > 0) {
                selectedPort = ports[0];
                System.out.println("[SERIAL] No USB/ACM port found, using: " + selectedPort);
            } else {
                System.out.println("[SERIAL] ERROR: No serial ports available.");
                return;
            }
        }

        this.serialPort = new SerialPort(selectedPort);

        try {
            serialPort.openPort();
            serialPort.setParams(
                SerialPort.BAUDRATE_115200,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE
            );
            // Disable flow control - not supported on most USB-Serial adapters

            serialPort.addEventListener(this);
            initialized = true;

            System.out.println("[SERIAL] Connected to: " + serialPort.getPortName());

            new Thread(new MessageProcessor()).start();

        } catch (SerialPortException e) {
            System.out.println("[SERIAL] Error opening port: " + e.getMessage());
        }
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        if (event.isRXCHAR() && event.getEventValue() > 0) {
            try {
                String data = serialPort.readString(event.getEventValue());
                if (data != null) {
                    StringBuilder sb = new StringBuilder();
                    for (char c : data.toCharArray()) {
                        if (c == '\n' || c == '\r') {
                            if (sb.length() > 0) {
                                messageQueue.offer(sb.toString());
                                sb.setLength(0);
                            }
                        } else {
                            sb.append(c);
                        }
                    }
                }
            } catch (SerialPortException e) {
                System.out.println("[SERIAL] Read error: " + e.getMessage());
            }
        }
    }

    private class MessageProcessor implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    String line = messageQueue.take();
                    processMessage(line);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void processMessage(String line) {
        line = line.trim();
        if (line.isEmpty()) return;

        System.out.println("[SERIAL] Received: " + line);

        if (!line.contains(",")) return;

        String[] parts = line.split(",", 2);
        if (parts.length < 2 || !parts[1].startsWith("VALVE:")) return;

        String arduinoMode = parts[0].trim();
        String valveStr = parts[1].substring(6).trim();

        int valve;
        try {
            valve = Integer.parseInt(valveStr);
        } catch (NumberFormatException e) {
            System.out.println("[SERIAL] Invalid valve value: " + valveStr);
            return;
        }

        if (!arduinoMode.equals(currentMode[0])) {
            System.out.println("[SERIAL] Mode changed by Arduino: " + currentMode[0] + " -> " + arduinoMode);
            currentMode[0] = arduinoMode;
            callback.onModeChanged(arduinoMode);
        }

        if ("MANUAL".equals(arduinoMode)) {
            callback.onValveChanged(valve);
        }
    }

    public void sendMode(String mode) {
        if (serialPort == null || !serialPort.isOpened()) {
            System.out.println("[SERIAL] Port not open, cannot send mode");
            return;
        }
        try {
            serialPort.writeString(mode + "\n");
            System.out.println("[SERIAL] Sent mode: " + mode);
        } catch (SerialPortException e) {
            System.out.println("[SERIAL] Write error: " + e.getMessage());
        }
    }

    public void sendValve(int percent) {
        if (serialPort == null || !serialPort.isOpened()) {
            System.out.println("[SERIAL] Port not open, cannot send valve");
            return;
        }
        try {
            serialPort.writeString("VALVE:" + percent + "\n");
            System.out.println("[SERIAL] Sent valve: " + percent);
        } catch (SerialPortException e) {
            System.out.println("[SERIAL] Write error: " + e.getMessage());
        }
    }

    public String getCurrentMode() {
        return currentMode[0];
    }

    public boolean isInitialized() {
        return initialized;
    }

    public interface SerialCallback {
        void onModeChanged(String mode);
        void onValveChanged(int valve);
    }
}