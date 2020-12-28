package main;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

import sw.*;

public class FileTransfer {

    private final int CRC_LENGTH = 4;
    private final int SOCKET_TIMEOUT = 10000;

    public FileTransfer() { }

    public boolean fileRequest(InetAddress host, int port, File file) throws IOException {
        // send start packet & receive answer
        SWHandler handler = new SWHandler(host, port, SOCKET_TIMEOUT);
        SWStartPacket startPacket = new SWStartPacket(file);
        if (handler.dataRequest(startPacket)) {
            System.out.println("FT: Start packet '" + startPacket.getSessionNumber() + "' sent, received ACK");
        } else {
            handler.closeSocket();
            System.out.println("FT: Error: Start packet '" + startPacket.getSessionNumber() +
                    "' could not be sent and/or verified!");
            return false;
        }

        // prepare data packets
        DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file.getPath()));
        SWDataPacket dataPacket = new SWDataPacket();
        short sessionNumber = startPacket.getSessionNumber();
        byte packetNumber = startPacket.getPacketNumber();
        CRC32 crc = new CRC32();
        boolean endOfFile = false;

        while (!endOfFile) {
            // read data from file
            byte[] data = new byte[dataPacket.MAX_CONTENT_SIZE];
            int bytesRead = dataInputStream.read(data);

            // update crc over every packet, as long as bytes are read
            if (bytesRead == -1) {
                bytesRead = 0;
            } else {
                crc.update(data, 0, bytesRead);
            }

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
                System.out.println("FT: Data packet '" + startPacket.getSessionNumber() + "' sent, received ACK");
            } else {
                System.out.println("FT: Error: Data packet '" + startPacket.getSessionNumber() +
                        "' could not be sent and/or verified!");
                return false;
            }
        }

        dataInputStream.close();
        handler.closeSocket();
        return true;
    }

    public String fileIndication(int port) throws IOException {
        SWHandler handler = new SWHandler(port, SOCKET_TIMEOUT);

        // wait for start packet
        SWStartPacket startPacket = null;
        System.out.println("FT: Waiting for start packet...");

        do {
            try {
                startPacket = (SWStartPacket) handler.dataIndication(new SWStartPacket());
            } catch (SocketTimeoutException e) {
                handler.closeSocket();
            }
        } while (startPacket == null);

        handler.dataResponse();
        System.out.println("FT: Start packet '" + startPacket.getSessionNumber() + "' received, ACK sent");


        // check if file in start packet already exists locally and rename it in that case
        // System.getProperty("user.dir") - returns String of path, where the application was executed
        File file = new File(System.getProperty("user.dir") + "\\" + startPacket.getFileName());

        if (file.exists()) {
            File newFile;
            String newFileName;
            int duplicates = 1;
            String[] fileNameSplit = startPacket.getFileName().split("\\.", 2);

            do {
                newFileName = fileNameSplit[0] + duplicates + "." + fileNameSplit[1];
                newFile = new File(file.getParentFile(), newFileName);
                duplicates++;
            } while (newFile.exists());

            file = newFile;
        }

        // wait for data packets
        DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(file.getPath()));
        SWDataPacket dataPacket = new SWDataPacket();
        CRC32 crc = new CRC32();
        int fileSize = startPacket.getFileLength();
        int bytesReceived = 0;
        int crcReceived;
        byte[] content;
        boolean endOfFile = false;

        while (!endOfFile) {
            try {
                System.out.println("FT: Waiting for data packet...");
                dataPacket = (SWDataPacket) handler.dataIndication(dataPacket);
            } catch (SocketTimeoutException e) {
                handler.closeSocket();
                System.out.println("FT: Timeout reached! Could not receive data packet!");
                return null;
            }

            if (dataPacket == null) {
                handler.closeSocket();
                System.out.println("FT: Error: Could not receive data packet!");
                return null;
            }

            // write data to stream
            content = dataPacket.getContent();
            bytesReceived += content.length;
            int progress = (int) (((double) bytesReceived / (double) fileSize) * 100);

            System.out.println("FT: Data packet '" + dataPacket.getSessionNumber() + "' received! - (" +
                    progress + "%)");

            if (bytesReceived < fileSize) {
                // normal data packet received
                crc.update(content, 0, content.length);
                dataOutputStream.write(content);
                handler.dataResponse();
                System.out.println("FT: ACK sent!");
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
                    System.out.println("FT: Received valid CRC - file complete!");
                    handler.dataResponse();
                    System.out.println("FT: ACK sent!");
                } else {
                    System.out.println("FT: Received invalid CRC - file corrupted!");
                    dataOutputStream.close();
                    // delete corrupted file
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