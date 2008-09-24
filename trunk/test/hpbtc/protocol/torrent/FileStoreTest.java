/*
 * Created on 23.09.2008
 */

package hpbtc.protocol.torrent;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import org.junit.Test;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class FileStoreTest {

    @Test
    public void testInit() {
    }
    
    @Test
    public void testComputeChunksInPiece() throws IOException,
            NoSuchAlgorithmException {
        FileStore fs = new FileStore(20481, new byte[20], ".", "test", 86016);
        assert fs.computeChunksInPiece(0) == 2;
        assert fs.computeChunksInPiece(4) == 1;
    }

    @Test
    public void testSaveLoad() {
    }
}
