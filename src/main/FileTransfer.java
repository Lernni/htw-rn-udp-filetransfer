package main;

import java.io.*;
import java.net.*;

import sw.*;

public class FileTransfer {

    public FileTransfer() { }

    public boolean sendRequest(InetAddress host, int port, File file) {
        // send start packet & receive answer
        SWStartPacket startPacket = new SWStartPacket(file);
        SWHandler handler = new SWHandler();
        if (handler.sendPacket(startPacket, host, port)) {
            System.out.println("FT: Start packet '" + startPacket.getSessionNumber() + "' sent, received ACK!");
        } else {
            return false;
        }

        //packetNumber = (byte) (packetNumber ^ 1); // XOR
        // loop
            // send data packets
            // receivce answer
        return true;
    }

    public boolean receiveRequest(int port) {
        // wait for packet & send answer
        SWHandler handler = new SWHandler();
        SWStartPacket startPacket = new SWStartPacket();
        startPacket = (SWStartPacket) handler.receivePacket(startPacket, port);
        if (startPacket != null) {
            System.out.println("FT: Start packet '" + startPacket.getSessionNumber() + "' received, ACK sent!");
        }
        return startPacket != null;
    }
}