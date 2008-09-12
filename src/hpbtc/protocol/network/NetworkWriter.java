package hpbtc.protocol.network;

import hpbtc.processor.MessageWriter;
import hpbtc.protocol.torrent.Peer;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Chris
 */
public class NetworkWriter implements Network {

    private static Logger logger = Logger.getLogger(NetworkWriter.class.getName());
    private boolean running;
    private Selector selector;
    private Queue<RegisterOp> registered;
    private MessageWriter writer;
    
    public NetworkWriter(MessageWriter writer) {
        this.writer = writer;
        registered = new ConcurrentLinkedQueue<RegisterOp>();
    }
    
    public int connect() throws IOException {
        selector = Selector.open();
        running = true;
        new Thread(new Runnable() {

            public void run() {
                try {
                    listen();
                } catch (Exception e) {
                    running = false;
                    logger.log(Level.WARNING, e.getLocalizedMessage(), e);
                }
                try {
                    selector.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, e.getLocalizedMessage(), e);
                }
            }
        }).start();
        return 0;
    }

    public void registerNow(Peer peer, int op) throws IOException {
        SocketChannel ch = (SocketChannel) peer.getChannel();
        if (ch != null) {
            if (ch.isOpen()) {
                SelectionKey sk = ch.keyFor(selector);
                if (sk == null || (sk != null && (sk.interestOps() & op) == 0)) {
                    registered.add(new RegisterOp(op, peer));
                    selector.wakeup();
                }
            }
        } else {
            ch = SocketChannel.open();
            ch.configureBlocking(false);
            peer.setChannel(ch);
            if (ch.connect(peer.getAddress())) {
                registered.add(new RegisterOp(op, peer));
            } else {
                registered.add(
                        new RegisterOp(SelectionKey.OP_CONNECT | op, peer));
            }
            selector.wakeup();
        }
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
                                ch.register(selector, (key.interestOps() |
                                        SelectionKey.OP_READ) &
                                        ~SelectionKey.OP_CONNECT, peer);
                                logger.info("Connected to " + peer);
                            } else {
                                if (key.isValid() && key.isWritable()) {
                                    ch.register(selector, key.interestOps() &
                                            ~SelectionKey.OP_WRITE, peer);
                                    writer.writeNext(peer);
                                    if (ch.isOpen() && !writer.isEmpty(peer)) {
                                        ch.register(selector, key.interestOps() |
                                                SelectionKey.OP_WRITE, peer);
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
            RegisterOp ro = registered.poll();
            while (ro != null) {
                SelectableChannel q = (SelectableChannel) ro.peer.getChannel();
                if (q.isOpen()) {
                    SelectionKey w = q.keyFor(selector);
                    try {
                        if (w != null && w.isValid()) {
                            q.register(selector, w.interestOps() | ro.operation,
                                    ro.peer);
                        } else if (w == null) {
                            q.register(selector, ro.operation, ro.peer);
                        }
                        logger.info("Registered " + ro.peer);
                    } catch (ClosedChannelException e) {
                        logger.log(Level.WARNING, e.getLocalizedMessage(), e);
                    }
                }
                ro = registered.poll();
            }
        }
    }

    public void disconnect() {
        running = false;
        selector.wakeup();
    }
    
    private class RegisterOp {

        private Peer peer;
        private int operation;

        private RegisterOp(int op, Peer peer) {
            this.operation = op;
            this.peer = peer;
        }
    }
}
