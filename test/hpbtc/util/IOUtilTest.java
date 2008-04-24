package hpbtc.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import org.junit.Test;
import org.junit.Before;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Cristian Mocanu
 */
public class IOUtilTest {

    private ReadableByteChannel mockReadable;
    private WritableByteChannel mockWritable;

    @Before
    public void setUp() {
        mockReadable = createMock(ReadableByteChannel.class);
        mockWritable = createMock(WritableByteChannel.class);
    }

    @Test
    public void testReadNothingFromChannel() {
        ByteBuffer b = ByteBuffer.allocate(1);
        try {
            reset(mockReadable);
            expect(mockReadable.read(b)).andReturn(-1);
            replay(mockReadable);
            int rep = IOUtil.readFromChannel(mockReadable, b);
            verify(mockReadable);
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
            expect(mockReadable.read(b)).andReturn(4).andReturn(6).andReturn(-1);
            replay(mockReadable);
            int rep = IOUtil.readFromChannel(mockReadable, b);
            verify(mockReadable);
            reset(mockReadable);
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
            expect(mockReadable.read(b)).andReturn(4).andReturn(8);
            replay(mockReadable);
            int rep = IOUtil.readFromChannel(mockReadable, b);
            verify(mockReadable);
            reset(mockReadable);
            assert rep == 10 : "Incorrect bytes read ";
            assert b.remaining() == 0 : "Incorrect buffer state";
        } catch (IOException e) {
            assert false : e.getMessage();
        }
        reset(mockReadable);
    }

    @Test
    public void testReadBufferEmpty() {
        ByteBuffer b = ByteBuffer.allocate(0);
        try {
            expect(mockReadable.read(b)).andReturn(0);
            replay(mockReadable);
            int rep = IOUtil.readFromChannel(mockReadable, b);
            verify(mockReadable);
            reset(mockReadable);
            assert rep == 0 : "Incorrect bytes read";
            assert b.remaining() == 0 : "Incorrect buffer state";
        } catch (IOException e) {
            assert false : e.getMessage();
        }
        reset(mockReadable);
    }
    
    @Test
    public void testWriteToChannel() {
    }
}
