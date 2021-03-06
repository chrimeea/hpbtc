package hpbtc.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
        int rep = IOUtil.readFromChannel(new ChannelStub(0, false), b);
        assert rep == 0 : "Incorrect bytes read";
        assert b.remaining() == 1 : "Incorrect buffer state";
    }

    @Test
    public void testReadUntilEndChannel() throws IOException {
        ByteBuffer b = ByteBuffer.allocate(15);
        ChannelStub ch = new ChannelStub(10, false);
        int rep = IOUtil.readFromChannel(ch, b);
        assert rep == 10 : "Incorrect bytes read";
        assert ch.remaining() == 0 : "Incorrect channel state";
        assert b.remaining() == 5 : "Incorrect buffer state";
    }

    @Test
    public void testReadUntilClosedChannel() throws IOException {
        ByteBuffer b = ByteBuffer.allocate(15);
        ChannelStub ch = new ChannelStub(0, true);
        assert IOUtil.readFromChannel(ch, b) == -1;
        assert ch.remaining() == 0 : "Incorrect channel state";
        assert b.remaining() == 15 : "Incorrect buffer state";
    }

    @Test
    public void testReadUntilEndBuffer() throws IOException {
        ByteBuffer b = ByteBuffer.allocate(10);
        ChannelStub ch = new ChannelStub(12, false);
        int rep = IOUtil.readFromChannel(ch, b);
        assert rep == 10 : "Incorrect bytes read ";
        assert ch.remaining() == 2 : "Incorrect channel state";
        assert b.remaining() == 0 : "Incorrect buffer state";
    }

    @Test
    public void testReadBufferEmpty() throws IOException {
        ChannelStub ch = new ChannelStub(1, false);
        int rep = IOUtil.readFromChannel(ch, ByteBuffer.allocate(0));
        assert rep == 0 : "Incorrect bytes read";
        assert ch.remaining() == 1 : "Incorrect channel state";
    }

    @Test
    public void testWriteNothingToChannel() throws IOException {
        ChannelStub ch = new ChannelStub(1, false);
        int rep = IOUtil.writeToChannel(ch, ByteBuffer.allocate(0));
        assert rep == 0 : "Incorrect bytes read";
        assert ch.remaining() == 1 : "Incorrect channel state";
    }

    @Test
    public void testWriteUntilEndChannel() throws IOException {
        ByteBuffer b = ByteBuffer.allocate(15);
        ChannelStub ch = new ChannelStub(10, false);
        int rep = IOUtil.writeToChannel(ch, b);
        assert rep == 10 : "Incorrect bytes read";
        assert ch.remaining() == 0 : "Incorrect channel state";
        assert b.remaining() == 5 : "Incorrect buffer state";
    }

    @Test
    public void testWriteUntilEndBuffer() throws IOException {
        ChannelStub ch = new ChannelStub(12, false);
        ByteBuffer b = ByteBuffer.allocate(10);
        int rep = IOUtil.writeToChannel(ch, b);
        assert rep == 10 : "Incorrect bytes read ";
        assert ch.remaining() == 2 : "Incorrect channel state";
        assert b.remaining() == 0 : "Incorrect buffer state";
    }

    @Test
    public void testWriteBufferEmpty() throws IOException {
        ChannelStub ch = new ChannelStub(1, false);
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
        ByteBuffer bb = ByteBuffer.allocate(2);
        IOUtil.bitsToBytes(bs, bb);
        bb.rewind();
        assert bb.getShort() == 356;
    }

    @Test
    public void testWriteToFile() throws IOException {
        File f = File.createTempFile("HPBTC", null);
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.put((byte) 5);
        bb.put((byte) 8);
        bb.rewind();
        IOUtil.writeToFile(f, 1, bb);
        FileInputStream fis = new FileInputStream(f);
        fis.read();
        assert fis.read() == 5;
        assert fis.read() == 8;
        fis.close();
        f.delete();
    }

    @Test
    public void testReadFromFile() throws IOException {
        File f = File.createTempFile("HPBTC", null);
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(7);
        fos.write(1);
        fos.write(9);
        fos.write(3);
        fos.write(4);
        fos.close();
        ByteBuffer bb = ByteBuffer.allocate(2);
        IOUtil.readFromFile(f, 2, bb);
        bb.rewind();
        assert bb.get() == 9;
        assert bb.get() == 3;
        assert bb.remaining() == 0;
        f.delete();
    }
}
