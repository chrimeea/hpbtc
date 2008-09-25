/*
 * Created on 25.09.2008
 */

package hpbtc.bencoding;

import java.io.UnsupportedEncodingException;
import org.junit.Test;


/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class ByteStringComparatorTest {

    private String encoding = "US-ASCII";
    
    @Test
    public void testCompare() throws UnsupportedEncodingException {
        ByteStringComparator c = new ByteStringComparator();
        assert c.compare("alfa".getBytes(encoding), "gamma".getBytes(encoding))
                < 0;
        assert c.compare("1abc".getBytes(encoding), "1abc".getBytes(encoding))
                == 0;
        assert c.compare("45ab".getBytes(encoding), "45Ab".getBytes(encoding))
                > 0;
    }
}
