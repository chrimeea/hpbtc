package hpbtc.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

/**
 *
 * @author Cristian Mocanu
 */
public class ChannelStub implements ByteChannel {

    private ByteBuffer ch;
    private boolean closed;
    
    public ChannelStub(int size, boolean closed) {
        ch = ByteBuffer.allocate(size);
        this.closed = closed;
    }
    
    public int read(ByteBuffer destination) throws IOException {
        if (closed) {
            return -1;
        }
        int s = 0;
        while (ch.hasRemaining() && destination.hasRemaining()) {
            destination.put(ch.get());
            s++;
        }
        return s;
    }

    public int write(ByteBuffer source) throws IOException {
        if (closed) {
            return -1;
        }
        int s = 0;
        while (ch.hasRemaining() && source.hasRemaining()) {
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
