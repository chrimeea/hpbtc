package util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import org.junit.Test;

/**
 *
 * @author Cristian Mocanu
 */
public class IOUtilTest {

    @Test
    public void testReadNothingFromChannel() throws IOException {
        ByteBuffer b = ByteBuffer.allocate(1);
        int rep = IOUtil.readFromChannel(new ChannelStub(0), b);
        assert rep == 0 : "Incorrect bytes read";
        assert b.remaining() == 1 : "Incorrect buffer state";
    }

    @Test
    public void testReadUntilEndChannel() throws IOException {
        ByteBuffer b = ByteBuffer.allocate(15);
        ChannelStub ch = new ChannelStub(10, 4, false);
        int rep = IOUtil.readFromChannel(ch, b);
        assert rep == 10 : "Incorrect bytes read";
        assert ch.remaining() == 0 : "Incorrect channel state";
        assert b.remaining() == 5 : "Incorrect buffer state";
    }

    @Test
    public void testReadUntilClosedChannel() {
        ByteBuffer b = ByteBuffer.allocate(15);
        ChannelStub ch = new ChannelStub(10, 4, true);
        try {
            IOUtil.readFromChannel(ch, b);
            assert false;
        } catch (IOException e) {
            assert ch.remaining() == 0 : "Incorrect channel state";
            assert b.remaining() == 5 : "Incorrect buffer state";
        }
    }

    @Test
    public void testReadUntilEndBuffer() throws IOException {
        ByteBuffer b = ByteBuffer.allocate(10);
        ChannelStub ch = new ChannelStub(12, 5, false);
        int rep = IOUtil.readFromChannel(ch, b);
        assert rep == 10 : "Incorrect bytes read ";
        assert ch.remaining() == 2 : "Incorrect channel state";
        assert b.remaining() == 0 : "Incorrect buffer state";
    }

    @Test
    public void testReadBufferEmpty() throws IOException {
        ChannelStub ch = new ChannelStub(1);
        int rep = IOUtil.readFromChannel(ch, ByteBuffer.allocate(0));
        assert rep == 0 : "Incorrect bytes read";
        assert ch.remaining() == 1 : "Incorrect channel state";
    }

    @Test
    public void testWriteNothingToChannel() throws IOException {
        ChannelStub ch = new ChannelStub(1);
        int rep = IOUtil.writeToChannel(ch, ByteBuffer.allocate(0));
        assert rep == 0 : "Incorrect bytes read";
        assert ch.remaining() == 1 : "Incorrect channel state";
    }

    @Test
    public void testWriteUntilEndChannel() throws IOException {
        ByteBuffer b = ByteBuffer.allocate(15);
        ChannelStub ch = new ChannelStub(10, 4, false);
        int rep = IOUtil.writeToChannel(ch, b);
        assert rep == 10 : "Incorrect bytes read";
        assert ch.remaining() == 0 : "Incorrect channel state";
        assert b.remaining() == 5 : "Incorrect buffer state";
    }

    @Test
    public void testWriteUntilEndBuffer() throws IOException {
        ChannelStub ch = new ChannelStub(12, 5, false);
        ByteBuffer b = ByteBuffer.allocate(10);
        int rep = IOUtil.writeToChannel(ch, b);
        assert rep == 10 : "Incorrect bytes read ";
        assert ch.remaining() == 2 : "Incorrect channel state";
        assert b.remaining() == 0 : "Incorrect buffer state";
    }

    @Test
    public void testWriteBufferEmpty() throws IOException {
        ChannelStub ch = new ChannelStub(1);
        int rep = IOUtil.writeToChannel(ch, ByteBuffer.allocate(0));
        assert rep == 0 : "Incorrect bytes read";
        assert ch.remaining() == 1 : "Incorrect buffer state";
    }
    
    @Test
    public void testBytesToBits() {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.putShort((short) 356);
        bb.rewind();
        BitSet bs = IOUtil.bytesToBits(bb);
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
        ByteBuffer bb = IOUtil.bitsToBytes(bs);
        bb.rewind();
        assert bb.getShort() == 356;
        assert bb.remaining() == 0;
    }
}
