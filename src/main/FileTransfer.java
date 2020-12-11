package main;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

import sw.*;

public class FileTransfer {

    public int maxContentLength = SWDataPacket.PACKET_SIZE - 3;

    public FileTransfer() { }

    public boolean sendRequest(InetAddress host, int port, File file) throws IOException {
        // send start packet & receive answer
        SWHandler handler = new SWHandler();
        SWStartPacket startPacket = new SWStartPacket(file);
        if (handler.sendPacket(startPacket, host, port)) {
            System.out.println("FT: Start packet '" + startPacket.getSessionNumber() + "' sent, received ACK!");
        } else {
            return false;
        }

        DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file.getPath()));

        short sessionNumber = startPacket.getSessionNumber();
        byte packetNumber = (byte) (startPacket.getPacketNumber() ^ 1); // XOR
        CRC32 crc = new CRC32();
        Integer crcValue = null;
        boolean endOfFile = false;

        while (!endOfFile) {
            // read data from file
            byte[] data = new byte[maxContentLength];
            int bytesRead = dataInputStream.read(data);

            if (bytesRead == -1) {
                bytesRead = 0;
            } else {
                crc.update(data, 0, bytesRead);
            }

            if (bytesRead <= maxContentLength - 4) { // check if crc must be included
                ByteBuffer buffer = ByteBuffer.allocate(bytesRead + 4);
                data = Arrays.copyOfRange(data, 0, bytesRead);
                buffer.put(data);
                buffer.putInt((int) crc.getValue());
                data = buffer.array();
                endOfFile = true;
            }

            // send data packet
            SWDataPacket dataPacket = new SWDataPacket(sessionNumber, packetNumber, data);
            if (handler.sendPacket(dataPacket, host, port)) {
                System.out.println("FT: Data packet '" + startPacket.getSessionNumber() + "' sent, received ACK!");
            } else {
                return false;
            }

            packetNumber = (byte) (packetNumber ^ 1);
        }

        dataInputStream.close();
        return true;
    }

    public String receiveRequest(int port) throws IOException {
        SWHandler handler = new SWHandler();

        // wait for start packet
        SWStartPacket startPacket = new SWStartPacket();
        while (true) {
            try {
                startPacket = (SWStartPacket) handler.receivePacket(startPacket, port);
                break;
            } catch (SocketTimeoutException ignored) {
                handler.closeSockets();
            }
        }

        if (startPacket != null) {
            System.out.println("FT: Start packet '" + startPacket.getSessionNumber() + "' received, ACK sent!");
        } else {
            return null;
        }

        File file = new File(System.getProperty("user.dir") + "\\" + startPacket.getFileName());

        // check if file already exists and rename it

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

        DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(file.getPath()));

        // wait for data packets
        SWDataPacket dataPacket = new SWDataPacket();
        CRC32 crc = new CRC32();
        int fileSize = startPacket.getFileLength();
        int bytesReceived = 0;
        int crcReceived;
        byte[] content;
        boolean endOfFile = false;

        while (!endOfFile) {
            try {
                dataPacket = (SWDataPacket) handler.receivePacket(dataPacket, port);
            } catch (SocketTimeoutException e) {
                return null;
            }

            if (dataPacket != null) {
                System.out.println("FT: Data packet '" + dataPacket.getSessionNumber() + "' with " + dataPacket.getContent().length + " bytes received, ACK sent!");
            } else {
                return null;
            }

            content = dataPacket.getContent();
            bytesReceived += content.length;

            if (bytesReceived > fileSize) {
                ByteBuffer buffer = ByteBuffer.allocate(content.length);
                buffer.put(content);
                crcReceived = buffer.getInt(content.length - 4);
                content = Arrays.copyOfRange(content, 0, content.length - 4);
                crc.update(content, 0, content.length);
                if (((int) crc.getValue()) != crcReceived) return null;
                dataOutputStream.write(content);
                endOfFile = true;
            } else {
                crc.update(content, 0, content.length);
                dataOutputStream.write(content);
            }
        }

        dataOutputStream.close();
        return file.getPath();
    }
}