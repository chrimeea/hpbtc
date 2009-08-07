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
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;

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
        serverCh.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public int connect() throws IOException {
        serverCh = ServerSocketChannel.open();
        ServerSocket s = serverCh.socket();
        s.bind(null);
        super.connect();
        serverCh.configureBlocking(false);
        serverCh.register(selector, SelectionKey.OP_ACCEPT);
        return s.getLocalPort();
    }

    protected void processKey(final SelectionKey key) throws IOException,
            NoSuchAlgorithmException {
        if (key.isAcceptable()) {
            final SocketChannel chan = serverCh.accept();
            chan.configureBlocking(false);
            final Peer peer = new Peer(IOUtil.getAddress(chan));
            peer.setChannel(chan);
            chan.register(selector, SelectionKey.OP_READ, peer);
            reader.connect(peer);
            logger.info("Accepted connection from " + peer);
        } else if (key.isReadable()) {
            final Peer peer = (Peer) key.attachment();
            if (!peer.isReading()) {
                peer.setReading(true);
                key.channel().register(selector,
                        key.interestOps() ^ SelectionKey.OP_READ, peer);
                new Thread(new Runnable() {

                    public void run() {
                        try {
                            while (reader.readMessage(peer));
                            peer.setReading(false);
                            register.registerNow(key.channel(), stype,
                                    key.interestOps() | SelectionKey.OP_READ, peer);
                        } catch (IOException ex) {
                            logger.log(Level.INFO, ex.getLocalizedMessage(), ex);
                            try {
                                disconnect(key);
                            } catch (IOException ex1) {
                                logger.log(Level.INFO, ex1.getLocalizedMessage(), ex1);
                            }
                        } catch (Exception ex2) {
                            logger.log(Level.INFO, ex2.getLocalizedMessage(), ex2);
                        }
                    }
                }).start();
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

    @Override
    protected void registerOperation(final SelectableChannel channel,
            int op, final Object peer) throws ClosedChannelException {
        final Peer p = (Peer) peer;
        if (p.isReading()) {
            op ^= SelectionKey.OP_READ;
        }
        super.registerOperation(channel, op, peer);
    }
}
