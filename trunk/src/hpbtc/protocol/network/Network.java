/*
 * Created on Jan 20, 2006
 *
 */
package hpbtc.protocol.network;

import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.message.SimpleMessage;
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
import java.util.Set;
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
    private Map<InetSocketAddress, Queue<SimpleMessage>> messagesToSend;
    private Queue<RawMessage> messagesReceived;
    private ByteBuffer current;
    private boolean running;

    public Network() {
        messagesReceived = new ConcurrentLinkedQueue<RawMessage>();
        messagesToSend = new ConcurrentHashMap<InetSocketAddress, Queue<SimpleMessage>>();
        registered = new ConcurrentLinkedQueue<RegisterOp>();
        current = ByteBuffer.allocate(16384);
    }

    public void cancelPieceMessage(int begin, int index, int length, InetSocketAddress peer) {
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

    private SocketChannel findByAddress(InetSocketAddress peer) {
        Set<SelectionKey> keys = selector.keys();
        for (SelectionKey k : keys) {
            SelectableChannel ch = k.channel();
            if (ch instanceof SocketChannel) {
                SocketChannel sc = (SocketChannel) ch;
                if (((InetSocketAddress) sc.socket().getRemoteSocketAddress()).equals(peer)) {
                    return sc;
                }
            }
        }
        return null;
    }

    private void registerNow(InetSocketAddress peer, int op) throws IOException {
        SocketChannel ch = findByAddress(peer);
        if (ch != null) {
            if ((ch.keyFor(selector).interestOps() & op) == 0) {
                registered.add(new RegisterOp(op, ch));
            }
        } else {
            ch = SocketChannel.open();
            ch.configureBlocking(false);
            if (ch.connect(peer)) {
                registered.add(new RegisterOp(op, ch));
            } else {
                registered.add(new RegisterOp(SelectionKey.OP_CONNECT | op, ch));
            }
        }
        selector.wakeup();
    }

    public boolean hasUnreadMessages() {
        return !messagesReceived.isEmpty();
    }

    private void readMessage(SocketChannel ch, Network net) {
        int i;
        try {
            i = IOUtil.readFromChannel(ch, current);
        } catch (IOException e) {
            logger.warning(e.getLocalizedMessage());
            i = current.position();
            disconnectedByPeer(ch, net);
        }
        if (i > 0) {
            byte[] b = new byte[i];
            current.rewind();
            current.get(b);
            current.clear();
            messagesReceived.add(new RawMessage(IOUtil.getAddress(ch), b));
            synchronized (net) {
                net.notify();
            }
        }
    }

    private void disconnectedByPeer(SocketChannel ch, Network net) {
        try {
            ch.close();
        } catch (IOException e) {
            logger.warning(e.getLocalizedMessage());
        }
        messagesReceived.add(new RawMessage(IOUtil.getAddress(ch)));
        synchronized (net) {
            net.notify();
        }
    }

    public void closeConnection(InetSocketAddress address) throws IOException {
        SocketChannel ch = findByAddress(address);
        if (ch != null) {
            ch.close();
        }
    }

    private void writeNext(SocketChannel ch, Network net) {
        Queue<SimpleMessage> q = messagesToSend.get(IOUtil.getAddress(ch));
        try {
            do {
                ByteBuffer b = q.poll().send();
                do {
                    IOUtil.writeToChannel(ch, b);
                } while (b.remaining() == 0);
            } while (!q.isEmpty());
        } catch (IOException e) {
            logger.warning(e.getLocalizedMessage());
            disconnectedByPeer(ch, net);
        }
    }

    public RawMessage takeMessage() {
        return messagesReceived.poll();
    }

    public void postMessage(InetSocketAddress peer, SimpleMessage message) throws IOException {
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
                SelectableChannel q = ro.channel;
                SelectionKey w = q.keyFor(selector);
                if (w != null && w.isValid()) {
                    q.register(selector, w.interestOps() | ro.operation);
                } else if (w == null) {
                    q.register(selector, ro.operation);
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
                        chan.register(selector, SelectionKey.OP_READ);
                    } else {
                        SocketChannel ch = (SocketChannel) key.channel();
                        if (key.isConnectable() && ch.finishConnect()) {
                            ch.register(selector, (key.interestOps() | SelectionKey.OP_READ) & ~SelectionKey.OP_CONNECT);
                        } else {
                            if (key.isReadable()) {
                                ch.register(selector, key.interestOps() & ~SelectionKey.OP_READ);
                                readMessage(ch, net);
                                if (ch.isOpen()) {
                                    ch.register(selector, key.interestOps() | SelectionKey.OP_READ);
                                }
                            }
                            if (key.isValid() && key.isWritable()) {
                                ch.register(selector, key.interestOps() & ~SelectionKey.OP_WRITE);
                                writeNext(ch, net);
                                if (ch.isOpen() && !messagesToSend.isEmpty()) {
                                    ch.register(selector, key.interestOps() | SelectionKey.OP_WRITE);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private class RegisterOp {

        private SelectableChannel channel;
        private int operation;

        private RegisterOp(int op, SelectableChannel channel) {
            this.operation = op;
            this.channel = channel;
        }
    }
}
