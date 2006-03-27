/*
 * Created on Jan 19, 2006
 *
 */
package hpbtc.bencoding.element;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * @author chris
 *
 */
public class BencodedString extends BencodedElement implements Comparable<BencodedString>
{    
    private static Logger logger = Logger.getLogger(BencodedString.class.getName());
    
    private byte[] element;
    
    /**
     * 
     */
    public BencodedString() {
        element = null;
    }
    
    /**
     * @param s
     */
    public BencodedString(String s) {
        try {
            element = s.getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            logger.severe("ISO-8859-1 is not available");
        }
        
    }
    
    public int compareTo(BencodedString s) {
        if (element == null && s.element != null) {
            return -1;
        }
        return getValue().compareTo(s.getValue());
    }
    
    /**
     * @return
     */
    public byte[] getBytes() {
        return element;
    }
    
    /**
     * @return
     */
    public String getValue() {
        String s = null;
        try {
            s = new String(element, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            logger.severe("ISO-8859-1 is not available");
        }
        return s;
    }
    
    /**
     * @param v
     */
    public void setValue(byte[] v) {
        element = v;
    }

    /* (non-Javadoc)
     * @see hpbtc.bencoding.element.BencodedElement#getEncodedSize()
     */
    public int getEncodedSize() {
        if (element == null) {
            return 2;
        }
        return String.valueOf(element.length).length() + element.length + 1;
    }
    
    /* (non-Javadoc)
     * @see hpbtc.bencoding.element.BencodedElement#encode(byte[], int)
     */
    protected int encode(byte[] a, int offset) {
        if (element == null) {
            a[offset] = (int) '0';
            a[offset + 1] = (int) ':';
            return 2;
        }
        String s = element.length + ":";
        byte[] b = s.getBytes();
        System.arraycopy(b, 0, a, offset, b.length);
        offset += b.length;
        System.arraycopy(element, 0, a, offset, element.length);
        return offset + element.length;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object s) {
        byte[] x = null;
        if (s instanceof BencodedString) {
            x = ((BencodedString) s).element;
        } else if (s instanceof byte[]) {
            x = (byte[]) s;
        } else {
            return false;
        }
        return Arrays.equals(x, element);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return Arrays.hashCode(element);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return this.element == null ? null : new String(this.element);
    }
}
