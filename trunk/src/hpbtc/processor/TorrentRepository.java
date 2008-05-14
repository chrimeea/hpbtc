package hpbtc.processor;

import hpbtc.protocol.torrent.TorrentInfo;
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
    
    public TorrentInfo getTorrent(byte[] infoHash) {
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
    
    public void addTorrent(TorrentInfo torrent) {
        torrents.add(torrent);
    }
}
