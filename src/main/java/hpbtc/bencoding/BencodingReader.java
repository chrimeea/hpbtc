/*
 * Created on Jan 18, 2006
 *
 */
package hpbtc.bencoding;

import hpbtc.util.ByteStringComparator;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Cristian Mocanu
 *
 */
public class BencodingReader {

    private InputStream is;

    public BencodingReader(final InputStream is) {
        if (is.markSupported()) {
            this.is = is;
        } else {
            this.is = new BufferedInputStream(is);
        }
    }
    
    private long readNextNumber(final char terminator) throws IOException {
        int c = is.read();
        if (c == terminator) {
            throw new BencodingException("Parse error !");
        }
        int sign = -1;
        long n = 0L;
        if (c != '-') {
            if (c == '0') {
                is.mark(1);
                int d = is.read();
                if (d != terminator) {
                    throw new BencodingException("Numbers must not start with 0");
                }
                is.reset();
            }
            sign = 1;
            n = c - 48;
        }
        c = is.read();
        while (c != terminator) {
            if (!Character.isDigit(c)) {
                throw new BencodingException("Found number containing illegal character: '" +
                        (char) c + "'");
            }
            n *= 10;
            n += c - 48;
            c = is.read();
        }
        if (n == 0 && sign == -1) {
            throw new BencodingException("Number -0 is illegal");
        }
        n *= sign;
        return n;
    }

    public byte[] readNextString() throws IOException {
        int n = (int) readNextNumber(':');
        if (n < 0) {
            throw new BencodingException(
                    "Found string element with negative length");
        }
        if (n > 0) {
            byte[] dst = new byte[n];
            int s = 0;
            int r;
            do {
                r = is.read(dst, s, n - s);
                s += r;
            } while (r > 0 && s < n);
            return dst;
        }
        return null;
    }

    public Long readNextInteger() throws IOException {
        final int c = is.read();
        if (c != 'i') {
            throw new BencodingException("Found char: '" + (char) c +
                    "', required: 'i'");
        }
        return new Long(readNextNumber('e'));
    }

    public List<Object> readNextList() throws IOException {
        final List<Object> r = new LinkedList<Object>();
        int c = is.read();
        if (c != 'l') {
            throw new BencodingException("Found char: '" + (char) c +
                    "', required: 'l'");
        }
        is.mark(1);
        c = is.read();
        while (c != 'e') {
            is.reset();
            r.add(readNextElement());
            is.mark(1);
            c = is.read();
        }
        return r;
    }

    public Map<byte[], Object> readNextDictionary() throws IOException {
        final Map<byte[], Object> r = new TreeMap<byte[], Object>(
                new ByteStringComparator());
        int c = is.read();
        if (c != 'd') {
            throw new BencodingException("Found char: '" + (char) c +
                    "', required: 'd'");
        }
        is.mark(1);
        c = is.read();
        while (c != 'e') {
            is.reset();
            byte[] key = readNextString();
            final Object value = readNextElement();
            r.put(key, value);
            is.mark(1);
            c = is.read();
        }
        return r;
    }

    private Object readNextElement() throws IOException {
        final Object r;
        is.mark(1);
        int c = is.read();
        is.reset();
        if (Character.isDigit(c)) {
            r = readNextString();
        } else if (c == 'i') {
            r = readNextInteger();
        } else if (c == 'l') {
            r = readNextList();
        } else if (c == 'd') {
            r = readNextDictionary();
        } else {
            throw new BencodingException("Unrecognized element type: " +
                    (char) c);
        }
        return r;
    }
}
