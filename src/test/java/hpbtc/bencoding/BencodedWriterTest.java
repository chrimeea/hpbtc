package hpbtc.bencoding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 *
 * @author Cristian Mocanu
 */
public class BencodedWriterTest {

    private String byteEncoding = "US-ASCII";

    @Test
    public void testWriteBencodedString() throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream(10);
        final BencodingWriter w = new BencodingWriter(os);
        w.write("penelopa".getBytes(byteEncoding));
        assert os.toString(byteEncoding).equals("8:penelopa") :
                "Error encoded string";
    }

    @Test
    public void testWriteBencodedByteString() throws IOException {
        byte[] x = new byte[10];
        for (byte i = 0; i < x.length; i++) {
            x[i] = i;
        }
        final ByteArrayOutputStream os = new ByteArrayOutputStream(10);
        final BencodingWriter w = new BencodingWriter(os);
        w.write(x);
        final String r = os.toString(byteEncoding);
        assert r.substring(0, 3).equals("10:") : "Error encoded string";
        byte[] b = r.substring(3).getBytes(byteEncoding);
        assert Arrays.equals(x, b) : "Error encoded string";
    }

    @Test
    public void testWriteBencodedInteger() throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream(8);
        final BencodingWriter w = new BencodingWriter(os);
        w.write(-12908);
        assert os.toString(byteEncoding).equals("i-12908e") :
                "Error encoded integer";
    }

    @Test
    public void testWriteBencodedList() throws IOException {
        final List l = new ArrayList(3);
        l.add(4527120);
        l.add("dvd".getBytes(byteEncoding));
        final List ll = new ArrayList(1);
        ll.add("nemo".getBytes(byteEncoding));
        l.add(ll);
        final ByteArrayOutputStream os = new ByteArrayOutputStream(24);
        final BencodingWriter w = new BencodingWriter(os);
        w.write(l);
        assert os.toString(byteEncoding).equals("li4527120e3:dvdl4:nemoee") :
                "Error encoded list";
    }

    @Test
    public void testWriteBencodedDictionary() throws IOException {
        final Map mm = new HashMap(1);
        mm.put("second".getBytes(byteEncoding),
                "value".getBytes(byteEncoding));
        final Map m = new HashMap(2);
        m.put("mykey".getBytes(byteEncoding), mm);
        m.put("another".getBytes(byteEncoding),
                "myvalue".getBytes(byteEncoding));
        final ByteArrayOutputStream os = new ByteArrayOutputStream(44);
        final BencodingWriter w = new BencodingWriter(os);
        w.write(m);
        assert os.toString(byteEncoding).equals(
                "d7:another7:myvalue5:mykeyd6:second5:valueee") :
                "Error encoded map";
    }
}
