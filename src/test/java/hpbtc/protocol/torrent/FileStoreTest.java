/*
 * Created on 23.09.2008
 */
package hpbtc.protocol.torrent;

import hpbtc.util.ByteStringComparator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.Test;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class FileStoreTest {

    private String byteEncoding = "US-ASCII";
    
    @Test
    public void testComputeChunksInPiece() throws IOException,
            NoSuchAlgorithmException {
        final FileStore fs = new FileStore(20481, new byte[20], ".", "test",
                86016);
        assert fs.computeChunksInPiece(0) == 2;
        assert fs.computeChunksInPiece(4) == 1;
    }

    @Test
    public void testSingleFileSaveLoad() throws IOException,
            NoSuchAlgorithmException {
        byte[] b1 = new byte[16384];
        Arrays.fill(b1, (byte) 97);
        byte[] b2 = new byte[4097];
        Arrays.fill(b2, (byte) 98);
        final MessageDigest md = MessageDigest.getInstance("SHA1");
        md.update(b1);
        md.update(b2);
        final File f = File.createTempFile("hpbtc", null);
        byte[] dig = md.digest();
        FileStore fs = new FileStore(20481, dig,
                f.getParentFile().getPath(), f.getName(), 20481);
        assert !fs.isPieceComplete(0);
        assert !fs.savePiece(0, 0, ByteBuffer.wrap(b1));
        ByteBuffer bb = ByteBuffer.allocate(16384);
        fs.loadPiece(0, 0, bb);
        assert Arrays.equals(bb.array(), b1);
        assert fs.savePiece(16384, 0, ByteBuffer.wrap(b2));
        bb = ByteBuffer.allocate(4097);
        fs.loadPiece(16384, 0, bb);
        assert Arrays.equals(bb.array(), b2);
        assert fs.isPieceComplete(0);
        fs = new FileStore(20481, dig,
                f.getParentFile().getPath(), f.getName(), 20481);
        assert fs.isPieceComplete(0);
        f.delete();
    }
    
    @Test
    public void testMultipleFileSaveLoad() throws IOException,
            NoSuchAlgorithmException {
        final File f1 = File.createTempFile("hpbtc", null);
        final File f2 = File.createTempFile("hpbtc", null);
        byte[] b = new byte[50];
        byte[] b1 = new byte[20];
        Arrays.fill(b1, (byte) 65);
        for (int i = 0; i < 20; i++) {
            b[i] = b1[i];
        }
        byte[] b2 = new byte[30];
        Arrays.fill(b2, (byte) 66);
        for (int i = 0; i < 30; i++) {
            b[i + 20] = b2[i];
        }
        final MessageDigest md = MessageDigest.getInstance("SHA1");
        md.update(b);
        byte[] dig = md.digest();
        List<Map<byte[], Object>> fls = new ArrayList<Map<byte[], Object>>(2);
        ByteStringComparator comp = new ByteStringComparator();
        Map<byte[], Object> map = new TreeMap<byte[], Object>(comp);
        map.put("path".getBytes(byteEncoding),
                Collections.singletonList(f1.getName().getBytes(byteEncoding)));
        map.put("length".getBytes(byteEncoding), new Long(20));
        fls.add(map);
        map = new TreeMap<byte[], Object>(comp);
        map.put("path".getBytes(byteEncoding),
                Collections.singletonList(f2.getName().getBytes(byteEncoding)));
        map.put("length".getBytes(byteEncoding), new Long(30));
        fls.add(map);
        FileStore fs = new FileStore(50, dig, f1.getParentFile().getPath(),
                fls, byteEncoding);
        assert !fs.isPieceComplete(0);
        assert fs.savePiece(0, 0, ByteBuffer.wrap(b));
        byte[] r1 = new byte[20];
        FileInputStream fis = new FileInputStream(f1);
        fis.read(r1);
        assert Arrays.equals(r1, b1);
        r1 = new byte[30];
        fis.close();
        fis = new FileInputStream(f2);
        fis.read(r1);
        assert Arrays.equals(r1, b2);
        fis.close();
        ByteBuffer bb = ByteBuffer.allocate(50);
        fs.loadPiece(0, 0, bb);
        assert Arrays.equals(bb.array(), b);
        assert fs.isPieceComplete(0);
        f1.delete();
        f2.delete();
    }
}
