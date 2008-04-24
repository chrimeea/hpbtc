package hpbtc.bencoding;

import hpbtc.bencoding.BencodingException;
import hpbtc.bencoding.element.BencodedDictionary;
import hpbtc.bencoding.element.BencodedElement;
import hpbtc.bencoding.element.BencodedInteger;
import hpbtc.bencoding.element.BencodedList;
import hpbtc.bencoding.element.BencodedString;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import org.junit.Test;

/**
 *
 * @author Cristian Mocanu
 */
public class BencodingParserTest {

    @Test
    public void testReadNextString() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("4:test".getBytes("ISO-8859-1"));
            BencodingParser parser = new BencodingParser(is);
            BencodedString string = parser.readNextString();
            assert string.equals("test") : "String is incorrect";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
    }

    @Test(expected = BencodingException.class)
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
    public void testReadNextInteger() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("i89e".getBytes("ISO-8859-1"));
            BencodingParser parser = new BencodingParser(is);
            BencodedInteger i = parser.readNextInteger();
            assert i.getValue() == 89 : "Integer is incorrect";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
    }

    @Test(expected = BencodingException.class)
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

    @Test(expected = BencodingException.class)
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
    public void testReadNextList() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("l5:gammai-13ee".getBytes("ISO-8859-1"));
            BencodingParser parser = new BencodingParser(is);
            BencodedList list = parser.readNextList();
            Iterator<BencodedElement> it = list.iterator();
            BencodedString elString = (BencodedString) it.next();
            assert elString.equals("gamma") : "Incorrect list";
            BencodedInteger elInteger = (BencodedInteger) it.next();
            assert elInteger.getValue() == -13 : "Incorrect list";
            assert !it.hasNext() : "Incorrect list";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
    }

    @Test(expected=BencodingException.class)
    public void testReadNextListWithTwoLists() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("lli1eeie".getBytes("ISO-8859-1"));
            BencodingParser parser = new BencodingParser(is);
            parser.readNextList();
            assert false : "Incorrect list";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
    }
    
    @Test
    public void readNextDictionary() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("d3:cow3:moo4:spami60ee".getBytes("ISO-8859-1"));
            BencodingParser parser = new BencodingParser(is);
            BencodedDictionary dict = parser.readNextDictionary();
            Iterator<BencodedString> keys = dict.getKeys().iterator();
            BencodedString elStringKey = keys.next();
            assert elStringKey.equals("cow") : "Incorrect dictionary";
            BencodedString elStringValue = (BencodedString) dict.get(elStringKey);
            assert elStringValue.equals("moo");
            elStringKey = keys.next();
            assert elStringKey.equals("spam") : "Incorrect dictionary";
            BencodedInteger elIntValue = (BencodedInteger) dict.get(elStringKey);
            assert elIntValue.getValue() == 60 : "Incorrect dictionary";
            assert !keys.hasNext() : "Incorrect dictionary";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
    }

    @Test(expected=BencodingException.class)
    public void readNextDictionaryWithError() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("di8e3:vale".getBytes("ISO-8859-1"));
            BencodingParser parser = new BencodingParser(is);
            parser.readNextDictionary();
            assert false : "Incorrect dictionary";
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
    }
    
    @Test
    public void readNextElement() throws IOException {
        try {
            ByteArrayInputStream is = new ByteArrayInputStream("10:calculator".getBytes("ISO-8859-1"));
            BencodingParser parser = new BencodingParser(is);
            assert parser.readNextString() instanceof BencodedString;
            is = new ByteArrayInputStream("i-9e".getBytes("ISO-8859-1"));
            parser = new BencodingParser(is);
            assert parser.readNextElement() instanceof BencodedInteger;
            is = new ByteArrayInputStream("li55el1:qd2:pp1:kee3:abce".getBytes("ISO-8859-1"));
            parser = new BencodingParser(is);
            assert parser.readNextElement() instanceof BencodedList;
            is = new ByteArrayInputStream("d1:w2:ab1:yli1eee".getBytes("ISO-8859-1"));
            parser = new BencodingParser(is);
            assert parser.readNextElement() instanceof BencodedDictionary;
        } catch (UnsupportedEncodingException e) {
            assert false : e.getMessage();
        }
    }
}
