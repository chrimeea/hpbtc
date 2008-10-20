package hpbtc.processor;

import hpbtc.protocol.network.NetworkReader;
import hpbtc.protocol.network.NetworkWriter;
import hpbtc.protocol.network.Register;
import hpbtc.protocol.torrent.Torrent;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.Vector;
import util.TorrentUtil;

/**
 *
 * @author Cristian Mocanu
 */
public class Client {

    private Timer fastTimer;
    private MessageWriter writer;
    private MessageReader processor;
    private int port;
    private NetworkReader netReader;
    private NetworkWriter netWriter;
    private List<Torrent> torrents;
    private byte[] peerId;
    private byte[] protocol;

    public Client(byte[] peerId) throws UnsupportedEncodingException {
        fastTimer = new Timer(true);
        torrents = new Vector<Torrent>();
        this.peerId = peerId;
        protocol = TorrentUtil.getSupportedProtocol();        
    }
    
    public Client() throws UnsupportedEncodingException {
        this(TorrentUtil.generateId());
    }

    public Torrent download(final InputStream is, final String rootFolder)
            throws IOException, NoSuchAlgorithmException {
        final Torrent ti = new Torrent(is, rootFolder, peerId, port);
        for (Torrent t: torrents) {
            if (Arrays.equals(t.getInfoHash(), ti.getInfoHash())) {
                return t;
            }
        }
        torrents.add(ti);
        writer.download(ti);
        return ti;
    }

    public void stopProtocol() {
        netReader.disconnect();
        netWriter.disconnect();
    }
    
    public void stopTorrent(final Torrent torrent) throws IOException {
        writer.stopTorrent(torrent);
        torrents.remove(torrent);
    }
    
    private void initNetwork() {
        Register register = new Register();
        writer = new MessageWriter(register, fastTimer, peerId, protocol);
        netWriter = new NetworkWriter(writer, register);
        processor = new MessageReader(register, protocol, writer, torrents,
                peerId);
        netReader = new NetworkReader(processor, register);
    }
    
    public void startProtocol(int port) throws IOException {
        initNetwork();
        netReader.connect(port);
        netWriter.connect();
    }
    
    public int startProtocol() throws IOException {
        initNetwork();
        port = netReader.connect();
        netWriter.connect();
        return port;
    }

}
