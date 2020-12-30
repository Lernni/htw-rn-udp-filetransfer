package sw.packets;

import java.nio.ByteBuffer;

/*
* Stop and Wait Packet:
* 2 Byte - session number
* 1 Byte - packet number
* ...
* */

public abstract class SWPacket {
    public static final int SN_INDEX = 0;
    public static final int PN_INDEX = 2;

    public ByteBuffer buffer;
    public Short sessionNumber = 0;
    public Byte packetNumber = 0;

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