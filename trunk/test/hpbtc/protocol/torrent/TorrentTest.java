package hpbtc.protocol.torrent;

import hpbtc.bencoding.BencodingWriter;
import hpbtc.protocol.network.Network;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import util.IOUtil;
import hpbtc.processor.NetworkStub;

/**
 *
 * @author Cristian Mocanu
 */
public class TorrentTest {

    private byte[] pid = new byte[20];
    private Network network = new NetworkStub();
    
    @Test
    public void testTorrentInfo() throws IOException, NoSuchAlgorithmException {
        ByteArrayInputStream b = new ByteArrayInputStream("d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi85e4:name11:manifest.mf12:piece lengthi65536e6:pieces20:12345678901234567890ee".getBytes("UTF-8"));
        Torrent info = new Torrent(b, ".", pid, network);
        b.close();
        assert info.getFileLength() == 85;
        assert info.getComment().equals("test comment");
        assert info.getCreatedBy().equals("uTorrent/1770");
        assert info.getCreationDate().getTime() == 1209116668000L;
        assert info.getEncoding().equals("UTF-8");
        assert info.getPieceLength() == 65536;
        assert info.getNrPieces() == 1;
        List<LinkedList<String>> trackers = info.getTrackers();
        assert trackers.size() == 1;
        List<String> l = trackers.get(0);
        assert l.size() == 1;
        assert l.get(0).equals("http://www.test.ro/announce");
        List<BTFile> files = info.getFiles();
        assert files.size() == 1;
        BTFile f = files.get(0);
        assert f.getLength() == 85;
        assert f.getPath().equals(".\\manifest.mf");
    }
    
    @Test
    public void testSavePiece() throws IOException, NoSuchAlgorithmException {
        File f = File.createTempFile("hpbtc-save-piece", "test");
        String t = "d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi85e4:name";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        BencodingWriter wr = new BencodingWriter(bos);
        wr.write(f.getName());
        String s = new String(bos.toByteArray(), "UTF-8");
        t += s + "12:piece lengthi65536e6:pieces20:12345678901234567890ee";        
        bos.close();
        ByteArrayInputStream b = new ByteArrayInputStream(t.getBytes("UTF-8"));
        Torrent info = new Torrent(b, f.getParent(), pid, network);
        b.close();
        ByteBuffer piece = ByteBuffer.allocate(info.getPieceLength());
        for (int i = 0; i < info.getPieceLength(); i++) {
            piece.put((byte) 5);
        }
        piece.rewind();
        info.savePiece(0, 0, piece);
        ByteBuffer dest = ByteBuffer.allocate(info.getPieceLength());
        IOUtil.readFromFile(f, 0, dest);
        f.delete();
        dest.rewind();
        piece.rewind();
        assert dest.equals(piece);
    }
    
    @Test
    public void testLoadPiece() throws IOException, NoSuchAlgorithmException {
        File f = File.createTempFile("hpbtc-load-piece", "test");
        String t = "d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi85e4:name";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        BencodingWriter wr = new BencodingWriter(bos);
        wr.write(f.getName());
        String s = new String(bos.toByteArray(), "UTF-8");
        t += s + "12:piece lengthi65536e6:pieces20:12345678901234567890ee";        
        bos.close();
        ByteArrayInputStream b = new ByteArrayInputStream(t.getBytes("UTF-8"));
        Torrent info = new Torrent(b, f.getParent(), pid, network);
        b.close();
        ByteBuffer piece = ByteBuffer.allocate(info.getPieceLength());
        for (int i = 0; i < info.getPieceLength(); i++) {
            piece.put((byte) 5);
        }
        piece.rewind();
        IOUtil.writeToFile(f, 0, piece);
        piece.rewind();
        ByteBuffer dest = info.loadPiece(0, 0, info.getPieceLength());
        f.delete();
        dest.rewind();
        assert piece.equals(dest);
    }
}
