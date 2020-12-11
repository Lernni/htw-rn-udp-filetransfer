package sw;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class SWAckPacket extends SWPacket {
    public static final int PACKET_SIZE = 3;

    public SWAckPacket(short sessionNumber, byte packetNumber) {
        buffer = ByteBuffer.allocate(PACKET_SIZE);

        // session number
        this.sessionNumber = sessionNumber;
        buffer.putShort(sessionNumber); // 16 bit

        // packet number
        this.packetNumber = packetNumber;
        buffer.put(packetNumber); // 8 bit
    }

    public SWAckPacket() {
    }

    public boolean setData(byte[] data) {
        buffer = ByteBuffer.allocate(PACKET_SIZE);
        buffer.put(Arrays.copyOfRange(data, 0, PACKET_SIZE));

        // check if buffer data is valid
        packetNumber = buffer.get(2);
        if (packetNumber != 0 && packetNumber != 1) {
            packetNumber = 0;
            buffer.clear();
            return false;
        }

        // write buffer data in vars
        sessionNumber = buffer.getShort(0);

        return true;
    }
}