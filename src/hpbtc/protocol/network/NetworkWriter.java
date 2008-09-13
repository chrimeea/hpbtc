package hpbtc.protocol.network;

import hpbtc.processor.MessageWriter;
import hpbtc.protocol.torrent.Peer;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu
 */
public class NetworkWriter {

    private static Logger logger = Logger.getLogger(
            NetworkWriter.class.getName());
    private boolean running;
    private Selector selector;
    private MessageWriter writer;
    private Register register;

    public NetworkWriter(final MessageWriter writer, final Register register) {
        this.writer = writer;
        this.register = register;
    }

    public int connect() throws IOException {
        selector = register.openWriteSelector();
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
            }
        }).start();
        return 0;
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
                            peer = (Peer) key.attachment();
                            SocketChannel ch = (SocketChannel) key.channel();
                            if (key.isConnectable() && ch.finishConnect()) {
                                logger.fine("Connected to " + peer);
                                ch.register(selector, key.interestOps() &
                                        ~SelectionKey.OP_CONNECT, peer);
                                register.registerRead(peer);
                            } else {
                                if (key.isValid() && key.isWritable()) {
                                    ch.register(selector, key.interestOps() &
                                            ~SelectionKey.OP_WRITE, peer);
                                    writer.writeNext(peer);
                                    if (ch.isOpen() && !writer.isEmpty(peer)) {
                                        ch.register(selector, key.interestOps() |
                                                SelectionKey.OP_WRITE, peer);
                                    } else {
                                        key.cancel();
                                    }
                                }
                            }
                        } catch (IOException ioe) {
                            logger.log(Level.WARNING, ioe.getLocalizedMessage(),
                                    ioe);
                        }
                    }
                }
            }
            register.performWriteRegistration();
        }
    }

    public void disconnect() {
        running = false;
        selector.wakeup();
    }
}
