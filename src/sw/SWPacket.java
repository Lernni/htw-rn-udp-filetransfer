package sw;

import java.nio.ByteBuffer;

public abstract class SWPacket {
    public ByteBuffer buffer;

    public short sessionNumber = 0;
    public byte packetNumber = 0;

    public SWPacket() {
    }

    public byte[] getData() {
        return buffer.array();
    }

    public abstract boolean setData(byte[] data);

    public short getSessionNumber() {
        return sessionNumber;
    }

    public byte getPacketNumber() {
        return packetNumber;
    }
}