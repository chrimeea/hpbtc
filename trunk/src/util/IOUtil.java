/*
 * Created on Jan 24, 2006
 *
 */
package util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * @author chris
 *
 */
public class IOUtil {

    public static int readFromChannel(ReadableByteChannel s, ByteBuffer b) throws IOException {
        int x = b.remaining();
        int c = s.read(b);
        int r = c;
        while (c != 0 && c != -1 && r < x) {
            c = s.read(b);
            r += c;
        }
        return c == -1 ? r + 1 : r;
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
}