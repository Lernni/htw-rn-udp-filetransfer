package sw.packets;

import java.nio.ByteBuffer;
import java.util.Arrays;

/*
* Data Packet:
* 2 Byte - session number
* 1 Byte - packet number
* 4-~1400 Byte - data (crc included in last packet)
* */

public class SWDataPacket extends SWPacket {
    public static final int PACKET_SIZE = 1350; // make it var
    public final int HEADER_SIZE = this.sessionNumber.BYTES + this.packetNumber.BYTES;
    public final int MAX_CONTENT_SIZE = PACKET_SIZE - HEADER_SIZE;

    private byte[] content = null;

    public SWDataPacket(Short sessionNumber, Byte packetNumber, byte[] content) {
        // prepare buffer
        int contentLength = Math.min(content.length, MAX_CONTENT_SIZE);
        buffer = ByteBuffer.allocate(contentLength + HEADER_SIZE);

        // session number
        this.sessionNumber = sessionNumber;
        buffer.putShort(sessionNumber);

        // packet number
        this.packetNumber = packetNumber;
        buffer.put(packetNumber);

        // content
        this.content = Arrays.copyOfRange(content, 0, contentLength);
        buffer.put(content);
    }

    public SWDataPacket() {}

    public boolean setData(byte[] data) {
        // prepare buffer
        int dataLength = Math.min(data.length, PACKET_SIZE);
        buffer = ByteBuffer.allocate(dataLength);
        buffer.put(Arrays.copyOfRange(data, 0, dataLength));

        // write buffer data in vars
        sessionNumber = buffer.getShort(SN_INDEX);
        packetNumber = buffer.get(PN_INDEX);
        content = Arrays.copyOfRange(buffer.array(), HEADER_SIZE, dataLength);

        return true;
    }

    public byte[] getContent() {
        return content;
    }
}
