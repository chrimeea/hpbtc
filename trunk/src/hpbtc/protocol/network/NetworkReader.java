/*
 * Created on Jan 20, 2006
 *
 */
package hpbtc.protocol.network;

import hpbtc.processor.MessageReader;
import hpbtc.protocol.torrent.Peer;
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
        this.reader = reader;
    }

    public void connect(final int port) throws IOException {
        serverCh = ServerSocketChannel.open();
        serverCh.socket().bind(new InetSocketAddress(
                InetAddress.getLocalHost(), port));
        super.connect();
        reader.setReadSelector(selector);
        serverCh.configureBlocking(false);
        serverCh.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public int connect() throws IOException {
        serverCh = ServerSocketChannel.open();
        ServerSocket s = serverCh.socket();
        s.bind(null);
        super.connect();
        reader.setReadSelector(selector);
        serverCh.configureBlocking(false);
        serverCh.register(selector, SelectionKey.OP_ACCEPT);
        return s.getLocalPort();
    }

    protected void processKey(final SelectionKey key) throws IOException,
            NoSuchAlgorithmException {
        Peer peer;
        if (key.isAcceptable()) {
            final SocketChannel chan = serverCh.accept();
            chan.configureBlocking(false);
            peer = new Peer(chan);
            reader.connect(peer);
            logger.info("Accepted connection from " + peer);
        } else if (key.isReadable()) {
            peer = (Peer) key.attachment();
            reader.readMessage(peer);
        }
    }

    @Override
    protected void disconnect(final SelectionKey key) throws IOException {
        reader.disconnect((Peer) key.attachment());
    }
}
