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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Cristian Mocanu
 *
 */
public class NetworkReader {

    private static final int MIN_PORT = 6881;
    private static final int MAX_PORT = 6999;
    private static Logger logger = Logger.getLogger(
            NetworkReader.class.getName());
    private ServerSocketChannel serverCh;
    private Selector selector;
    private boolean running;
    private MessageReader processor;

    public NetworkReader(final MessageReader processor) {
        this.processor = processor;
    }

    private int findPort() throws IOException {
        int port = MIN_PORT;
        while (port <= MAX_PORT) {
            try {
                serverCh.socket().bind(new InetSocketAddress(
                        InetAddress.getLocalHost(), port));
                break;
            } catch (IOException e) {
                port++;
            }
        }
        if (port > MAX_PORT) {
            throw new IOException("No ports available");
        } else {
            return port;
        }
    }

    private void startListen() throws IOException {
        selector = processor.openReadSelector();
        serverCh.configureBlocking(false);
        serverCh.register(selector, SelectionKey.OP_ACCEPT);
        running = true;
        new Thread(new Runnable() {

            public void run() {
                try {
                    listen();
                } catch (Exception e) {
                    running = false;
                    logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
                }
                try {
                    selector.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
                }
                try {
                    serverCh.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
                }
            }
        }).start();        
    }
    
    public void connect(final int port) throws IOException {
        serverCh = ServerSocketChannel.open();
        serverCh.socket().bind(new InetSocketAddress(
                        InetAddress.getLocalHost(), port));
        startListen();
    }
    
    public int connect() throws IOException {
        serverCh = ServerSocketChannel.open();
        int port = findPort();
        startListen();
        return port;
    }

    public void disconnect() {
        running = false;
        selector.wakeup();
    }

    private void listen() throws IOException, NoSuchAlgorithmException {
        while (running) {
            if (selector.select() > 0) {
                final Iterator<SelectionKey> i = selector.selectedKeys().
                        iterator();
                while (i.hasNext()) {
                    final SelectionKey key = i.next();
                    i.remove();
                    Peer peer = null;
                    if (key.isValid()) {
                        try {
                            if (key.isAcceptable()) {
                                final SocketChannel chan = serverCh.accept();
                                chan.configureBlocking(false);
                                peer = new Peer(chan);
                                processor.connect(peer);
                                logger.info("Accepted connection from " + peer);
                            } else if (key.isReadable()) {
                                peer = (Peer) key.attachment();
                                processor.readMessage(peer);
                            }
                        } catch (IOException ioe) {
                            logger.log(Level.FINE, peer == null ? ioe.
                                    getLocalizedMessage() : peer.toString(), ioe);
                            if (peer != null) {
                                processor.disconnect(peer);
                            } else {
                                key.cancel();
                            }
                        }
                    }
                }
            }
            processor.performReadRegistration();
        }
    }
}
