package hpbtc.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 *
 * @author Cristian Mocanu
 */
public class ChannelStub implements ReadableByteChannel, WritableByteChannel {

    private ByteBuffer ch;
    
    public ChannelStub(int size) {
        ch = ByteBuffer.allocate(size);
    }
    
    public int read(ByteBuffer destination) throws IOException {
        int s = ch.remaining();
        if (s == 0) {
            return -1;
        }
        while (ch.hasRemaining()) {
            destination.put(ch.get());
        }
        return s;
    }

    public int write(ByteBuffer source) throws IOException {
        int s = source.remaining();
        while (source.hasRemaining()) {
            ch.put(source.get());
        }
        return s;
    }

    public void close() throws IOException {
        ch.clear();
    }

    public boolean isOpen() {
        return true;
    }
}
