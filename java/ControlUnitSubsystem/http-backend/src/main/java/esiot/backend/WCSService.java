package esiot.backend;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Consumer;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

public class WCSService implements SerialPortEventListener {

    private static final String PORT = "/dev/tty.usbmodem11201";
    private static final int BAUD_RATE = 115200;

    private SerialPort serialPort;
    private BlockingQueue<String> queue;
    private StringBuffer currentMsg = new StringBuffer("");
    private Consumer<String> onMessageReceived;

    public WCSService(Consumer<String> onMessageReceived) {
        this.onMessageReceived = onMessageReceived;
        this.queue = new ArrayBlockingQueue<>(100);
    }

    public void start() throws Exception {
        serialPort = new SerialPort(PORT);
        serialPort.openPort();

        serialPort.setParams(BAUD_RATE,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);

        serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN |
                SerialPort.FLOWCONTROL_RTSCTS_OUT);

        serialPort.addEventListener(this);

        System.out.println("[WCS] Serial port opened: " + PORT + " at " + BAUD_RATE);
    }

    public void sendMode(String mode) {
        sendMsg("MODE:" + mode);
    }

    public void sendValve(int percent) {
        sendMsg("VALVE:" + percent);
    }

    private void sendMsg(String msg) {
        char[] array = (msg + "\n").toCharArray();
        byte[] bytes = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            bytes[i] = (byte) array[i];
        }
        try {
            synchronized (serialPort) {
                serialPort.writeBytes(bytes);
            }
            System.out.println("[WCS] Sent: " + msg);
        } catch (Exception ex) {
            System.out.println("[WCS] Send error: " + ex.getMessage());
        }
    }

    public boolean isMsgAvailable() {
        return !queue.isEmpty();
    }

    public String receiveMsg() throws InterruptedException {
        return queue.take();
    }

    public void close() {
        try {
            if (serialPort != null) {
                serialPort.removeEventListener();
                serialPort.closePort();
                System.out.println("[WCS] Serial port closed");
            }
        } catch (Exception ex) {
            System.out.println("[WCS] Close error: " + ex.getMessage());
        }
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        if (event.isRXCHAR()) {
            try {
                String msg = serialPort.readString(event.getEventValue());
                msg = msg.replaceAll("\r", "");

                currentMsg.append(msg);

                boolean goAhead = true;
                while (goAhead) {
                    String msg2 = currentMsg.toString();
                    int index = msg2.indexOf("\n");
                    if (index >= 0) {
                        String completeMsg = msg2.substring(0, index);
                        queue.put(completeMsg);
                        currentMsg = new StringBuffer("");
                        if (index + 1 < msg2.length()) {
                            currentMsg.append(msg2.substring(index + 1));
                        }

                        System.out.println("[WCS] Received: " + completeMsg);
                        if (onMessageReceived != null) {
                            onMessageReceived.accept(completeMsg);
                        }
                    } else {
                        goAhead = false;
                    }
                }
            } catch (Exception ex) {
                System.out.println("[WCS] Receive error: " + ex.getMessage());
            }
        }
    }
}