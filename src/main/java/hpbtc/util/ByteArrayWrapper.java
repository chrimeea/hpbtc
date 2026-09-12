/*
 * Created on 11.07.2009
 */
package hpbtc.util;

import java.util.Arrays;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class ByteArrayWrapper {

    private byte[] b;

    public ByteArrayWrapper(byte[] b) {
        this.b = b;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof byte[]) {
            return Arrays.equals(b, (byte[]) o);
        } else if (o instanceof ByteArrayWrapper) {
            return Arrays.equals(b, ((ByteArrayWrapper) o).b);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return new String(b).hashCode();
    }
}
