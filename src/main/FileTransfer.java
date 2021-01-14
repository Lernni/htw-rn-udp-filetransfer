package main;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

import sw.*;
import sw.packets.SWDataPacket;
import sw.packets.SWStartPacket;

public class FileTransfer {

    private final int CRC_LENGTH = 4;
    private final int SERVER_TIMEOUT = 10000;
    private final int CLIENT_TIMEOUT = 1000; // start value

    private final boolean debugMode;
    private double lossRate;
    private int averageDelay;

    // for client
    public FileTransfer(boolean debugMode) {
        this.debugMode = debugMode;
    }

    // for server
    public FileTransfer(double lossRate, int averageDelay, boolean debugMode) {
        this.debugMode = debugMode;
        this.lossRate = lossRate;
        this.averageDelay = averageDelay;
    }

    public boolean fileRequest(InetAddress host, int port, File file) throws IOException {
        // prepare stop & wait handler and rate measurement
        RateMeasurement rateMeasurement = new RateMeasurement("FT: %s", 1000);
        SWHandler handler = new SWHandler(host, port, CLIENT_TIMEOUT, rateMeasurement, debugMode);

        // send start packet & receive answer
        SWStartPacket startPacket = new SWStartPacket(file);
        if (handler.dataRequest(startPacket)) {
            System.out.println("FT: Start packet 'SN: " + startPacket.getUnsignedSessionNumber() + "' sent, received ACK");
        } else {
            handler.closeSocket();
            System.out.println("FT: Error: Start packet '" + startPacket.getUnsignedSessionNumber() +
                    "' could not be sent and/or verified!");
            return false;
        }

        // state: connection to server established

        // start rate measurement
        long fileSize = startPacket.getFileLength();
        rateMeasurement.setFileSize(fileSize);
        rateMeasurement.start();

        // prepare data packets
        DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file.getPath()));
        SWDataPacket dataPacket = new SWDataPacket();
        short sessionNumber = startPacket.getSessionNumber();
        byte packetNumber = startPacket.getPacketNumber();
        CRC32 crc = new CRC32();
        boolean endOfFile = false;
        int bytesSent = 0;
        int bytesRead;
        byte[] data;

        while (!endOfFile) {
            // read data from file
            data = new byte[dataPacket.MAX_CONTENT_SIZE];
            bytesRead = dataInputStream.read(data);

            // update crc over every packet, as long as bytes are read
            if (bytesRead == -1) {
                bytesRead = 0;
            } else {
                crc.update(data, 0, bytesRead);
            }

            if (debugMode) bytesSent += bytesRead;

            // check if crc must be included
            if (bytesRead <= dataPacket.MAX_CONTENT_SIZE - CRC_LENGTH) {
                ByteBuffer buffer = ByteBuffer.allocate(bytesRead + CRC_LENGTH);
                data = Arrays.copyOfRange(data, 0, bytesRead);
                buffer.put(data);
                buffer.putInt((int) crc.getValue());
                data = buffer.array();
                endOfFile = true;
            }

            // switch packet number for data packet
            packetNumber = (byte) (packetNumber ^ 1);

            // send data packet
            dataPacket = new SWDataPacket(sessionNumber, packetNumber, data);
            if (handler.dataRequest(dataPacket)) {
                if (debugMode) {
                    int progress = (int) (((double) bytesSent / (double) fileSize) * 100);
                    System.out.println("FT: Data packet sent, received ACK | " + progress + "%");
                }
            } else {
                System.out.println("FT: Error: Data packet could not be sent and/or verified!");
                rateMeasurement.stop();
                dataInputStream.close();
                handler.closeSocket();
                return false;
            }
        }

        // state: file request successful

        // end file request
        rateMeasurement.stop();
        dataInputStream.close();
        handler.closeSocket();
        return true;
    }

    public String fileIndication(int port) throws IOException {
        SWHandler handler = new SWHandler(port, lossRate, averageDelay, SERVER_TIMEOUT, debugMode);

        // wait for start packet
        SWStartPacket startPacket = null;
        System.out.println("FT: Waiting for start packet...");

        do {
            try {
                startPacket = (SWStartPacket) handler.dataIndication(new SWStartPacket());
            } catch (SocketTimeoutException ignored) {}
        } while (startPacket == null);

        System.out.println("FT: Start packet 'SN: " + startPacket.getUnsignedSessionNumber() + "' received. Send ACK...");
        handler.dataResponse();

        // check if file in start packet already exists locally and rename it in that case
        // System.getProperty("user.dir") - returns String of path, where the application was executed
        File file = new File(System.getProperty("user.dir") + "\\" + startPacket.getFileName());

        if (file.exists()) {
            File newFile;
            String newFileName;
            int duplicates = 1;
            String[] fileNameSplit = startPacket.getFileName().split("\\.", 2);

            if (fileNameSplit.length < 2) {
                newFileName = fileNameSplit[0] + "%d";
            } else {
                newFileName = fileNameSplit[0] + "%d." + fileNameSplit[1];
            }

            do {
                newFile = new File(file.getParentFile(), String.format(newFileName, duplicates));
                duplicates++;
            } while (newFile.exists());

            file = newFile;
        }

        // wait for data packets
        DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(file.getPath()));
        SWDataPacket dataPacket = new SWDataPacket();
        CRC32 crc = new CRC32();
        long fileSize = startPacket.getFileLength();
        int bytesReceived = 0;
        int crcReceived;
        byte[] content;
        boolean endOfFile = false;

        while (!endOfFile) {
            try {
                if (debugMode) System.out.println("FT: Waiting for data packet...");
                dataPacket = (SWDataPacket) handler.dataIndication(dataPacket);
            } catch (SocketTimeoutException e) {
                handler.closeSocket();
                dataOutputStream.close();
                System.out.println("FT: Timeout reached! Could not receive data packet!");
                if (file.delete()) System.out.println("FT: Deleted corrupted file!");
                return null;
            }

            // write data to stream
            content = dataPacket.getContent();
            bytesReceived += content.length;
            int progress = (int) (((double) bytesReceived / (double) fileSize) * 100);
            System.out.println("FT: Data packet received! | " + progress + "%");

            if (bytesReceived < fileSize) {
                // normal data packet received
                crc.update(content, 0, content.length);
                dataOutputStream.write(content);
                if (handler.dataResponse() && debugMode) System.out.println("FT: ACK sent!");
            } else {
                // last data packet received
                ByteBuffer buffer = ByteBuffer.allocate(content.length);
                buffer.put(content);
                crcReceived = buffer.getInt(content.length - CRC_LENGTH);
                content = Arrays.copyOfRange(content, 0, content.length - CRC_LENGTH);
                dataOutputStream.write(content);
                crc.update(content, 0, content.length);

                // check if received crc is valid
                if (((int) crc.getValue()) == crcReceived) {
                    System.out.println("FT: Received valid CRC - file complete!\n" +
                            "Calculated CRC: " + Integer.toHexString((int) crc.getValue()) + "\n" +
                            "Received CRC: " + Integer.toHexString(crcReceived));
                    if (handler.dataResponse() && debugMode) System.out.println("FT: ACK sent!");
                } else {
                    System.out.println("FT: Received invalid CRC - file corrupted!\n" +
                            "Calculated CRC: " + Integer.toHexString((int) crc.getValue()) + "\n" +
                            "Received CRC: " + Integer.toHexString(crcReceived));
                    dataOutputStream.close();
                    if (file.delete()) System.out.println("FT: Deleted corrupted file!");
                    handler.closeSocket();
                    return null;
                }
                endOfFile = true;
            }
        }

        dataOutputStream.close();
        handler.closeSocket();
        return file.getPath();
    }
}