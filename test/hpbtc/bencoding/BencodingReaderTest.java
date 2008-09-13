package hpbtc.bencoding;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 *
 * @author Cristian Mocanu
 */
public class BencodingReaderTest {

    private String byteEncoding = "US-ASCII";

    @Test
    public void testReadNextString() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream(
                    "4:test".getBytes(byteEncoding));
            BencodingReader parser = new BencodingReader(is);
            byte[] string = parser.readNextString();
            assert Arrays.equals(string, "test".getBytes(byteEncoding)) :
                    "String is incorrect";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getLocalizedMessage();
        }
    }

    @Test(expected = BencodingException.class)
    public void testReadNextStringWithWrongLength() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("4a:test".
                    getBytes(byteEncoding));
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
                    byteEncoding));
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
                    byteEncoding));
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
                    byteEncoding));
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
                    getBytes(byteEncoding));
            BencodingReader parser = new BencodingReader(is);
            List list = parser.readNextList();
            Iterator it = list.iterator();
            byte[] elString = (byte[]) it.next();
            assert Arrays.equals(elString, "gamma".getBytes(byteEncoding)) :
                    "Incorrect list";
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
                    getBytes(byteEncoding));
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
                    "d3:cow3:moo4:spami60ee".getBytes(byteEncoding));
            BencodingReader parser = new BencodingReader(is);
            Map<byte[], Object> dict = parser.readNextDictionary();
            Iterator<byte[]> keys = dict.keySet().iterator();
            byte[] elStringKey = keys.next();
            assert Arrays.equals(elStringKey, "cow".getBytes(byteEncoding)) :
                    "Incorrect dictionary ";
            byte[] elStringValue = (byte[]) dict.get(elStringKey);
            assert Arrays.equals(elStringValue, "moo".getBytes(byteEncoding));
            elStringKey = keys.next();
            assert Arrays.equals(elStringKey, "spam".getBytes(byteEncoding)) :
                    "Incorrect dictionary";
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
                    getBytes(byteEncoding));
            BencodingReader parser = new BencodingReader(is);
            parser.readNextDictionary();
            assert false : "Incorrect dictionary";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getLocalizedMessage();
        }
    }
}
