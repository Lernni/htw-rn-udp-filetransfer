package sw;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.CRC32;

public class SWStartPacket extends SWPacket {
    public static final int PACKET_SIZE = 273;
    private static final String startTag = "Start";
    private int fileLength = 0;
    private String fileName = null;

    private CRC32 crc;

    public SWStartPacket(File file) {
        // prepare vars
        Random r = new Random();
        sessionNumber = (short) r.nextInt(65536); // possible numbers: 0 - 65535 --> range of values: 2^16-1
        packetNumber = 0;
        fileLength = (int) file.length();
        fileName = file.getName();
        byte[] fileNameBytes = fileName.getBytes(StandardCharsets.UTF_8);
        if (fileNameBytes.length > 255) fileNameBytes = Arrays.copyOfRange(fileNameBytes, 0, 255);

        // prepare buffer
        buffer = ByteBuffer.allocate(fileNameBytes.length + 18); // 18 bytes fix

        // write buffer
        buffer.putShort(sessionNumber); // 16 bit
        buffer.put(packetNumber); // 8 bit
        buffer.put(startTag.getBytes(StandardCharsets.UTF_8)); // 5 byte "Start"
        buffer.putInt(fileLength); // 4 byte file length
        buffer.putShort((short) file.getName().length()); // 2 byte file name length
        buffer.put(fileNameBytes); // 0 - 255 byte file name

        // prepare crc 32
        crc = new CRC32();
        buffer.flip(); // read mode
        crc.update(buffer); // create crc
        buffer.flip(); // write mode

        // configure buffer to not override existing data
        buffer.position(buffer.limit());
        buffer.limit(buffer.capacity());

        // write crc
        buffer.putInt((int) crc.getValue()); // 4 byte crc value
    }

    public SWStartPacket() {

    }

    public boolean setData(byte[] data) {
        int dataLength = Math.min(data.length, PACKET_SIZE);
        buffer = ByteBuffer.allocate(dataLength);
        buffer.put(Arrays.copyOfRange(data, 0, dataLength));

        // check if buffer data is valid
        crc = new CRC32();
        short fileNameLength = buffer.getShort(12);
        int crcIndex = fileNameLength + 14;
        crc.update(Arrays.copyOfRange(buffer.array(), 0, crcIndex));
        int sentCRCValue = buffer.getInt(crcIndex);
        if ((int) crc.getValue() != sentCRCValue) {
            buffer.clear();
            return false;
        }

        // write buffer data in vars
        sessionNumber = buffer.getShort(0);
        fileLength = buffer.getInt(8);
        byte[] fileNameBytes = Arrays.copyOfRange(buffer.array(), 14, 14 + fileNameLength);
        fileName = new String(fileNameBytes, StandardCharsets.UTF_8);

        return true;
    }

    public int getFileLength() {
        return fileLength;
    }

    public String getFileName() {
        return fileName;
    }
}