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
        byte[] b = s.getEncoded();
        assert Arrays.equals(b, "8:penelopa".getBytes("ISO-8859-1")) : "Error encoded string";
    }
    
    @Test
    public void testBencodedInteger() throws UnsupportedEncodingException {
        BencodedInteger i = new BencodedInteger(-12908);
        byte[] b = i.getEncoded();
        assert Arrays.equals(b, "i-12908e".getBytes("ISO-8859-1")) : "Error encoded integer";
    }
}
