package hpbtc.protocol.torrent;

import hpbtc.bencoding.BencodingWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import util.IOUtil;
import java.util.Arrays;
import java.util.BitSet;

/**
 *
 * @author Cristian Mocanu
 */
public class TorrentTest {

    private String byteEncoding = "US-ASCII";

    @Test
    public void testTorrentInfo() throws IOException, NoSuchAlgorithmException {
        final ByteArrayInputStream b =
                new ByteArrayInputStream("d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi85e4:name11:manifest.mf12:piece lengthi65536e6:pieces20:12345678901234567890ee".
                getBytes(byteEncoding));
        final Torrent info = new Torrent(b, ".", null, 0);
        b.close();
        assert info.getFileLength() == 85;
        assert info.getComment().equals("test comment");
        assert info.getCreatedBy().equals("uTorrent/1770");
        assert info.getCreationDate().getTime() == 1209116668000L;
        assert info.getEncoding().equals("UTF-8");
        assert info.getPieceLength() == 65536;
        assert info.getNrPieces() == 1;
        final List<LinkedList<byte[]>> trackers = info.getTrackers();
        assert trackers.size() == 1;
        final List<byte[]> l = trackers.get(0);
        assert l.size() == 1;
        assert Arrays.equals(l.get(0), "http://www.test.ro/announce".getBytes(
                byteEncoding));
        final List<BTFile> files = info.getFiles();
        assert files.size() == 1;
        final BTFile f = files.get(0);
        assert f.getLength() == 85;
        assert f.getPath().equals(".\\manifest.mf");
    }

    @Test
    public void testSavePiece() throws IOException, NoSuchAlgorithmException {
        final File f = File.createTempFile("hpbtc-save-piece", "test");
        String t =
                "d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi85e4:name";
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final BencodingWriter wr = new BencodingWriter(bos);
        wr.write(f.getName().getBytes(byteEncoding));
        final String s = new String(bos.toByteArray(), byteEncoding);
        t += s + "12:piece lengthi65536e6:pieces20:12345678901234567890ee";
        bos.close();
        final ByteArrayInputStream b = new ByteArrayInputStream(t.getBytes(
                byteEncoding));
        final Torrent info = new Torrent(b, f.getParent(), null, 0);
        b.close();
        final ByteBuffer piece = ByteBuffer.allocate(info.getPieceLength());
        for (int i = 0; i < info.getPieceLength(); i++) {
            piece.put((byte) 5);
        }
        piece.rewind();
        info.savePiece(0, 0, piece);
        final ByteBuffer dest = ByteBuffer.allocate(info.getPieceLength());
        IOUtil.readFromFile(f, 0, dest);
        f.delete();
        dest.rewind();
        piece.rewind();
        assert dest.equals(piece);
    }

    @Test
    public void testLoadPiece() throws IOException, NoSuchAlgorithmException {
        final File f = File.createTempFile("hpbtc-load-piece", "test");
        String t =
                "d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi85e4:name";
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final BencodingWriter wr = new BencodingWriter(bos);
        wr.write(f.getName().getBytes(byteEncoding));
        final String s = new String(bos.toByteArray(), byteEncoding);
        t += s + "12:piece lengthi65536e6:pieces20:12345678901234567890ee";
        bos.close();
        final ByteArrayInputStream b = new ByteArrayInputStream(t.getBytes(
                byteEncoding));
        final Torrent info = new Torrent(b, f.getParent(), null, 0);
        b.close();
        final ByteBuffer piece = ByteBuffer.allocate(info.getPieceLength());
        for (int i = 0; i < info.getPieceLength(); i++) {
            piece.put((byte) 5);
        }
        piece.rewind();
        IOUtil.writeToFile(f, 0, piece);
        piece.rewind();
        final ByteBuffer dest = info.loadPiece(0, 0, info.getPieceLength());
        f.delete();
        dest.rewind();
        assert piece.equals(dest);
    }

    @Test
    public void testUpdateAvailability() throws IOException,
            NoSuchAlgorithmException {
        final ByteArrayInputStream b =
                new ByteArrayInputStream("d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi10240e4:name11:manifest.mf12:piece lengthi256e6:pieces20:12345678901234567890ee".
                getBytes(byteEncoding));
        final Torrent info = new Torrent(b, ".", null, 0);
        b.close();
        final BitSet bs = new BitSet();
        bs.set(10);
        assert info.getAvailability(10) == 0;
        info.updateAvailability(bs);
        assert info.getAvailability(10) == 1;
        info.updateAvailability(bs);
        assert info.getAvailability(10) == 2;
        final Peer p = new Peer(InetSocketAddress.createUnresolved("localhost",
                6000), null);
        p.setPiece(10);
        info.addPeer(p, false);
        info.updateAvailability(10);
        assert info.getAvailability(10) == 3;
        info.removePeer(p);
        assert info.getAvailability(10) == 2;
    }

    @Test
    public void testGetOtherPieces() throws IOException,
            NoSuchAlgorithmException {
        final ByteArrayInputStream b =
                new ByteArrayInputStream("d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi10240e4:name11:manifest.mf12:piece lengthi256e6:pieces20:12345678901234567890ee".
                getBytes(byteEncoding));
        final Torrent info = new Torrent(b, ".", null, 0);
        b.close();
        final Peer p = new Peer(InetSocketAddress.createUnresolved("localhost",
                6000), null);
        BitSet bs = info.getOtherPieces(p);
        assert bs.isEmpty();
        p.setPiece(6);
        p.setPiece(2);
        bs = info.getOtherPieces(p);
        assert bs.cardinality() == 2;
        assert bs.get(6);
        assert bs.get(2);
    }

    @Test
    public void testGetChunksSavedAndRequested() throws IOException,
            NoSuchAlgorithmException {
        final ByteArrayInputStream b =
                new ByteArrayInputStream("d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi10240e4:name11:manifest.mf12:piece lengthi256e6:pieces20:12345678901234567890ee".
                getBytes(byteEncoding));
        final Torrent info = new Torrent(b, ".", null, 0);
        b.close();
        final Peer p = new Peer(InetSocketAddress.createUnresolved("localhost",
                6000),
                null);
        p.setTorrent(info);
        p.addRequest(10, info.getChunkSize() + 1);
        final BitSet bs = info.getChunksSavedAndRequested(p, 10);
        assert bs.cardinality() == 1;
        assert bs.get(1);
    }
}
