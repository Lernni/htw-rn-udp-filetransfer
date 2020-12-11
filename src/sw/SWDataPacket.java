package sw;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class SWDataPacket extends SWPacket {
    public static final int PACKET_SIZE = 576;
    private byte[] content;

    public SWDataPacket(short sessionNumber, byte packetNumber, byte[] content) {
        // prepare buffer
        int contentLength = Math.min(content.length, PACKET_SIZE - 3);
        buffer = ByteBuffer.allocate(contentLength + 3);

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
        int dataLength = Math.min(data.length, PACKET_SIZE);
        buffer = ByteBuffer.allocate(dataLength);
        buffer.put(Arrays.copyOfRange(data, 0, dataLength));

        // write buffer data in vars
        sessionNumber = buffer.getShort(0);
        packetNumber = buffer.get(2);
        content = Arrays.copyOfRange(buffer.array(), 3, dataLength);

        return true;
    }

    public byte[] getContent() {
        return content;
    }
}
