package util;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.Test;

/**
 *
 * @author Cristian Mocanu
 */
public class IOUtilTest {

    @Test
    public void testReadNothingFromChannel() {
        try {
            ByteBuffer b = ByteBuffer.allocate(1);
            int rep = IOUtil.readFromChannel(new ChannelStub(0), b);
            assert rep == 0 : "Incorrect bytes read";
            assert b.remaining() == 1 : "Incorrect buffer state";
        } catch (IOException e) {
            assert false : e.getMessage();
        }
    }

    @Test
    public void testReadUntilEndChannel() {
        try {
            ByteBuffer b = ByteBuffer.allocate(15);
            ChannelStub ch = new ChannelStub(10, 4);
            int rep = IOUtil.readFromChannel(ch, b);
            assert rep == 10 : "Incorrect bytes read";
            assert ch.remaining() == 0 : "Incorrect channel state";
            assert b.remaining() == 5 : "Incorrect buffer state";
        } catch (IOException e) {
            assert false : e.getMessage();
        }
    }

    @Test
    public void testReadUntilEndBuffer() {
        try {
            ByteBuffer b = ByteBuffer.allocate(10);
            ChannelStub ch = new ChannelStub(12, 5);
            int rep = IOUtil.readFromChannel(ch, b);
            assert rep == 10 : "Incorrect bytes read ";
            assert ch.remaining() == 2 : "Incorrect channel state";
            assert b.remaining() == 0 : "Incorrect buffer state";
        } catch (IOException e) {
            assert false : e.getMessage();
        }
    }

    @Test
    public void testReadBufferEmpty() {
        try {
            ChannelStub ch = new ChannelStub(1);
            int rep = IOUtil.readFromChannel(ch, ByteBuffer.allocate(0));
            assert rep == 0 : "Incorrect bytes read";
            assert ch.remaining() == 1 : "Incorrect channel state";
        } catch (IOException e) {
            assert false : e.getMessage();
        }
    }
    
    @Test
    public void testWriteNothingToChannel() {
        try {
            ChannelStub ch = new ChannelStub(1);
            int rep = IOUtil.writeToChannel(ch, ByteBuffer.allocate(0));
            assert rep == 0 : "Incorrect bytes read";
            assert ch.remaining() == 1 : "Incorrect channel state";
        } catch (IOException e) {
            assert false : e.getMessage();
        }
    }
    
    @Test
    public void testWriteUntilEndChannel() {
        try {
            ByteBuffer b = ByteBuffer.allocate(15);
            ChannelStub ch = new ChannelStub(10, 4);
            int rep = IOUtil.writeToChannel(ch, b);
            assert rep == 10 : "Incorrect bytes read";
            assert ch.remaining() == 0 : "Incorrect channel state";
            assert b.remaining() == 5 : "Incorrect buffer state";
        } catch (IOException e) {
            assert false : e.getMessage();
        }
    }
    
    @Test
    public void testWriteUntilEndBuffer() {
        try {
            ChannelStub ch = new ChannelStub(12, 5);
            ByteBuffer b = ByteBuffer.allocate(10);
            int rep = IOUtil.writeToChannel(ch, b);
            assert rep == 10 : "Incorrect bytes read ";
            assert ch.remaining() == 2 : "Incorrect channel state";
            assert b.remaining() == 0 : "Incorrect buffer state";
        } catch (IOException e) {
            assert false : e.getMessage();
        }
    }
    
    @Test
    public void testWriteBufferEmpty() {
        try {
            ChannelStub ch = new ChannelStub(1);
            int rep = IOUtil.writeToChannel(ch, ByteBuffer.allocate(0));
            assert rep == 0 : "Incorrect bytes read";
            assert ch.remaining() == 1 : "Incorrect buffer state";
        } catch (IOException e) {
            assert false : e.getMessage();
        }
    }
}
