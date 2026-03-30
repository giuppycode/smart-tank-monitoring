package esiot.backend;

import jssc.SerialPort;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class SerialService implements SerialPortEventListener {
    
    private static final String PORT = "/dev/ttyUSB0";
    private static final int BAUD_RATE = 115200;
    
    private SerialPort serialPort;
    private BlockingQueue<String> messageQueue;
    private AtomicBoolean connected;
    private String currentMode;
    private int currentValvePercent;
    
    public SerialService() {
        this.messageQueue = new LinkedBlockingQueue<>();
        this.connected = new AtomicBoolean(false);
        this.currentMode = "AUTOMATIC";
        this.currentValvePercent = 0;
    }
    
    public boolean connect() {
        try {
            serialPort = new SerialPort(PORT);
            serialPort.openPort();
            serialPort.setParams(BAUD_RATE, 8, 1, 0);
            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN | 
                                          SerialPort.FLOWCONTROL_RTSCTS_OUT);
            serialPort.addEventListener(this);
            connected.set(true);
            System.out.println("[SERIAL] Connected to " + PORT);
            return true;
        } catch (SerialPortException e) {
            System.out.println("[SERIAL] Connection failed: " + e.getMessage());
            return false;
        }
    }
    
    public void sendMode(String mode) {
        if (!connected.get()) return;
        try {
            String cmd = "MODE:" + mode;
            serialPort.writeString(cmd + "\n");
            this.currentMode = mode;
            System.out.println("[SERIAL] Sent: " + cmd);
        } catch (SerialPortException e) {
            System.out.println("[SERIAL] Failed to send MODE: " + e.getMessage());
        }
    }
    
    public void sendValve(int percent) {
        if (!connected.get()) return;
        try {
            String cmd = "VALVE:" + percent;
            serialPort.writeString(cmd + "\n");
            this.currentValvePercent = percent;
            System.out.println("[SERIAL] Sent: " + cmd);
        } catch (SerialPortException e) {
            System.out.println("[SERIAL] Failed to send VALVE: " + e.getMessage());
        }
    }
    
    public void disconnect() {
        if (serialPort != null && serialPort.isOpened()) {
            try {
                serialPort.closePort();
                connected.set(false);
                System.out.println("[SERIAL] Disconnected");
            } catch (SerialPortException e) {
                System.out.println("[SERIAL] Disconnect error: " + e.getMessage());
            }
        }
    }
    
    public String pollMessage() {
        return messageQueue.poll();
    }
    
    public boolean hasMessages() {
        return !messageQueue.isEmpty();
    }
    
    @Override
    public void serialEvent(jssc.SerialPortEvent event) {
        if (event.isRXCHAR() && event.getEventValue() > 0) {
            try {
                String data = serialPort.readString();
                if (data != null) {
                    String[] lines = data.split("\n");
                    for (String line : lines) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            processMessage(line);
                        }
                    }
                }
            } catch (SerialPortException e) {
                System.out.println("[SERIAL] Read error: " + e.getMessage());
            }
        }
    }
    
    private void processMessage(String msg) {
        System.out.println("[SERIAL] Received: " + msg);
        
        if (msg.startsWith("STATE:")) {
            String payload = msg.substring(6);
            String[] parts = payload.split(",");
            if (parts.length == 2) {
                currentMode = parts[0];
                try {
                    currentValvePercent = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    System.out.println("[SERIAL] Invalid valve percent: " + parts[1]);
                }
            }
        }
        
        messageQueue.offer(msg);
    }
    
    public boolean isConnected() {
        return connected.get();
    }
    
    public String getCurrentMode() {
        return currentMode;
    }
    
    public int getCurrentValvePercent() {
        return currentValvePercent;
    }
}
