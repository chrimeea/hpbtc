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
import java.util.BitSet;

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

    public static BitSet bytesToBits(final ByteBuffer bb) {
        int len = bb.remaining();
        int j = 0;
        int k = 0;
        BitSet pieces = new BitSet(len * 8);
        for (int i = 0; i < len; i++) {
            byte bit = bb.get();
            byte c = (byte) 128;
            for (int p = 0; p < 8; p++) {
                if ((bit & c) == c) {
                    pieces.set(j);
                    k++;
                }
                bit <<= 1;
                j++;
            }
        }
        return pieces;
    }

    public static void bitsToBytes(final BitSet bs, final ByteBuffer dest) {
        int len = bs.length();
        byte x = 0;
        byte y = (byte) -128;
        for (int i = 0; i < len; i++) {
            if (i % 8 == 0 && i != 0) {
                dest.put(x);
                x = 0;
                y = (byte) -128;
            }
            if (bs.get(i)) {
                x |= y;
            }
            y >>= 1;
            if (y < 0) {
                y ^= (byte) -128;
            }
        }
        if (dest.remaining() > 0) {
            dest.put(x);
        }
    }

    public static int writeToFile(final File file, final long begin,
            final ByteBuffer piece) throws IOException {
        RandomAccessFile r = new RandomAccessFile(file, "rw");
        r.seek(begin);
        int i = writeToChannel(r.getChannel(), piece);
        r.close();
        return i;
    }

    public static int readFromFile(final File file, final long begin,
            final ByteBuffer dest) throws IOException {
        RandomAccessFile r = new RandomAccessFile(file, "r");
        r.seek(begin);
        int i = readFromChannel(r.getChannel(), dest);
        r.close();
        return i;
    }
}
