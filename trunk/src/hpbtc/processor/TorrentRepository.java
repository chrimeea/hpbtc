package hpbtc.processor;

import hpbtc.protocol.torrent.Torrent;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Cristian Mocanu
 */
public class TorrentRepository {

    private Map<byte[], Torrent> torrents;
    
    public TorrentRepository() {
        torrents = new HashMap<byte[], Torrent>();
    }
    
    public boolean haveTorrent(byte[] infoHash) {
        return torrents.containsKey(infoHash);
    }
    
    public void addTorrent(String fileName) throws IOException, NoSuchAlgorithmException {
        FileInputStream fis = new FileInputStream(fileName);
        Torrent ti = new Torrent(fis);
        torrents.put(ti.getInfoHash(), ti);
        fis.close();
    }
    
    public long getNrPieces(byte[] infoHash) {
        return torrents.get(infoHash).getNrPieces();
    }
    
    public long getPieceLength(byte[] infoHash) {
        return torrents.get(infoHash).getPieceLength();
    }
    
    public byte[] getPieceHash(byte[] infoHash, int index) {
        return torrents.get(infoHash).getPieceHash(index);
    }
}
