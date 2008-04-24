package hpbtc.bencoding;

import java.io.InputStream;
import org.junit.Test;
import org.junit.Before;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Cristian Mocanu
 */
public class BencodingParserTest {

    private InputStream mockIs;
    private BencodingParser parser;
    
    @Before
    public void setUp() {
        mockIs = createMock(InputStream.class);
        parser = new BencodingParser(mockIs);
    }
    
    @Test
    public void testReadNextString() {
    }
    
    @Test
    public void testReadNextInteger() {
    }
    
    @Test
    public void testReadNextList() {
    }
    
    @Test
    public void readNextDictionary() {
    }
    
    @Test
    public void readNextElement() {
    }
}
