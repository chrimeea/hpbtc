package hpbtc.bencoding;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 *
 * @author Cristian Mocanu
 */
public class BencodingReaderTest {

    @Test
    public void testReadNextString() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream(
                    "4:test".getBytes("ISO-8859-1"));
            BencodingReader parser = new BencodingReader(is);
            String string = parser.readNextString();
            assert string.equals("test") : "String is incorrect";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getLocalizedMessage();
        }
    }

    @Test(expected = BencodingException.class)
    public void testReadNextStringWithWrongLength() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("4a:test".
                    getBytes("ISO-8859-1"));
            BencodingReader parser = new BencodingReader(is);
            parser.readNextString();
            assert false : "String is incorrect";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getLocalizedMessage();
        }
    }

    @Test
    public void testReadNextInteger() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("i89e".getBytes(
                    "ISO-8859-1"));
            BencodingReader parser = new BencodingReader(is);
            int i = parser.readNextInteger();
            assert i == 89 : "Integer is incorrect";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getLocalizedMessage();
        }
    }

    @Test(expected = BencodingException.class)
    public void testReadNextIntegerWithoutEnding() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("i89".getBytes(
                    "ISO-8859-1"));
            BencodingReader parser = new BencodingReader(is);
            parser.readNextInteger();
            assert false : "Integer is incorrect";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getLocalizedMessage();
        }
    }

    @Test(expected = BencodingException.class)
    public void testReadNextIntegerWithZeroPrefix() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("i05e".getBytes(
                    "ISO-8859-1"));
            BencodingReader parser = new BencodingReader(is);
            parser.readNextInteger();
            assert false : "Integer is incorrect";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getLocalizedMessage();
        }
    }

    @Test
    public void testReadNextList() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("l5:gammai-13ee".
                    getBytes("ISO-8859-1"));
            BencodingReader parser = new BencodingReader(is);
            List list = parser.readNextList();
            Iterator it = list.iterator();
            String elString = (String) it.next();
            assert elString.equals("gamma") : "Incorrect list";
            int elInteger = (Integer) it.next();
            assert elInteger == -13 : "Incorrect list";
            assert !it.hasNext() : "Incorrect list";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getLocalizedMessage();
        }
    }

    @Test(expected = BencodingException.class)
    public void testReadNextListWithTwoLists() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("lli1eeie".
                    getBytes("ISO-8859-1"));
            BencodingReader parser = new BencodingReader(is);
            parser.readNextList();
            assert false : "Incorrect list";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getLocalizedMessage();
        }
    }

    @Test
    public void readNextDictionary() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream(
                    "d3:cow3:moo4:spami60ee".getBytes("ISO-8859-1"));
            BencodingReader parser = new BencodingReader(is);
            Map<String, Object> dict = parser.readNextDictionary();
            Iterator<String> keys = dict.keySet().iterator();
            String elStringKey = keys.next();
            assert elStringKey.equals("cow") : "Incorrect dictionary";
            String elStringValue = (String) dict.get(elStringKey);
            assert elStringValue.equals("moo");
            elStringKey = keys.next();
            assert elStringKey.equals("spam") : "Incorrect dictionary";
            int elIntValue = (Integer) dict.get(elStringKey);
            assert elIntValue == 60 : "Incorrect dictionary";
            assert !keys.hasNext() : "Incorrect dictionary";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getLocalizedMessage();
        }
    }

    @Test(expected = BencodingException.class)
    public void readNextDictionaryWithError() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("di8e3:vale".
                    getBytes("ISO-8859-1"));
            BencodingReader parser = new BencodingReader(is);
            parser.readNextDictionary();
            assert false : "Incorrect dictionary";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getLocalizedMessage();
        }
    }
}
