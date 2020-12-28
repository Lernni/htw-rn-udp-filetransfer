package sw;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.CRC32;

/* Start Packet:
* 2 Byte - session number
* 1 Byte - packet number
* 5 Byte - start tag
* 4 Byte - file length
* 2 Byte - file name length
* 0-255 Byte - file name
* 4 Byte - CRC
* */

public class SWStartPacket extends SWPacket {
    public final int PACKET_SIZE = 273;
    private final int SN_RANGE = 65536; // possible numbers: 0 - 65535 --> range of values: 2^16-1
    private final int FILE_NAME_SIZE = 255;
    private final int FILE_LENGTH_INDEX = 8;
    private final int FILE_NAME_LENGTH_INDEX = 12;
    private final String startTag = "Start";
    private Integer fileLength = 0;
    private Integer crcValue = 0;
    private Short fileNameLength = 0;
    private String fileName = null;

    private CRC32 crc;

    public SWStartPacket(File file) {
        // prepare vars
        Random r = new Random();
        sessionNumber = (short) r.nextInt(SN_RANGE);
        fileLength = (int) file.length();
        fileName = file.getName();
        byte[] fileNameBytes = fileName.getBytes(StandardCharsets.UTF_8);
        if (fileNameBytes.length > FILE_NAME_SIZE) {
            fileNameBytes = Arrays.copyOfRange(fileNameBytes, 0, FILE_NAME_SIZE);
        }

        // prepare buffer
        buffer = ByteBuffer.allocate(fileNameBytes.length + sessionNumber.BYTES + packetNumber.BYTES +
                startTag.length() + fileLength.BYTES + fileNameLength.BYTES + crcValue.BYTES); // 18 bytes fix

        // write buffer
        buffer.putShort(sessionNumber); // 16 bit
        buffer.put(packetNumber); // 8 bit
        buffer.put(startTag.getBytes(StandardCharsets.UTF_8)); // 5 byte "Start"
        buffer.putInt(fileLength); // 4 byte file length
        fileNameLength = (short) file.getName().length();
        buffer.putShort(fileNameLength); // 2 byte file name length
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
        crcValue = (int) crc.getValue();
        buffer.putInt(crcValue); // 4 byte crc value
    }

    public SWStartPacket() {

    }

    public boolean setData(byte[] data) {
        int dataLength = Math.min(data.length, PACKET_SIZE);
        buffer = ByteBuffer.allocate(dataLength);
        buffer.put(Arrays.copyOfRange(data, 0, dataLength));

        // check if buffer data is valid
        crc = new CRC32();
        fileNameLength = buffer.getShort(FILE_NAME_LENGTH_INDEX);
        int headerLength = sessionNumber.BYTES + packetNumber.BYTES + startTag.length() +
                fileLength.BYTES + fileNameLength.BYTES;
        int crcIndex = fileNameLength + headerLength;
        if (crcIndex > dataLength - crcValue.BYTES || crcIndex < crcValue.BYTES) return false;
        crc.update(Arrays.copyOfRange(buffer.array(), 0, crcIndex));
        int sentCRCValue = buffer.getInt(crcIndex);
        if ((int) crc.getValue() != sentCRCValue) {
            buffer.clear();
            return false;
        }

        // write buffer data in vars
        sessionNumber = buffer.getShort(SN_INDEX);
        fileLength = buffer.getInt(FILE_LENGTH_INDEX);
        byte[] fileNameBytes = Arrays.copyOfRange(buffer.array(), headerLength, crcIndex);
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