package hpbtc.processor;

import hpbtc.protocol.network.NetworkReader;
import hpbtc.protocol.network.NetworkWriter;
import hpbtc.protocol.network.Register;
import hpbtc.protocol.torrent.Torrent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
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

    public Client() throws UnsupportedEncodingException {
        fastTimer = new Timer(true);
        torrents = new Vector<Torrent>();
        this.peerId = TorrentUtil.generateId();
        protocol = TorrentUtil.getSupportedProtocol();
    }

    public void download(final File fileName, final String rootFolder)
            throws IOException, NoSuchAlgorithmException {
        FileInputStream fis = new FileInputStream(fileName);
        final Torrent ti = new Torrent(fis, rootFolder, peerId, port);
        fis.close();
        torrents.add(ti);
        writer.download(ti);
    }

    public void stopProtocol() {
        netReader.disconnect();
        netWriter.disconnect();
    }
    
    public int startProtocol() throws IOException {
        Register register = new Register();
        writer = new MessageWriterImpl(register, fastTimer, peerId, protocol);
        netWriter = new NetworkWriter(writer, register);
        processor = new MessageReaderImpl(register, protocol, writer, torrents,
                peerId);
        netReader = new NetworkReader(processor, register);
        port = netReader.connect();
        netWriter.connect();
        return port;
    }

}
