package hpbtc.bencoding;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 *
 * @author Cristian Mocanu
 */
public class BencodingWriter {

    private OutputStream os;
    
    public BencodingWriter(OutputStream os) {
        this.os = os;
    }
    
    public void write(Number n) throws IOException {
        os.write((int) 'i');
        if (n == null) {
            os.write((int) '0');
        } else {
            os.write(n.toString().getBytes("ISO-8859-1"));
        }
        os.write((int) 'e');
    }
    
    public void write(String s) throws IOException {
        if (s == null || s.isEmpty()) {
            os.write((int) '0');
            os.write((int) ':');
        } else {
            os.write((s.length() + ":" + s).getBytes("ISO-8859-1"));
        }
    }
    
    public void write(List l)  throws IOException {
        os.write((int) 'l');
        for (Object o: l) {
            write(o);
        }
        os.write((int) 'e');
    }
    
    public void write(Map m)  throws IOException {
        os.write((int) 'd');
        TreeSet<String> s = new TreeSet<String>(m.keySet());
        for (String key: s) {
            write(key);
            write(m.get(key));
        }
        os.write((int) 'e');
    }
    
    private void write(Object o) throws IOException {
        if (o instanceof Number) {
            write((Number) o);
        } else if (o instanceof String) {
            write((String) o);
        } else if (o instanceof List) {
            write((List) o);
        } else if (o instanceof Map) {
            write((Map) o);
        } else {
            throw new BencodingException("Wrong type");
        }
    }
}
