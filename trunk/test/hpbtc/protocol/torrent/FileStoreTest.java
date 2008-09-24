/*
 * Created on 23.09.2008
 */

package hpbtc.protocol.torrent;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
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
    public void testSaveLoad() throws IOException, NoSuchAlgorithmException {
        byte[] b1 = new byte[16384];
        Arrays.fill(b1, (byte) 5);
        byte[] b2 = new byte[4097];
        Arrays.fill(b2, (byte) 30);
        MessageDigest md = MessageDigest.getInstance("SHA1");
        md.update(b1);
        md.update(b2);
        File f = File.createTempFile("HPBTC", null);
        FileStore fs = new FileStore(20481, md.digest(),
                f.getParentFile().getPath(), f.getName(), 20481);
        assert !fs.savePiece(0, 0, ByteBuffer.wrap(b1));
        assert Arrays.equals(fs.loadPiece(0, 0, 16384).array(), b1);
        assert fs.savePiece(16384, 0, ByteBuffer.wrap(b2));
        assert Arrays.equals(fs.loadPiece(16384, 0, 4097).array(), b2);
        assert fs.isPieceComplete(0);
        f.delete();
    }
}
