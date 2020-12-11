package sw;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class SWHandler {
    DatagramSocket serverSocket;

    public SWHandler() {}

    public boolean sendPacket(SWPacket packet, InetAddress host, int port) {
        // create client socket
        DatagramSocket clientSocket;
        try {
            clientSocket = new DatagramSocket();
            clientSocket.setSoTimeout(1000);
        } catch (SocketException e) {
            e.printStackTrace();
            return false;
        }

        // prepare send-datagram
        byte[] sendData = packet.getData();
        DatagramPacket datagramSendPacket = new DatagramPacket(sendData, sendData.length, host, port);

        // prepare receive-datagram
        byte[] receiveData = new byte[SWAckPacket.PACKET_SIZE];
        DatagramPacket datagramReceivePacket = new DatagramPacket(receiveData, receiveData.length);

        // loop will be left, if a packet got received or an error had occurred
        int timeoutTries = 0;
        while (true) {
            // send datagram
            try {
                clientSocket.send(datagramSendPacket);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            // receive datagram
            try {
                while (true) {
                    clientSocket.receive(datagramReceivePacket);

                    // check if received packet is valid and if the packet number is correct
                    SWAckPacket ackPacket = new SWAckPacket();
                    if (ackPacket.setData(datagramReceivePacket.getData())) {
                        if (packet.getPacketNumber() == ackPacket.getPacketNumber()) break;
                    }
                }
                break;
            } catch (SocketTimeoutException e) {
                // timeout reached
                timeoutTries++;
                if (timeoutTries == 10) {
                    System.out.println("SW: No answer from host after 10 retries, canceling request...");
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        clientSocket.close();
        return true;
    }

    public SWPacket receivePacket(SWPacket packet, int port) throws IOException {
        // create server socket
        try {
            serverSocket = new DatagramSocket(port);
            serverSocket.setSoTimeout(10000);
        } catch (SocketException e) {
            e.printStackTrace();
            return null;
        }

        // wait for datagram
        byte[] receiveData = new byte[1024];
        DatagramPacket datagramReceivePacket = new DatagramPacket(receiveData, receiveData.length);

        serverSocket.receive(datagramReceivePacket);

        // check if received packet is valid
        receiveData = Arrays.copyOfRange(receiveData, 0, datagramReceivePacket.getLength());
        if (!packet.setData(receiveData)) {
            serverSocket.close();
            return null;
        }

        // get sender information
        InetAddress clientHost = datagramReceivePacket.getAddress();
        int clientPort = datagramReceivePacket.getPort();

        // prepare send-datagram (answer)
        SWAckPacket ackPacket = new SWAckPacket(packet.getSessionNumber(), packet.getPacketNumber());
        byte[] sendData = ackPacket.getData();
        DatagramPacket datagramSendPacket = new DatagramPacket(sendData, sendData.length, clientHost, clientPort);

        // send datagram (answer)
        try {
            serverSocket.send(datagramSendPacket);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        serverSocket.close();
        return packet; // return packet to higher level
    }

    public void closeSockets() {
        serverSocket.close();
    }
}