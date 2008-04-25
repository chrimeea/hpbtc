package hpbtc.bencoding.element;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import org.junit.Test;

/**
 *
 * @author Cristian Mocanu
 */
public class BencodedElementTest {

    @Test
    public void testBencodedString() throws UnsupportedEncodingException {
        BencodedString s = new BencodedString("penelopa");
        assert Arrays.equals(s.getEncoded(), "8:penelopa".getBytes("ISO-8859-1")) : "Error encoded string";
    }
    
    @Test
    public void testBencodedInteger() throws UnsupportedEncodingException {
        BencodedInteger i = new BencodedInteger(-12908);
        assert Arrays.equals(i.getEncoded(), "i-12908e".getBytes("ISO-8859-1")) : "Error encoded integer";
    }
    
    @Test
    public void testBencodedList() throws UnsupportedEncodingException {
        BencodedList l = new BencodedList();
        l.addElement(new BencodedInteger(4527120));
        l.addElement(new BencodedString("dvd"));
        BencodedList ll = new BencodedList();
        ll.addElement(new BencodedString("nemo"));
        l.addElement(ll);
        assert Arrays.equals(l.getEncoded(), "li4527120e3:dvdl4:nemoee".getBytes("ISO-8859-1")) : "Error encoded list";
    }
    
    @Test
    public void testBencodedDictionary() throws UnsupportedEncodingException {
        BencodedDictionary d = new BencodedDictionary();
        BencodedDictionary dd = new BencodedDictionary();
        dd.put(new BencodedString("second"), new BencodedString("value"));
        d.put(new BencodedString("mykey"), dd);
        d.put(new BencodedString("another"), new BencodedString("myvalue"));
        assert Arrays.equals(d.getEncoded(), "d7:another7:myvalue5:mykeyd6:second5:valueee".getBytes("ISO-8859-1")) : "Error encoded dictionary";
    }
}
