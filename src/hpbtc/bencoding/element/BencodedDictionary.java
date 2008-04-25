/*
 * Created on Jan 19, 2006
 *
 */
package hpbtc.bencoding.element;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author chris
 *
 */
public class BencodedDictionary extends BencodedElement {
    
    private Map<BencodedString, BencodedElement> element;

    /**
     * 
     */
    public BencodedDictionary() {
        element = new TreeMap<BencodedString, BencodedElement>();
    }
    
    /**
     * @return
     */
    public Set<BencodedString> getKeys() {
        return element.keySet();
    }
    
    /**
     * @param key
     * @param e
     */
    public void addKeyValuePair(BencodedString key, BencodedElement e) {
        element.put(key, e);
    }
    
    /**
     * @param key
     * @return
     */
    public BencodedElement get(BencodedString key) {
        return element.get(key);
    }

    /**
     * @param key
     * @return
     */
    public BencodedElement get(String key) {
        return element.get(new BencodedString(key));
    }
    
    /**
     * @param key
     * @return
     */
    public boolean containsKey(String key) {
        return element.containsKey(new BencodedString(key));
    }
    
    /* (non-Javadoc)
     * @see hpbtc.bencoding.element.BencodedElement#getEncodedSize()
     */
    protected int getEncodedSize() {
        if (element == null) {
            return 2;
        }
        int i = 0;
        for (BencodedString k : element.keySet()) {
            i += k.getEncodedSize();
            i += element.get(k).getEncodedSize();
        }
        return i + 2;
    }
    
    /* (non-Javadoc)
     * @see hpbtc.bencoding.element.BencodedElement#encode(byte[], int)
     */
    protected int encode(byte[] z, int offset) {
        z[offset++] = (int) 'd';
        for (BencodedString k : element.keySet()) {
            offset = k.encode(z, offset);
            offset = element.get(k).encode(z, offset); 
        }
        z[offset++] = (int) 'e';
        return offset;
    }
}
