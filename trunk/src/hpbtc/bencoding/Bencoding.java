/*
 * Created on Jan 18, 2006
 *
 */
package hpbtc.bencoding;

import hpbtc.bencoding.element.BencodedDictionary;
import hpbtc.bencoding.element.BencodedElement;
import hpbtc.bencoding.element.BencodedInteger;
import hpbtc.bencoding.element.BencodedList;
import hpbtc.bencoding.element.BencodedString;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author chris
 *
 */
public class Bencoding {
    
    private static int readNumber(InputStream is, char terminator) throws IOException {
        int c = is.read();
        if (c == terminator) {
            throw new BencodingException("Parse error !");
        }
        int sign = -1;
        int n = 0;
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
                throw new BencodingException("Found number containing illegal character: '" + (char) c + "'");
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
    
    private static BencodedString readString(InputStream is) throws IOException {
        int n = readNumber(is, ':');
        if (n < 0) {
            throw new BencodingException("Found string element with negative length");
        }
        BencodedString bs = new BencodedString("");
        if (n > 0) {
            byte[] dst = new byte[n];
            int s = 0;
            while (s < n) {
                s += is.read(dst, s, n - s);
            }
            bs.setValue(dst);
        }
        return bs;
    }
    
    private static BencodedInteger readInteger(InputStream is) throws IOException {
        int c = is.read();
        if (c != 'i') {
            throw new BencodingException("Found char: '" + (char) c + "', required: 'i'");
        }
        return new BencodedInteger(readNumber(is, 'e'));
    }
    
    private static BencodedList readList(InputStream is) throws IOException {
        BencodedList r = new BencodedList();
        int c = is.read();
        if (c != 'l') {
            throw new BencodingException("Found char: '" + (char) c + "', required: 'l'");
        }
        is.mark(1);
        c = is.read();
        while (c != 'e') {
            is.reset();
            BencodedElement o = readElement(is);
            r.addElement(o);
            is.mark(1);
            c = is.read();
        }
        return r;
    }
    
    private static BencodedDictionary readDictionary(InputStream is) throws IOException {
        BencodedDictionary r = new BencodedDictionary();
        int c = is.read();
        if (c != 'd') {
            throw new BencodingException("Found char: '" + (char) c + "', required: 'd'");
        }
        is.mark(1);
        c = is.read();
        while (c != 'e') {
            is.reset();
            BencodedString key = readString(is);
            BencodedElement value = readElement(is);
            r.addKeyValuePair(key, value);
            is.mark(1);
            c = is.read();
        }
        return r;
    }
    
    private static BencodedElement readElement(InputStream is) throws IOException {
        BencodedElement r;
        is.mark(1);
        int c = is.read();
        is.reset();
        if (Character.isDigit(c)) {
            r = readString(is);
        } else if (c == 'i') {
            r = readInteger(is);
        } else if (c == 'l') {
            r = readList(is);
        } else if (c == 'd') {
            r = readDictionary(is);
        } else {
            throw new BencodingException("Unrecognized element type: " + (char) c);
        }
        return r;
    }
    
    /**
     * @param is
     * @return
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static BencodedElement getTopElement(InputStream is) throws IOException, FileNotFoundException {
        if (!is.markSupported()) {
            throw new BencodingException("Input data is not buffered");
        }
        BencodedElement r = readElement(is);
        if (is.read() > -1) {
            throw new BencodingException("The file contains more than one top element");
        }
        return r;
    }
}
