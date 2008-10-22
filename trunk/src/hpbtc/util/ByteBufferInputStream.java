/*
 * Created on 21.10.2008
 */
package hpbtc.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class ByteBufferInputStream extends InputStream {

    private ByteBuffer buf;

    public ByteBufferInputStream(final ByteBuffer bb) {
        this.buf = bb;
    }

    @Override
    public int read() throws IOException {
        if (!buf.hasRemaining()) {
            return -1;
        }
        return buf.get();
    }

    @Override
    public int read(byte[] bytes, int off, int len) throws IOException {
        len = Math.min(len, buf.remaining());
        buf.get(bytes, off, len);
        return len;
    }

    @Override
    public boolean markSupported() {
        return true;
    }
    
    @Override
    public void mark(final int mark) {
        buf.mark();
    }

    @Override
    public void reset() throws IOException {
        buf.reset();
    }
}
