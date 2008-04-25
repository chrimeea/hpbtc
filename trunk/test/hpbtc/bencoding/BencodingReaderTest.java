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
            ByteArrayInputStream is = new ByteArrayInputStream("4:test".getBytes("UTF-8"));
            BencodingReader parser = new BencodingReader(is);
            String string = parser.readNextString();
            assert string.equals("test") : "String is incorrect";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
    }

    @Test(expected = BencodingException.class)
    public void testReadNextStringWithWrongLength() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("4a:test".getBytes("UTF-8"));
            BencodingReader parser = new BencodingReader(is);
            parser.readNextString();
            assert false : "String is incorrect";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
    }

    @Test
    public void testReadNextInteger() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("i89e".getBytes("UTF-8"));
            BencodingReader parser = new BencodingReader(is);
            long i = parser.readNextInteger();
            assert i == 89L : "Integer is incorrect";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
    }

    @Test(expected = BencodingException.class)
    public void testReadNextIntegerWithoutEnding() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("i89".getBytes("UTF-8"));
            BencodingReader parser = new BencodingReader(is);
            parser.readNextInteger();
            assert false : "Integer is incorrect";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
    }

    @Test(expected = BencodingException.class)
    public void testReadNextIntegerWithZeroPrefix() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("i05e".getBytes("UTF-8"));
            BencodingReader parser = new BencodingReader(is);
            parser.readNextInteger();
            assert false : "Integer is incorrect";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
    }

    @Test
    public void testReadNextList() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("l5:gammai-13ee".getBytes("UTF-8"));
            BencodingReader parser = new BencodingReader(is);
            List list = parser.readNextList();
            Iterator it = list.iterator();
            String elString = (String) it.next();
            assert elString.equals("gamma") : "Incorrect list";
            long elInteger = (Long) it.next();
            assert elInteger == -13L : "Incorrect list";
            assert !it.hasNext() : "Incorrect list";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
    }

    @Test(expected=BencodingException.class)
    public void testReadNextListWithTwoLists() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("lli1eeie".getBytes("UTF-8"));
            BencodingReader parser = new BencodingReader(is);
            parser.readNextList();
            assert false : "Incorrect list";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
    }
    
    @Test
    public void readNextDictionary() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("d3:cow3:moo4:spami60ee".getBytes("UTF-8"));
            BencodingReader parser = new BencodingReader(is);
            Map<String, Object> dict = parser.readNextDictionary();
            Iterator<String> keys = dict.keySet().iterator();
            String elStringKey = keys.next();
            assert elStringKey.equals("cow") : "Incorrect dictionary";
            String elStringValue = (String) dict.get(elStringKey);
            assert elStringValue.equals("moo");
            elStringKey = keys.next();
            assert elStringKey.equals("spam") : "Incorrect dictionary";
            long elIntValue = (Long) dict.get(elStringKey);
            assert elIntValue == 60L : "Incorrect dictionary";
            assert !keys.hasNext() : "Incorrect dictionary";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
    }

    @Test(expected=BencodingException.class)
    public void readNextDictionaryWithError() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("di8e3:vale".getBytes("UTF-8"));
            BencodingReader parser = new BencodingReader(is);
            parser.readNextDictionary();
            assert false : "Incorrect dictionary";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
    }
}
