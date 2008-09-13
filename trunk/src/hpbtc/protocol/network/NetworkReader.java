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
    private Register register;

    public NetworkReader(final MessageReader processor,
            final Register register) {
        this.processor = processor;
        this.register = register;
    }

    public int connect() throws IOException {
        int port = MIN_PORT;
        serverCh = ServerSocketChannel.open();
        while (port <= MAX_PORT) {
            try {
                InetSocketAddress isa = new InetSocketAddress(
                        InetAddress.getLocalHost(), port);
                serverCh.socket().bind(isa);
                break;
            } catch (IOException e) {
                port++;
            }
        }
        if (port > MAX_PORT) {
            throw new IOException("No ports available");
        } else {
            selector = register.openReadSelector();
            serverCh.configureBlocking(false);
            serverCh.register(selector, SelectionKey.OP_ACCEPT);
        }
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
        return port;
    }

    public void disconnect() {
        running = false;
        selector.wakeup();
    }

    private void listen() throws IOException,
            NoSuchAlgorithmException {
        while (running) {
            if (selector.select() > 0) {
                Iterator<SelectionKey> i = selector.selectedKeys().iterator();
                while (i.hasNext()) {
                    SelectionKey key = i.next();
                    i.remove();
                    if (key.isValid()) {
                        Peer peer;
                        try {
                            if (key.isAcceptable()) {
                                SocketChannel chan = serverCh.accept();
                                chan.configureBlocking(false);
                                peer = new Peer(chan);
                                chan.register(selector, SelectionKey.OP_READ,
                                        peer);
                                logger.fine("Accepted connection from " + peer);
                            } else if (key.isReadable()) {
                                peer = (Peer) key.attachment();
                                SocketChannel ch = (SocketChannel) key.channel();
                                ch.register(selector, key.interestOps() &
                                        ~SelectionKey.OP_READ, peer);
                                processor.readMessage(peer);
                                if (ch.isOpen()) {
                                    ch.register(selector,
                                            key.interestOps() |
                                            SelectionKey.OP_READ, peer);
                                }
                            }
                        } catch (IOException ioe) {
                            logger.log(Level.WARNING, ioe.getLocalizedMessage(),
                                    ioe);
                        }
                    }
                }
            }
            register.performReadRegistration();
        }
    }
}
