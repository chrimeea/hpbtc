/*
 * Created on Jan 20, 2006
 *
 */
package hpbtc.protocol;

import hpbtc.protocol.message.ProtocolMessage;
import hpbtc.protocol.message.HandshakeMessage;
import hpbtc.protocol.message.MessageFactory;
import hpbtc.protocol.message.PIDMessage;

import hpbtc.util.IOUtil;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/**
 * @author chris
 *
 */
public class Client {

    public static final int MIN_PORT = 6881;
    public static final int MAX_PORT = 6999;
    private static Logger logger = Logger.getLogger(Client.class.getName());
    private ServerSocketChannel serverCh;
    private Selector selector;
    private byte[] peerId;
    private Random rand = new Random();
    private Queue<RegisterOp> registered = new ConcurrentLinkedQueue<RegisterOp>();
    private Map<Peer, Queue<ByteBuffer>> messagesUpload = new HashMap<Peer, Queue<ByteBuffer>>();
    private int uploaded;
    private int downloaded;
    private Queue<ClientProtocolMessage> messagesDownload = new ConcurrentLinkedQueue<ClientProtocolMessage>();
    private int length;
    private boolean handshakeRead;
    private boolean pidRead;
    private int read;
    private ByteBuffer head;
    private ByteBuffer current;

    public Client() throws IOException {
        peerId = generateId();
    }

    public void connect() throws IOException {
        int port = MIN_PORT;
        try {
            serverCh = ServerSocketChannel.open();
        } catch (IOException e) {
            logger.severe("Can not open server socket");
            return;
        }
        while (port <= MAX_PORT) {
            try {
                InetSocketAddress isa = new InetSocketAddress(
                        InetAddress.getLocalHost(), port);
                serverCh.socket().bind(isa);
                break;
            } catch (IOException e) {
                logger.warning("Port " + port + " is not available");
                port++;
            }
        }
        if (port > MAX_PORT) {
            throw new IOException("No ports available");
        } else {
            logger.info("server started " + port);
            serverCh.configureBlocking(false);
            selector = Selector.open();
            serverCh.register(selector, SelectionKey.OP_ACCEPT, null);
        }
    }

    /**
     * @return
     */
    public byte[] getPIDBytes() {
        return peerId;
    }

    private byte[] generateId() {
        byte[] pid = new byte[20];
        pid[0] = '-';
        pid[1] = 'H';
        pid[2] = 'P';
        pid[3] = '0';
        pid[4] = '1';
        pid[5] = '0';
        pid[6] = '0';
        pid[7] = '-';
        for (int i = 8; i < 20; i++) {
            pid[i] = (byte) (rand.nextInt(256) - 128);
        }
        return pid;
    }

    /**
     * @return
     */
    public int getPort() {
        return serverCh.socket().getLocalPort();
    }

    private void registerNow(Peer peer, int op) {
        registered.add(new RegisterOp(peer, op, channel));
        selector.wakeup();
    }

