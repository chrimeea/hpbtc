/*
 * Created on Jan 19, 2006
 *
 */
package hpbtc.bencoding.element;

/**
 * @author chris
 *
 */
public class BencodedInteger extends BencodedElement {
    
    private Integer element;
    
    /**
     * 
     */
    public BencodedInteger() {
        element = null;
    }
    
    /**
     * @param n
     */
    public BencodedInteger(int n) {
        element = n;
    }
    
    /**
     * @return
     */
    public Integer getValue() {
        return element;
    }
    
    /**
     * @param v
     */
    public void setValue(Integer v) {
        element = v;
    }
    
    /* (non-Javadoc)
     * @see hpbtc.bencoding.element.BencodedElement#getEncodedSize()
     */
    public int getEncodedSize() {
        if (element == null) {
            return 2;
        }
        return 2 + element.toString().length();
    }
    
    /* (non-Javadoc)
     * @see hpbtc.bencoding.element.BencodedElement#encode(byte[], int)
     */
    protected int encode(byte[] b, int offset) {
        StringBuilder sb = new StringBuilder();
        sb.append('i');
        if (element == null) {
            sb.append('0');
        } else {
            sb.append(element);
        }
        sb.append('e');
        byte [] a = sb.toString().getBytes();
        System.arraycopy(a, 0, b, offset, a.length);
        return offset + a.length;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.element == null ? null : this.element.toString();
    }
}
