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
    public void testComputeChunksInPiece() throws IOException,
            NoSuchAlgorithmException {
        final FileStore fs = new FileStore(20481, new byte[20], ".", "test",
                86016);
        assert fs.computeChunksInPiece(0) == 2;
        assert fs.computeChunksInPiece(4) == 1;
    }

    @Test
    public void testSaveLoad() throws IOException, NoSuchAlgorithmException {
        byte[] b1 = new byte[16384];
        Arrays.fill(b1, (byte) 5);
        byte[] b2 = new byte[4097];
        Arrays.fill(b2, (byte) 30);
        final MessageDigest md = MessageDigest.getInstance("SHA1");
        md.update(b1);
        md.update(b2);
        final File f = File.createTempFile("HPBTC", null);
        byte[] dig = md.digest();
        FileStore fs = new FileStore(20481, dig,
                f.getParentFile().getPath(), f.getName(), 20481);
        assert !fs.savePiece(0, 0, ByteBuffer.wrap(b1));
        ByteBuffer bb = ByteBuffer.allocate(16384);
        fs.loadPiece(0, 0, bb);
        assert Arrays.equals(bb.array(), b1);
        assert fs.savePiece(16384, 0, ByteBuffer.wrap(b2));
        bb = ByteBuffer.allocate(4097);
        fs.loadPiece(16384, 0, bb);
        assert Arrays.equals(bb.array(), b2);
        assert fs.isPieceComplete(0);
        assert fs.isTorrentComplete();
        fs = new FileStore(20481, dig,
                f.getParentFile().getPath(), f.getName(), 20481);
        assert fs.isPieceComplete(0);
        assert fs.isTorrentComplete();
        f.delete();
    }
}
