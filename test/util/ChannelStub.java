package util;

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
    private int chunk;
    
    public ChannelStub(int size, int chunk) {
        ch = ByteBuffer.allocate(size);
        this.chunk = chunk <= size ? chunk : size;
    }
    
    public ChannelStub(int size) {
        this(size, size);
    }
    
    public int read(ByteBuffer destination) throws IOException {
        int s = 0;
        while (ch.hasRemaining() && destination.hasRemaining() && s < chunk) {
            destination.put(ch.get());
            s++;
        }
        return s;
    }

    public int write(ByteBuffer source) throws IOException {
        int s = 0;
        while (ch.hasRemaining() && source.hasRemaining() && s < chunk) {
            ch.put(source.get());
            s++;
        }
        return s;
    }

    public void close() throws IOException {
        ch.clear();
    }

    public boolean isOpen() {
        return true;
    }
    
    public int remaining() {
        return ch.remaining();
    }
}
