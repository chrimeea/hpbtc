/*
 * Created on Jan 20, 2006
 *
 */
package hpbtc.protocol.network;

import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.torrent.Peer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author chris
 *
 */
public class PeerNetwork implements Network {

    private static final int MIN_PORT = 6881;
    private static final int MAX_PORT = 6999;
    private static Logger logger = Logger.getLogger(PeerNetwork.class.getName());
    private ServerSocketChannel serverCh;
    private Selector selector;
    private Queue<RegisterOp> registered;
    private Map<Peer, Queue<SimpleMessage>> messagesToSend;
    private Queue<RawMessage> messagesReceived;
    private ByteBuffer currentRead;
    private boolean running;
    private ByteBuffer currentWrite;

    public PeerNetwork() {
        messagesReceived = new ConcurrentLinkedQueue<RawMessage>();
        messagesToSend = new ConcurrentHashMap<Peer, Queue<SimpleMessage>>();
        registered = new ConcurrentLinkedQueue<RegisterOp>();
        currentRead = ByteBuffer.allocate(16384);
    }

    public void cancelPieceMessage(int begin, int index, int length, Peer peer) {
        Queue<SimpleMessage> q = messagesToSend.get(peer);
        if (q != null) {
            Iterator<SimpleMessage> i = q.iterator();
            while (i.hasNext()) {
                SimpleMessage m = i.next();
                if (m instanceof PieceMessage) {
                    PieceMessage pm = (PieceMessage) m;
                    if (pm.getIndex() == index && pm.getBegin() == begin && pm.
                            getLength() == length) {
                        i.remove();
                    }
                }
            }
        }
    }

    public void connect() throws IOException {
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
            serverCh.configureBlocking(false);
            selector = Selector.open();
            serverCh.register(selector, SelectionKey.OP_ACCEPT);
        }
        running = true;
        final PeerNetwork net = this;
        new Thread(new Runnable() {

            public void run() {
                try {
                    listen(net);
                } catch (IOException e) {
                    running = false;
                    logger.log(Level.WARNING, e.getLocalizedMessage(), e);
                }
                try {
                    selector.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, e.getLocalizedMessage(), e);
                }
                try {
                    serverCh.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, e.getLocalizedMessage(), e);
                }
                synchronized (net) {
                    net.notify();
                }
            }
        }).start();
    }

    public void disconnect() {
        running = false;
        selector.wakeup();
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * @return
     */
    public int getPort() {
        return serverCh.socket().getLocalPort();
    }

    private void registerNow(Peer peer, int op) throws IOException {
        SocketChannel ch = (SocketChannel) peer.getChannel();
        if (ch != null) {
            SelectionKey sk = ch.keyFor(selector);
            if (sk == null || (sk != null && (sk.interestOps() & op) == 0)) {
                registered.add(new RegisterOp(op, peer));
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
        }
        selector.wakeup();
    }

    public boolean hasUnreadMessages() {
        return !messagesReceived.isEmpty();
    }

    private void readMessage(Peer peer, PeerNetwork net) throws IOException {
        int i;
        try {
            i = peer.download(currentRead);
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getLocalizedMessage(), e);
            i = currentRead.position();
            disconnectedByPeer(peer, net);
        }
        if (i > 0) {
            byte[] b = new byte[i];
            currentRead.rewind();
            currentRead.get(b);
            currentRead.clear();
            messagesReceived.add(new RawMessage(peer, b));
            synchronized (net) {
                net.notify();
            }
        }
    }

    private void disconnectedByPeer(Peer peer, PeerNetwork net) throws
            IOException {
        closeConnection(peer);
        messagesReceived.add(new RawMessage(peer));
        synchronized (net) {
            net.notify();
        }
    }

    public void closeConnection(Peer peer) throws IOException {
        messagesToSend.remove(peer);
        ByteChannel ch = peer.getChannel();
        if (ch != null) {
            ch.close();
        }
    }

    private void writeNext(Peer peer, PeerNetwork net) throws IOException {
        if (currentWrite == null || currentWrite.remaining() == 0) {
            Queue<SimpleMessage> q = messagesToSend.get(peer);
            currentWrite = q.poll().send();
        }
        try {
            peer.upload(currentWrite);
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getLocalizedMessage(), e);
            disconnectedByPeer(peer, net);
        }
    }

    public RawMessage takeMessage() {
        return messagesReceived.poll();
    }

    public void postMessage(SimpleMessage message) throws IOException {
        Peer peer = message.getDestination();
        Queue<SimpleMessage> q = messagesToSend.get(peer);
        if (q == null) {
            q = new ConcurrentLinkedQueue<SimpleMessage>();
            messagesToSend.put(peer, q);
        }
        q.add(message);
        registerNow(peer, SelectionKey.OP_WRITE);
    }

    private void listen(PeerNetwork net) throws IOException {
        while (running) {
            int n = selector.select();
            RegisterOp ro = registered.poll();
            while (ro != null) {
                SelectableChannel q = (SelectableChannel) ro.peer.getChannel();
                SelectionKey w = q.keyFor(selector);
                if (w != null && w.isValid()) {
                    q.register(selector, w.interestOps() | ro.operation, ro.peer);
                } else if (w == null) {
                    q.register(selector, ro.operation, ro.peer);
                }
                logger.info("Registered peer " + ro.peer.getAddress());
                ro = registered.poll();
            }
            if (n == 0) {
                continue;
            }
            Iterator<SelectionKey> i = selector.selectedKeys().iterator();
            while (i.hasNext()) {
                SelectionKey key = i.next();
                i.remove();
                if (key.isValid()) {
                    Peer peer;
                    if (key.isAcceptable()) {
                        SocketChannel chan = serverCh.accept();
                        chan.configureBlocking(false);
                        peer = new Peer(chan);
                        chan.register(selector, SelectionKey.OP_READ, peer);
                        logger.info("Accepted connection from " + peer.
                                getAddress());
                    } else {
                        peer = (Peer) key.attachment();
                        SocketChannel ch = (SocketChannel) key.channel();
                        try {
                            if (key.isConnectable() && ch.finishConnect()) {
                                ch.register(selector, (key.interestOps() |
                                        SelectionKey.OP_READ) &
                                        ~SelectionKey.OP_CONNECT, peer);
                                logger.info("Connected to peer " + peer.
                                        getAddress());
                            } else {
                                if (key.isReadable()) {
                                    ch.register(selector, key.interestOps() &
                                            ~SelectionKey.OP_READ, peer);
                                    readMessage(peer, net);
                                    if (ch.isOpen()) {
                                        ch.register(selector,
                                                key.interestOps() |
                                                SelectionKey.OP_READ, peer);
                                    }
                                    logger.info("Received message from peer " +
                                            peer.getAddress());
                                }
                                if (key.isValid() && key.isWritable()) {
                                    ch.register(selector, key.interestOps() &
                                            ~SelectionKey.OP_WRITE, peer);
                                    writeNext(peer, net);
                                    if (ch.isOpen()) {
                                        Queue<SimpleMessage> q = messagesToSend.
                                                get(peer);
                                        if (!q.isEmpty()) {
                                            ch.register(selector, key.
                                                    interestOps() |
                                                    SelectionKey.OP_WRITE, peer);
                                        }
                                    }
                                    logger.info("Sent message to peer " + peer.
                                            getAddress());
                                }
                            }
                        } catch (IOException ioe) {
                            logger.log(Level.WARNING, ioe.getLocalizedMessage(), ioe);
                        }
                    }
                }
            }
        }
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
