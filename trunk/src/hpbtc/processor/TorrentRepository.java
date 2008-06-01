package hpbtc.processor;

import hpbtc.protocol.torrent.TorrentInfo;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Cristian Mocanu
 */
public class TorrentRepository {

    private Set<TorrentInfo> torrents;
    
    public TorrentRepository() {
        torrents = new HashSet<TorrentInfo>();
    }
    
    private TorrentInfo getTorrent(byte[] infoHash) {
        for (TorrentInfo t : torrents) {
            if (Arrays.equals(t.getInfoHash(), infoHash)) {
                return t;
            }
        }
        return null;
    }

    public boolean haveTorrent(byte[] infoHash) {
        return getTorrent(infoHash) != null;
    }
    
    public void addTorrent(String fileName) throws IOException, NoSuchAlgorithmException {
        FileInputStream fis = new FileInputStream(fileName);
        torrents.add(new TorrentInfo(fis));
        fis.close();
    }
    
    public long getNrPieces(byte[] infoHash) {
        return getTorrent(infoHash).getNrPieces();
    }
    
    public long getPieceLength(byte[] infoHash) {
        return getTorrent(infoHash).getPieceLength();
    }
    
    public byte[] getPieceHash(byte[] infoHash, int index) {
        return getTorrent(infoHash).getPieceHash(index);
    }
}
