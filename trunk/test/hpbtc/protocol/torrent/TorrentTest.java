package hpbtc.protocol.torrent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;

/**
 *
 * @author Cristian Mocanu
 */
public class TorrentTest {

    @Test
    public void testTorrentInfo() throws IOException, NoSuchAlgorithmException {
        ByteArrayInputStream b = new ByteArrayInputStream("d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi85e4:name11:manifest.mf12:piece lengthi65536e6:pieces20:12345678901234567890ee".getBytes("UTF-8"));
        Torrent info = new Torrent(new FileStore(), b, new File("."));
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
        assert f.getPath().equals("manifest.mf");
    }
}
