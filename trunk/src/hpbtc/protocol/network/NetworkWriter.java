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
                        Peer peer = (Peer) key.attachment();
                        try {
                            SocketChannel ch = (SocketChannel) key.channel();
                            if (key.isConnectable() && ch.finishConnect()) {
                                writer.connect(peer);
                                logger.info("Connected to " + peer);
                            } else if (key.isWritable()) {
                                writer.writeNext(peer);
                            }
                        } catch (IOException ioe) {
                            logger.log(Level.FINE, ioe.getLocalizedMessage(),
                                    ioe);
                            writer.disconnect(peer);
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
