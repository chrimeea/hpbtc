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
import java.util.logging.Logger;
import util.IOUtil;

/**
 * @author chris
 *
 */
public class Network {

    public static final int MIN_PORT = 6881;
    public static final int MAX_PORT = 6999;
    private static Logger logger = Logger.getLogger(Network.class.getName());
    private ServerSocketChannel serverCh;
    private Selector selector;
    private Queue<RegisterOp> registered;
    private Map<Peer, Queue<SimpleMessage>> messagesToSend;
    private Queue<RawMessage> messagesReceived;
    private ByteBuffer currentRead;
    private boolean running;
    private ByteBuffer currentWrite;

    public Network() {
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
                    if (pm.getIndex() == index && pm.getBegin() == begin && pm.getLength() == length) {
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
        final Network net = this;
        new Thread(new Runnable() {

            public void run() {
                try {
                    listen(net);
                } catch (IOException e) {
                    running = false;
                    logger.warning(e.getLocalizedMessage());
                }
                try {
                    selector.close();
                } catch (IOException e) {
                    logger.warning(e.getLocalizedMessage());
                }
                try {
                    serverCh.close();
                } catch (IOException e) {
                    logger.warning(e.getLocalizedMessage());
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
        SocketChannel ch = peer.getChannel();
        if (ch != null) {
            if ((ch.keyFor(selector).interestOps() & op) == 0) {
                registered.add(new RegisterOp(op, peer));
            }
        } else {
            ch = SocketChannel.open();
            ch.configureBlocking(false);
            peer.setChannel(ch);
            if (ch.connect(peer.getAddress())) {
                registered.add(new RegisterOp(op, peer));
            } else {
                registered.add(new RegisterOp(SelectionKey.OP_CONNECT | op, peer));
            }
        }
        selector.wakeup();
    }

    public boolean hasUnreadMessages() {
        return !messagesReceived.isEmpty();
    }

    private void readMessage(Peer peer, Network net) {
        int i;
        try {
            i = IOUtil.readFromChannel(peer.getChannel(), currentRead);
        } catch (IOException e) {
            logger.warning(e.getLocalizedMessage());
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

    private void disconnectedByPeer(Peer peer, Network net) {
        try {
            closeConnection(peer);
        } catch (IOException e) {
            logger.warning(e.getLocalizedMessage());
        }
        messagesToSend.remove(peer);
        messagesReceived.add(new RawMessage(peer));
        synchronized (net) {
            net.notify();
        }
    }

    public void closeConnection(Peer peer) throws IOException {
        SocketChannel ch = peer.getChannel();
        if (ch != null) {
            ch.close();
        }
    }

    private void writeNext(Peer peer, Network net) {
        if (currentWrite == null || currentWrite.remaining() == 0) {
            Queue<SimpleMessage> q = messagesToSend.get(peer);
            currentWrite = q.poll().send();
        }
        try {
            IOUtil.writeToChannel(peer.getChannel(), currentWrite);
        } catch (IOException e) {
            logger.warning(e.getLocalizedMessage());
            disconnectedByPeer(peer, net);
        }
    }

    public RawMessage takeMessage() {
        return messagesReceived.poll();
    }

    public void postMessage(Peer peer, SimpleMessage message) throws IOException {
        Queue<SimpleMessage> q = messagesToSend.get(peer);
        if (q == null) {
            q = new ConcurrentLinkedQueue<SimpleMessage>();
            messagesToSend.put(peer, q);
        }
        q.add(message);
        registerNow(peer, SelectionKey.OP_WRITE);
    }

    private void listen(Network net) throws IOException {
        while (running) {
            int n = selector.select();
            RegisterOp ro = registered.poll();
            while (ro != null) {
                SelectableChannel q = ro.peer.getChannel();
                SelectionKey w = q.keyFor(selector);
                if (w != null && w.isValid()) {
                    q.register(selector, w.interestOps() | ro.operation, ro.peer);
                } else if (w == null) {
                    q.register(selector, ro.operation, ro.peer);
                }
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
                    if (key.isAcceptable()) {
                        SocketChannel chan = serverCh.accept();
                        chan.configureBlocking(false);
                        chan.register(selector, SelectionKey.OP_READ, new Peer(chan));
                    } else {
                        Peer peer = (Peer) key.attachment();
                        SocketChannel ch = (SocketChannel) key.channel();
                        if (key.isConnectable() && ch.finishConnect()) {
                            ch.register(selector, (key.interestOps() | SelectionKey.OP_READ) & ~SelectionKey.OP_CONNECT, peer);
                        } else {
                            if (key.isReadable()) {
                                ch.register(selector, key.interestOps() & ~SelectionKey.OP_READ, peer);
                                readMessage(peer, net);
                                if (ch.isOpen()) {
                                    ch.register(selector, key.interestOps() | SelectionKey.OP_READ, peer);
                                }
                            }
                            if (key.isValid() && key.isWritable()) {
                                ch.register(selector, key.interestOps() & ~SelectionKey.OP_WRITE, peer);
                                writeNext(peer, net);
                                if (ch.isOpen()) {
                                    Queue<SimpleMessage> q = messagesToSend.get(peer);
                                    if (!q.isEmpty()) {
                                        ch.register(selector, key.interestOps() | SelectionKey.OP_WRITE, peer);
                                    }
                                }
                            }
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
