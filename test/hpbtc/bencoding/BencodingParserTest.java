package hpbtc.bencoding;

import hpbtc.bencoding.element.BencodedInteger;
import hpbtc.bencoding.element.BencodedString;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.junit.Test;

/**
 *
 * @author Cristian Mocanu
 */
public class BencodingParserTest {

    @Test
    public void testReadNextString() {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("4:test".getBytes("ISO-8859-1"));
            BencodingParser parser = new BencodingParser(is);
            try {
                BencodedString string = parser.readNextString();
                assert string.equals("test") : "String is incorrect";
            } catch (IOException e) {
                assert false : e.getMessage();
            }
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
    }

    @Test(expected = hpbtc.bencoding.BencodingException.class)
    public void testReadNextStringWithWrongLength() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("4a:test".getBytes("ISO-8859-1"));
            BencodingParser parser = new BencodingParser(is);
            parser.readNextString();
            assert false : "String is incorrect";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
    }

    @Test
    public void testReadNextInteger() {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("i89e".getBytes("ISO-8859-1"));
            BencodingParser parser = new BencodingParser(is);
            try {
                BencodedInteger i = parser.readNextInteger();
                assert i.getValue() == 89 : "Integer is incorrect";
            } catch (IOException e) {
                assert false : e.getMessage();
            }
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
    }

    @Test(expected = hpbtc.bencoding.BencodingException.class)
    public void testReadNextIntegerWithoutEnding() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("i89".getBytes("ISO-8859-1"));
            BencodingParser parser = new BencodingParser(is);
            parser.readNextInteger();
            assert false : "Integer is incorrect";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
    }

    @Test(expected = hpbtc.bencoding.BencodingException.class)
    public void testReadNextIntegerWithZeroPrefix() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("i05e".getBytes("ISO-8859-1"));
            BencodingParser parser = new BencodingParser(is);
            parser.readNextInteger();
            assert false : "Integer is incorrect";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
    }
    
    @Test
    public void testReadNextList() {
    }

    @Test
    public void readNextDictionary() {
    }

    @Test
    public void readNextElement() {
    }
}
