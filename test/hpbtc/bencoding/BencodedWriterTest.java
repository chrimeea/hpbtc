package hpbtc.bencoding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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
    public void testBencodedString() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(10);
        BencodingWriter w = new BencodingWriter(os);
        w.write("penelopa");
        assert os.toString("ISO-8859-1").equals("8:penelopa") : "Error encoded string";        
    }
    
    @Test
    public void testBencodedInteger() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(8);
        BencodingWriter w = new BencodingWriter(os);
        w.write(-12908);
        assert os.toString("ISO-8859-1").equals("i-12908e") : "Error encoded integer";
    }
    
    @Test
    public void testBencodedList() throws IOException {
        List l = new ArrayList(3);
        l.add(4527120);
        l.add("dvd");
        List ll = new ArrayList(1);
        ll.add("nemo");
        l.add(ll);
        ByteArrayOutputStream os = new ByteArrayOutputStream(24);
        BencodingWriter w = new BencodingWriter(os);
        w.write(l);
        assert os.toString("ISO-8859-1").equals("li4527120e3:dvdl4:nemoee") : "Error encoded list";
    }
    
    @Test
    public void testBencodedDictionary() throws IOException {
        Map mm = new HashMap(1);
        mm.put("second", "value");
        Map m = new HashMap(2);
        m.put("mykey", mm);
        m.put("another", "myvalue");
        ByteArrayOutputStream os = new ByteArrayOutputStream(44);
        BencodingWriter w = new BencodingWriter(os);
        w.write(m);
        assert os.toString("ISO-8859-1").equals("d7:another7:myvalue5:mykeyd6:second5:valueee") : "Error encoded map";
    }
}