    public void listen() {
        while (true) {
            int n;
            try {
                n = selector.select();
            } catch (ClosedSelectorException e) {
                break;
            } catch (IOException e) {
                continue;
            }
            RegisterOp ro = registered.poll();
            while (ro != null) {
                Peer po = ro.peer;
                SocketChannel q = ro.channel;
                SelectionKey w = q.keyFor(selector);
                try {
                    if (w != null && w.isValid()) {
                        q.register(selector, w.interestOps() | ro.operation, po);
                    } else if (w == null) {
                        q.register(selector, ro.operation, po);
                    }
                } catch (CancelledKeyException e) {
                    logger.warning("A key was unexpectedly canceled");
                } catch (ClosedChannelException e) {
                    logger.warning("A channel was unexpectedly closed");
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
                try {
                    if (key.isValid()) {
                        Peer peer = (Peer) key.attachment();
                        SocketChannel ch = (SocketChannel) key.channel();
                        if (key.isReadable()) {
                            if (ch.isConnected()) {
                                ch.register(selector, key.interestOps() & ~SelectionKey.OP_READ, peer);
                                readMessage(ch);
                                ch.register(selector, key.interestOps() | SelectionKey.OP_READ, peer);
                            }
                        }
                        if (key.isWritable()) {
                            ch.register(selector, key.interestOps() & ~SelectionKey.OP_WRITE, peer);
                            if (writeNext(ch, peer)) {
                                ch.register(selector, key.interestOps() | SelectionKey.OP_WRITE, peer);
                            }
                        }
                        if (key.isAcceptable()) {
                            SocketChannel chan = serverCh.accept();
                            Socket s = chan.socket();
                            chan.configureBlocking(false);
                            Peer p = new Peer(s.getInetAddress().getHostAddress(), s.getPort(), null);
                            chan.register(selector, SelectionKey.OP_READ, p);
                            logger.info("incoming connection " + s.getInetAddress().getHostAddress());
                        } else if (key.isConnectable()) {
                            try {
                                if (ch.finishConnect()) {
                                    ch.register(selector, (key.interestOps() | SelectionKey.OP_READ) & ~SelectionKey.OP_CONNECT, peer);
                                }
                            } catch (IOException e) {
                                logger.info("connection failed " + e.getMessage());
                                ch.socket().close();
                            }
                        }
                    }
                } catch (CancelledKeyException e) {
                    logger.warning("A key was unexpectedly canceled");
                } catch (ClosedChannelException e) {
                    logger.warning("A channel was unexpectedly closed");
                } catch (IOException e) {
                    logger.warning("Error while multiplexing " + e.getMessage());
                }
            }
        }
    }

    public boolean addUploadMessage(ProtocolMessage rm, Peer peer) {
        //TODO: daca nu am deschis inca un canal cu acest peer
        /*
            ProtocolMessage hm = new HandshakeMessage(infoHash);
            addUploadMessage(hm, peer);
            hm = new PIDMessage(peerId);
            addUploadMessage(hm, peer);

        ch = SocketChannel.open();
        ch.configureBlocking(false);
        InetSocketAddress addr = new InetSocketAddress(peer.getIp(), peer.getPort());
        final Client cl = Client.getInstance();
        final DownloadItem item = cl.getDownloadItem();
        try {
            if (ch.connect(addr)) {
                cl.registerNow(this, SelectionKey.OP_READ);
                finishConnect();
                item.initiateConnections(1);
            } else {
                cl.registerNow(this, SelectionKey.OP_CONNECT);
            }

         */
        Queue q = messagesUpload.get(peer);
        if (q == null) {
            q = new ConcurrentLinkedQueue<ByteBuffer>();
            messagesUpload.put(peer, q);
        }
        boolean r = q.offer(rm.send());
        if (q.isEmpty() && r) {
            registerNow(peer, SelectionKey.OP_WRITE);
        }
        return r;
    }

    private boolean writeNext(SocketChannel ch, Peer peer) {
        Queue<ByteBuffer> q = messagesUpload.get(peer);
        try {
            ByteBuffer pm = q.peek();
            if (pm != null && pm.hasRemaining()) {
                uploaded += IOUtil.writeToSocket(ch, pm);
                if (!pm.hasRemaining()) {
                    q.remove();
                }
            }
        } catch (IOException e) {
            logger.warning("Error while sending message to peer " + peer.getIp() + " " + e.getMessage());
            ch.socket().close();
        }
        return !q.isEmpty();
    }

    private void startReading(SocketChannel ch) throws IOException {
        int r = IOUtil.readFromSocket(ch, current);
        downloaded += r;
        read += r;
        if (read == length) {
            current.rewind();
            ProtocolMessage pm;
            if (!handshakeRead) {
                pm = new HandshakeMessage();
                handshakeRead = true;
            } else if (!pidRead) {
                pm = new PIDMessage();
                pidRead = true;
            } else {
                pm = MessageFactory.createMessage(current.get());
            }
            read = 0;
            length = 0;
        }
    }

    private void readMessage(SocketChannel ch) {
        try {
            if (length == 0) {
                if (!handshakeRead) {
                    length = 48;
                } else if (!pidRead) {
                    length = 20;
                } else {
                    int r = IOUtil.readFromSocket(ch, head);
                    downloaded += r;
                    read += r;
                    if (read == 4) {
                        length = head.getInt(0);
                        head.rewind();
                        read = 0;
                        if (length == 0) {
                            return;
                        }
                    } else {
                        return;
                    }
                }
                current = ByteBuffer.allocate(length);
            }
            startReading(ch);
        } catch (IOException e) {
            InetAddress s = ch.socket().getInetAddress();
            String addr = s == null ? "unknown" : s.getHostAddress();
            logger.warning("Error while reading message from peer " + addr + " " + e.getMessage());
            ch.socket().close();
        }
    }

    private class RegisterOp {

        private SocketChannel channel;
        private Peer peer;
        private int operation;

        private RegisterOp(Peer peer, int op, SocketChannel channel) {
            this.peer = peer;
            this.operation = op;
            this.channel = channel;
        }
    }
}
