/*
 * Created on Jan 19, 2006
 *
 */
package hpbtc.bencoding.element;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author chris
 *
 */
public class BencodedList extends BencodedElement implements Iterable<BencodedElement> {
    
    private List<BencodedElement> element;

    /**
     * 
     */
    public BencodedList() {
        element = new LinkedList<BencodedElement>();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<BencodedElement> iterator() {
        return element.iterator();
    }
    
    /**
     * @return
     */
    public int getSize() {
        return element.size();
    }
    
    /**
     * @param e
     */
    public void addElement(BencodedElement e) {
        element.add(e);
    }
    
    /* (non-Javadoc)
     * @see hpbtc.bencoding.element.BencodedElement#getEncodedSize()
     */
    protected int getEncodedSize() {
        if (element == null) {
            return 2;
        }
        int i = 0;
        for (BencodedElement e : element) {
            i += e.getEncodedSize();
        }
        return i + 2;
    }
    
    /* (non-Javadoc)
     * @see hpbtc.bencoding.element.BencodedElement#encode(byte[], int)
     */
    protected int encode(byte[] y, int offset) {
        if (element == null) {
            y[offset] = (int) 'l';
            y[offset + 1] = (int) 'e';
            return 2;
        }
        y[offset++] = (int) 'l';
        for (BencodedElement e : element) {
            offset = e.encode(y, offset);
        }
        y[offset++] = (int) 'e';
        return offset;
    }

}
