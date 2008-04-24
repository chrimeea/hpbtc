package hpbtc.bencoding;

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
                is.close();
            } catch (IOException e) {
                assert false : e.getMessage();
            }
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
    }

    @Test
    public void testReadNextInteger() {
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
