package hpbtc.util;

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
        ByteBuffer b = ByteBuffer.allocate(1);
        try {
            int rep = IOUtil.readFromChannel(new ChannelStub(0), b);
            assert rep == 0 : "Incorrect bytes read";
            assert b.remaining() == 1 : "Incorrect buffer state";
        } catch (IOException e) {
            assert false : e.getMessage();
        }
    }

    @Test
    public void testReadUntilEndChannel() {
        ByteBuffer b = ByteBuffer.allocate(15);
        try {
            int rep = IOUtil.readFromChannel(new ChannelStub(10, 4), b);
            assert rep == 10 : "Incorrect bytes read";
            assert b.remaining() == 5 : "Incorrect buffer state";
        } catch (IOException e) {
            assert false : e.getMessage();
        }
    }

    @Test
    public void testReadUntilEndBuffer() {
        ByteBuffer b = ByteBuffer.allocate(10);
        try {
            int rep = IOUtil.readFromChannel(new ChannelStub(12, 5), b);
            assert rep == 10 : "Incorrect bytes read ";
            assert b.remaining() == 0 : "Incorrect buffer state";
        } catch (IOException e) {
            assert false : e.getMessage();
        }
    }

    @Test
    public void testReadBufferEmpty() {
        ByteBuffer b = ByteBuffer.allocate(0);
        try {
            int rep = IOUtil.readFromChannel(new ChannelStub(1), b);
            assert rep == 0 : "Incorrect bytes read";
            assert b.remaining() == 0 : "Incorrect buffer state";
        } catch (IOException e) {
            assert false : e.getMessage();
        }
    }
    
    @Test
    public void testWriteToChannel() {
    }
}
