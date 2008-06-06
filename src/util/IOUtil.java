/*
 * Created on Jan 24, 2006
 *
 */
package util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.BitSet;

/**
 * @author chris
 *
 */
public class IOUtil {

    public static InetSocketAddress getAddress(SocketChannel c) {
        return (InetSocketAddress) c.socket().getRemoteSocketAddress();
    }

    public static int readFromChannel(ReadableByteChannel s, ByteBuffer b) throws IOException {
        int x = b.remaining();
        int c = s.read(b);
        int r = c;
        while (c != 0 && c != -1 && r < x) {
            c = s.read(b);
            r += c;
        }
        if (c == -1) {
            throw new IOException("Should close channel");
        }
        return r;
    }

    public static int writeToChannel(WritableByteChannel s, ByteBuffer b) throws IOException {
        int x = b.remaining();
        int c = s.write(b);
        int r = c;
        while (c != 0 && r < x) {
            c = s.write(b);
            r += c;
        }
        return r;
    }
    
    public static BitSet bytesToBits(ByteBuffer bb) {
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
    
    public static ByteBuffer bitsToBytes(BitSet bs) {
        int len = bs.size();
        ByteBuffer bb = ByteBuffer.allocate(len);
        byte x = 0;
        byte y = (byte) -128;
        for (int i = 0; i < len; i++) {
            if (i % 8 == 0 && i != 0) {
                bb.put(x);
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
        return bb;
    }
}
