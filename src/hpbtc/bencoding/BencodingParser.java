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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author chris
 *
 */
public class BencodingParser {
    
    private BufferedInputStream is;
    
    public BencodingParser(InputStream is) {
        if (!(is instanceof BufferedInputStream)) {
            this.is = new BufferedInputStream(is);
        } else {
            this.is = (BufferedInputStream) is;
        }
    }
    
    public BencodingParser(String fileName) throws FileNotFoundException {
        this(new BufferedInputStream(new FileInputStream(fileName)));
    }
    
    private int readNextNumber(char terminator) throws IOException {
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
    
    private BencodedString readNextString() throws IOException {
        int n = readNextNumber(':');
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
    
    private BencodedInteger readNextInteger() throws IOException {
        int c = is.read();
        if (c != 'i') {
            throw new BencodingException("Found char: '" + (char) c + "', required: 'i'");
        }
        return new BencodedInteger(readNextNumber('e'));
    }
    
    private BencodedList readNextList() throws IOException {
        BencodedList r = new BencodedList();
        int c = is.read();
        if (c != 'l') {
            throw new BencodingException("Found char: '" + (char) c + "', required: 'l'");
        }
        is.mark(1);
        c = is.read();
        while (c != 'e') {
            is.reset();
            BencodedElement o = readNextElement();
            r.addElement(o);
            is.mark(1);
            c = is.read();
        }
        return r;
    }
    
    private BencodedDictionary readNextDictionary() throws IOException {
        BencodedDictionary r = new BencodedDictionary();
        int c = is.read();
        if (c != 'd') {
            throw new BencodingException("Found char: '" + (char) c + "', required: 'd'");
        }
        is.mark(1);
        c = is.read();
        while (c != 'e') {
            is.reset();
            BencodedString key = readNextString();
            BencodedElement value = readNextElement();
            r.addKeyValuePair(key, value);
            is.mark(1);
            c = is.read();
        }
        return r;
    }
    
    private BencodedElement readNextElement() throws IOException {
        BencodedElement r;
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
            throw new BencodingException("Unrecognized element type: " + (char) c);
        }
        return r;
    }
    
    /**
     * @param is
     * @return
     * @throws IOException
     */
    public BencodedElement parse() throws IOException {
        BencodedElement r = readNextElement();
        if (is.read() > -1) {
            throw new BencodingException("The file contains more than one top element");
        }
        is.close();
        return r;
    }
}
