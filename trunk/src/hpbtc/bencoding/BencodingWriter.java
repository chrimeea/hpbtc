package hpbtc.bencoding;

import util.ByteStringComparator;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author Cristian Mocanu
 */
public class BencodingWriter {

    private String byteEncoding = "US-ASCII";
    private OutputStream os;

    public BencodingWriter(final OutputStream os) {
        this.os = os;
    }

    public void write(final Number n) throws IOException {
        os.write((byte) 'i');
        if (n == null) {
            os.write((byte) '0');
        } else {
            os.write(n.toString().getBytes(byteEncoding));
        }
        os.write((byte) 'e');
    }

    public void write(final byte[] s) throws IOException {
        if (s == null || s.length == 0) {
            os.write((byte) '0');
            os.write((byte) ':');
        } else {
            os.write(String.valueOf(s.length).getBytes(byteEncoding));
            os.write((byte) ':');
            os.write(s);
        }
    }

    public void write(final List l) throws IOException {
        os.write((byte) 'l');
        for (Object o : l) {
            write(o);
        }
        os.write((byte) 'e');
    }

    public void write(final Map<byte[], Object> m) throws IOException {
        os.write((byte) 'd');
        final Set<byte[]> s = new TreeSet<byte[]>(new ByteStringComparator());
        s.addAll(m.keySet());
        for (byte[] key : s) {
            write(key);
            write(m.get(key));
        }
        os.write((byte) 'e');
    }

    private void write(final Object o) throws IOException {
        if (o instanceof Number) {
            write((Number) o);
        } else if (o instanceof byte[]) {
            write((byte[]) o);
        } else if (o instanceof List) {
            write((List) o);
        } else if (o instanceof Map) {
            write((Map<byte[], Object>) o);
        } else {
            throw new BencodingException("Wrong type");
        }
    }
}
