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

    @Test
    public void testWriteBencodedString() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(10);
        BencodingWriter w = new BencodingWriter(os);
        w.write("penelopa");
        assert os.toString("US-ASCII").equals("8:penelopa") : "Error encoded string";        
    }
    
    @Test
    public void testWriteBencodedByteString() throws IOException {
        byte[] x = new byte[10];
        for (byte i = 0; i < x.length; i++) {
            x[i] = i;
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream(10);
        BencodingWriter w = new BencodingWriter(os);
        w.write(new String(x, "US-ASCII"));
        String r = os.toString("US-ASCII");
        assert r.substring(0, 3).equals("10:") : "Error encoded string";
        byte[] b = r.substring(3).getBytes("US-ASCII");
        assert Arrays.equals(x, b) : "Error encoded string";
    }
    
    @Test
    public void testWriteBencodedInteger() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(8);
        BencodingWriter w = new BencodingWriter(os);
        w.write(-12908);
        assert os.toString("US-ASCII").equals("i-12908e") : "Error encoded integer";
    }
    
    @Test
    public void testWriteBencodedList() throws IOException {
        List l = new ArrayList(3);
        l.add(4527120);
        l.add("dvd");
        List ll = new ArrayList(1);
        ll.add("nemo");
        l.add(ll);
        ByteArrayOutputStream os = new ByteArrayOutputStream(24);
        BencodingWriter w = new BencodingWriter(os);
        w.write(l);
        assert os.toString("US-ASCII").equals("li4527120e3:dvdl4:nemoee") : "Error encoded list";
    }
    
    @Test
    public void testWriteBencodedDictionary() throws IOException {
        Map mm = new HashMap(1);
        mm.put("second", "value");
        Map m = new HashMap(2);
        m.put("mykey", mm);
        m.put("another", "myvalue");
        ByteArrayOutputStream os = new ByteArrayOutputStream(44);
        BencodingWriter w = new BencodingWriter(os);
        w.write(m);
        assert os.toString("US-ASCII").equals("d7:another7:myvalue5:mykeyd6:second5:valueee") : "Error encoded map";
    }
}
