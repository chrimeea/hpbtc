package hpbtc.protocol.message;

import java.nio.ByteBuffer;
import java.util.BitSet;
import org.junit.Test;

/**
 *
 * @author Administrator
 */
public class MessageTest {

    @Test
    public void testReadBitfield() {
        ByteBuffer bb = ByteBuffer.allocate(3);
        bb.putShort((short) 63472);
        bb.put((byte) 0);
        bb.rewind();
        BitfieldMessage m = new BitfieldMessage(bb, 3);
        BitSet bs = m.getBitfield();
        assert bs.nextSetBit(0) == 0;
        assert bs.nextSetBit(1) == 1;
        assert bs.nextSetBit(2) == 2;
        assert bs.nextSetBit(3) == 3;
        assert bs.nextSetBit(4) == 5;
        assert bs.nextSetBit(6) == 6;
        assert bs.nextSetBit(7) == 7;
        assert bs.nextSetBit(8) == 8;
        assert bs.nextSetBit(9) == 9;
        assert bs.nextSetBit(10) == 10;
        assert bs.nextSetBit(11) == 11;
        assert bs.nextSetBit(12) == -1;
    }
    
    @Test
    public void testWriteBitfield() {
        BitSet bs = new BitSet(20);
        bs.set(1); bs.set(2); bs.set(3); bs.set(7);
        bs.set(8); bs.set(9); bs.set(13); bs.set(14);
        BitfieldMessage m = new BitfieldMessage(bs, 20);
        ByteBuffer bb = m.send();
        bb.rewind();
        assert bb.getInt() == 4;
        assert bb.get() == SimpleMessage.TYPE_BITFIELD;
        assert bb.getShort() == 29126;
        assert bb.get() == 0;
        assert bb.remaining() == 0;
    }
}
