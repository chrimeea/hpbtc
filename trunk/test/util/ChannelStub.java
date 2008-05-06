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
    private boolean closed;
    
    public ChannelStub(int size, int chunk, boolean closed) {
        ch = ByteBuffer.allocate(size);
        this.chunk = chunk <= size ? chunk : size;
        this.closed = closed;
    }
    
    public ChannelStub(int size) {
        this(size, size, false);
    }
    
    public int read(ByteBuffer destination) throws IOException {
        if (!ch.hasRemaining() && closed) {
            return -1;
        }
        int s = 0;
        while (ch.hasRemaining() && destination.hasRemaining() && s < chunk) {
            destination.put(ch.get());
            s++;
        }
        return s;
    }

    public int write(ByteBuffer source) throws IOException {
        if (!ch.hasRemaining() && closed) {
            return -1;
        }
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
