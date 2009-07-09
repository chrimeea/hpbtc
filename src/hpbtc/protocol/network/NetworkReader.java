/*
 * Created on Jan 20, 2006
 *
 */
package hpbtc.protocol.network;

import hpbtc.protocol.processor.MessageReader;
import hpbtc.protocol.torrent.InvalidPeerException;
import hpbtc.protocol.torrent.Peer;
import hpbtc.util.IOUtil;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;

/**
 * @author Cristian Mocanu
 *
 */
public class NetworkReader extends NetworkLoop {

    private ServerSocketChannel serverCh;
    private MessageReader reader;
    
    public NetworkReader(final MessageReader reader, final Register register) {
        super(register);
        this.stype = Register.SELECTOR_TYPE.TCP_READ;
        this.reader = reader;
    }

    public void connect(final int port) throws IOException {
        serverCh = ServerSocketChannel.open();
        serverCh.socket().bind(new InetSocketAddress(
                InetAddress.getLocalHost(), port));
        super.connect();
        serverCh.configureBlocking(false);
        register.registerNow(serverCh, stype, SelectionKey.OP_ACCEPT, null);
    }

    @Override
    public int connect() throws IOException {
        serverCh = ServerSocketChannel.open();
        ServerSocket s = serverCh.socket();
        s.bind(null);
        super.connect();
        serverCh.configureBlocking(false);
        register.registerNow(serverCh, stype, SelectionKey.OP_ACCEPT, null);
        return s.getLocalPort();
    }

    protected void processKey(final SelectionKey key) throws IOException,
            NoSuchAlgorithmException {
        Peer peer;
        if (key.isAcceptable()) {
            final SocketChannel chan = serverCh.accept();
            chan.configureBlocking(false);
            peer = new Peer(IOUtil.getAddress(chan));
            peer.setChannel(chan);
            reader.connect(peer);
            logger.info("Accepted connection from " + peer);
        } else if (key.isReadable()) {
            peer = (Peer) key.attachment();
            try {
                reader.readMessage(peer);
            } catch (InvalidPeerException ex) {
                throw new IOException();
            }
        }
    }

    @Override
    protected void disconnect(final SelectionKey key) throws IOException {
        final Peer peer = (Peer) key.attachment();
        try {
            reader.disconnect(peer);
        } catch (InvalidPeerException ex) {
            throw new IOException("Invalid peer " + peer);
        }
    }
}
