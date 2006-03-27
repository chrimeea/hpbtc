/*
 * Created on Jan 24, 2006
 *
 */
package hpbtc.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

/**
 * @author chris
 *
 */
public class IOUtil {

    /**
     * @param s
     * @param b
     * @return
     * @throws IOException
     */
    public static int readFromSocket(SocketChannel s, ByteBuffer b) throws IOException {
        int x = b.remaining();
        int c = s.read(b);
        int r = c;
        while (c != 0 && c != -1 && r < x) {
            c = s.read(b);
            r += c;
        }
        return c == -1 ? r + 1 : r;
    }
    
    /**
     * @param s
     * @param b
     * @param z
     * @return
     * @throws IOException
     */
    public static int readFromFile(FileChannel s, ByteBuffer b, int z) throws IOException {
        int r = s.read(b);
        while (r < z && b.remaining() > 0) {
            r += s.read(b);
        }
        s.close();
        return r;
    }
    
    public static int writeToFile(FileChannel s, ByteBuffer b) throws IOException {
        int r = s.write(b);
        while (r < b.limit()) {
            r += s.write(b);
        }
        s.close();
        return r;
    }    
    
    public static int writeToSocket(SocketChannel s, ByteBuffer b) throws IOException {
        int x = b.remaining();
        int c = s.write(b);
        int r = c;
        while (c != 0 && r < x) {
            c = s.write(b);
            r += c;
        }
        return r;
    }
}
