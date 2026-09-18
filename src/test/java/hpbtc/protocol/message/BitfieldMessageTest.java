package hpbtc.protocol.message;

import java.nio.ByteBuffer;
import java.util.BitSet;

import org.junit.Test;

public class BitfieldMessageTest {

    @Test
    public void testBytesToBits() {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.putShort((short) 356);
        bb.rewind();
        BitSet bs = BitfieldMessage.bytesToBits(bb);
        assert bs.nextSetBit(0) == 7;
        assert bs.nextSetBit(8) == 9;
        assert bs.nextSetBit(10) == 10;
        assert bs.nextSetBit(11) == 13;
        assert bs.nextSetBit(14) == -1;
    }

    @Test
    public void testBitsToBytes() {
        BitSet bs = new BitSet(16);
        bs.set(7);
        bs.set(9);
        bs.set(10);
        bs.set(13);
        ByteBuffer bb = ByteBuffer.allocate(2);
        BitfieldMessage.bitsToBytes(bs, bb);
        bb.rewind();
        assert bb.getShort() == 356;
    }
}
