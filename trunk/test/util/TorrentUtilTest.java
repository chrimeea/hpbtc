package util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import org.junit.Test;

/**
 *
 * @author Cristian Mocanu
 */
public class TorrentUtilTest {

    @Test
    public void testComputeBeginIndex() {
        int index = TorrentUtil.computeBeginIndex(753677, 16384);
        assert index == 46;
    }
    
    @Test
    public void testComputeEndIndex() {
        int index = TorrentUtil.computeEndIndex(753677, 56320, 16384);
        assert index == 50;
    }
    
    @Test
    public void testComputeBeginPosition() {
        int position = TorrentUtil.computeBeginPosition(46, 16384);
        assert position == 753664;
    }
    
    @Test
    public void testGetSupportedProtocol() throws UnsupportedEncodingException {
        byte[] protocol = new byte[20];
        ByteBuffer pr = ByteBuffer.wrap(protocol);
        pr.put((byte) 19);
        pr.put("BitTorrent protocol".getBytes("US-ASCII"));
        assert Arrays.equals(protocol, TorrentUtil.getSupportedProtocol());
    }
    
    @Test
    public void testGenerateId() throws UnsupportedEncodingException {
        byte[] unu = TorrentUtil.generateId();
        assert unu.length == 20;
        byte[] doi = TorrentUtil.generateId();
        assert !Arrays.equals(unu, doi);
    }
    
    @Test
    public void testComputeInfoHash() throws NoSuchAlgorithmException {
        byte[] b = new byte[20];
        assert Arrays.equals(TorrentUtil.computeInfoHash(b),
                MessageDigest.getInstance("SHA1").digest(b));
    }
    
    @Test
    public void testComputeNextPieceIndexFromPosition() {
        assert TorrentUtil.computeNextPieceIndexFromPosition(101, 10) == 11;
    }
    
    @Test
    public void testComputeNrPieces() {
        assert TorrentUtil.computeNrPieces(53, 19) == 3;
    }
    
    @Test
    public void testComputeChunkSize() {
        assert TorrentUtil.computeChunkSize(7, 3, 8, 10240L, 24) == 8;
        assert TorrentUtil.computeChunkSize(427, 3, 8, 10240L, 24) == 8;
        assert TorrentUtil.computeChunkSize(427, 9, 8, 10240L, 24) == 7;
    }
    
    @Test
    public void testComputeRemainingLastPiece() {
        assert TorrentUtil.computeRemainingLastPiece(10, 1024L, 256) == 246;
        assert TorrentUtil.computeRemainingLastPiece(3, 1024L, 255) == 1;
    }
    
    @Test
    public void testComputeChunksInNotLastPiece() {
        assert TorrentUtil.computeChunksInNotLastPiece(1024, 256) == 4;
        assert TorrentUtil.computeChunksInNotLastPiece(10, 3) == 4;
    }
    
    @Test
    public void testComputeChunksInLastPiece() {
        assert TorrentUtil.computeChunksInLastPiece(10, 3, 1) == 1;
    }
}
