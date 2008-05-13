package hpbtc.processor;

import hpbtc.protocol.torrent.TorrentInfo;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Cristian Mocanu
 */
public class Client {

    private Protocol protocol;
    private List<TorrentInfo> torrents;
    
    public Client() {
        protocol = new Protocol();
        torrents = new LinkedList<TorrentInfo>();
    }
    
    public void start() throws IOException {
        protocol.startProtocol();
    }
    
    public void download(String fileName) throws IOException, NoSuchAlgorithmException {
        FileInputStream fis = new FileInputStream(fileName);
        torrents.add(new TorrentInfo(fis));
        fis.close();
    }
}
