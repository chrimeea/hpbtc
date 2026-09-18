/*
 * Created on Jan 24, 2006
 *
 */
package hpbtc.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

/**
 * @author Cristian Mocanu
 *
 */
public class IOUtil {

    public static InetSocketAddress getAddress(final SocketChannel c) {
        return (InetSocketAddress) c.socket().getRemoteSocketAddress();
    }

    public static int readFromChannel(final ReadableByteChannel s,
            final ByteBuffer b) throws IOException {
        return s.read(b);
    }

    public static int writeToChannel(final WritableByteChannel s,
            final ByteBuffer b) throws IOException {
        return s.write(b);
    }

    public static int writeToFile(final File file, final long begin,
            final ByteBuffer piece) throws IOException {
        final RandomAccessFile r = new RandomAccessFile(file, "rw");
        r.seek(begin);
        final int i = writeToChannel(r.getChannel(), piece);
        r.close();
        return i;
    }

    public static int readFromFile(final File file, final long begin,
            final ByteBuffer dest) throws IOException {
        final RandomAccessFile r = new RandomAccessFile(file, "r");
        r.seek(begin);
        final int i = readFromChannel(r.getChannel(), dest);
        r.close();
        return i;
    }
}
