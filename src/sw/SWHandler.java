package sw;

import sw.packets.SWAckPacket;
import sw.packets.SWPacket;

import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Random;

public class SWHandler {

    private final int MAX_TIMEOUT_RETRIES = 10;
    private final int MAX_EXPECTED_BYTES = 2000;

    private DatagramSocket socket;
    private RateMeasurement rateMeasurement;
    private boolean debugMode;

    // temporary vars for dataIndication -> dataResponse
    private Short sessionNumber;
    private Byte packetNumber = null;
    private InetAddress clientHost;
    private int clientPort;

    double lossRate;
    int averageDelay;
    Random random;

    // either serverHost and serverPort if dataRequest
    // or localhost (host = null) and serverPort if dataIndication, dataResponse
    private InetAddress host;
    private int port;

    // constructor for client use
    public SWHandler(InetAddress host, int port, Integer timeout, RateMeasurement rateMeasurement, boolean debugMode)
            throws IOException {
        this.host = host;
        this.port = port;
        this.debugMode = debugMode;
        this.rateMeasurement = rateMeasurement;
        socket = new DatagramSocket();
        if (timeout != null) socket.setSoTimeout(timeout);
    }

    // constructor for server use
    public SWHandler(int port, double lossRate, int averageDelay, Integer timeout, boolean debugMode)
            throws IOException {
        this.port = port;
        this.lossRate = lossRate;
        this.averageDelay = averageDelay;
        this.debugMode = debugMode;
        random = new Random();
        socket = new DatagramSocket(port);
        if (timeout != null) socket.setSoTimeout(timeout);
    }

    public boolean dataRequest(SWPacket packet) throws IOException {
        // prepare send datagram
        byte[] sendData = packet.getData();
        DatagramPacket datagramSendPacket = new DatagramPacket(sendData, sendData.length, host, port);

        // prepare receive datagram
        byte[] receiveData = new byte[SWAckPacket.PACKET_SIZE];
        DatagramPacket datagramReceivePacket = new DatagramPacket(receiveData, receiveData.length);

        // loop will be left, if a packet got received or an error had occurred
        int timeoutRetries = 0;
        while (true) {
            // send datagram
            socket.send(datagramSendPacket);
            if (debugMode) System.out.println("SW: >>> sent packet to host - (" +
                    datagramSendPacket.getLength() + " Bytes) (PN: " + packet.getPacketNumber() + ")");

            // receive datagram
            try {
                while (true) {
                    // wait for ACK - timeout starts
                    if (debugMode) System.out.println("SW: Waiting for ACK from server...");
                    socket.receive(datagramReceivePacket);
                    if (debugMode) System.out.println("SW: <<< received packet from host - (" +
                            datagramReceivePacket.getLength() + " Bytes)");

                    // check if received packet is valid and if the packet number is correct
                    SWAckPacket ackPacket = new SWAckPacket();
                    if (ackPacket.setData(datagramReceivePacket.getData())) {
                        if (debugMode) System.out.println("SW: Received ACK: PN: " + ackPacket.getPacketNumber());
                        if (packet.getPacketNumber() == ackPacket.getPacketNumber()) {
                            rateMeasurement.addSize(datagramSendPacket.getLength());
                            break;
                        } else {
                            System.out.println("SW: Received ACK is invalid! Waiting for valid ACK...");
                        }
                    }
                }

                // state: valid ack received, close data request
                break;

            } catch (SocketTimeoutException e) {
                // timeout reached

                timeoutRetries++;
                if (timeoutRetries != MAX_TIMEOUT_RETRIES) {
                    System.out.println("SW: No answer from host after " + timeoutRetries +
                            " retries, sending new data request...");
                } else {
                    System.out.println("SW: No answer from host after " + timeoutRetries +
                            " retries, canceling request...");
                    return false;
                }

            }
        }
        return true;
    }

    public SWPacket dataIndication(SWPacket packet) throws IOException {
        while (true) {
            byte[] receiveData = new byte[MAX_EXPECTED_BYTES];
            DatagramPacket datagramReceivePacket = new DatagramPacket(receiveData, receiveData.length);

            // wait for datagram
            do {
                if (debugMode) System.out.println("SW: Waiting for data from client...");
                socket.receive(datagramReceivePacket);
            } while (!simulateChannel());

            if (debugMode) System.out.println("SW: <<< received packet from client - (" +
                    datagramReceivePacket.getLength() + " Bytes)");

            // check if received packet is valid
            receiveData = Arrays.copyOfRange(receiveData, 0, datagramReceivePacket.getLength());
            if (!packet.setData(receiveData)) {
                System.out.println("SW: Received packet is invalid!");
                continue;
            }

            if (debugMode) System.out.println("SW: Received packet: PN: " + packet.getPacketNumber());

            // set temporary sender information for future data response
            sessionNumber = packet.getSessionNumber();
            clientHost = datagramReceivePacket.getAddress();
            clientPort = datagramReceivePacket.getPort();

            if (packetNumber == null || packetNumber != packet.getPacketNumber()) {
                // received packet is valid - break loop
                packetNumber = packet.getPacketNumber();
                break;
            } else {
                // received packet is invalid - wait again
                System.out.println("SW: Received packet was not expected! Wrong Packet Number! Resend ACK...");
                dataResponse();
            }
        }

        return packet; // return packet to higher level
    }

    public boolean dataResponse() throws IOException {
        // prepare send datagram
        SWAckPacket ackPacket = new SWAckPacket(sessionNumber, packetNumber);
        byte[] sendData = ackPacket.getData();
        DatagramPacket datagramSendPacket = new DatagramPacket(sendData, sendData.length, clientHost, clientPort);

        // send datagram
        boolean ackLost = simulateChannel();
        if (ackLost) {
            socket.send(datagramSendPacket);
            if (debugMode) System.out.println("SW: >>> sent ACK packet to client - (" +
                    datagramSendPacket.getLength() + " Bytes) (PN: " + ackPacket.getPacketNumber() + ")");
        }
        return ackLost;
    }

    public void closeSocket() {
        try {
            socket.close();
        } catch (Exception ignored) {}
    }

    private boolean simulateChannel() {
        // simulate network delay
        if (averageDelay != 0.0) {
            try {
                int currentDelay = (int) (random.nextDouble() * 2 * averageDelay);
                if (debugMode) System.out.println("SW: simulate network delay... (" + currentDelay + " ms)");
                Thread.sleep(currentDelay);
            } catch (InterruptedException ignored) {}
        }

        // decide whether to reply, or simulate packet loss
        if (lossRate == 0) return true;
        boolean packetLost = !(random.nextDouble() < lossRate);
        if (!packetLost) System.out.println("SW: simulate packet loss...");
        return packetLost;
    }
}