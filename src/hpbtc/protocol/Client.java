/*
 * Created on Jan 20, 2006
 *
 */
package hpbtc.protocol;

import hpbtc.protocol.torrent.Peer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/**
 * @author chris
 *
 */
public class Client {

    public static final int REQUEST_PIECE_LENGTH = 32768;
    public static final int MIN_PORT = 6881;
    public static final int MAX_PORT = 6999;
    private static Logger logger = Logger.getLogger(Client.class.getName());
    private ServerSocketChannel serverCh;
    private Selector selector;
    private byte[] peerId;
    private Random r;
    private Queue<RegisterOp> registered;

    public Client() throws IOException {
        registered = new ConcurrentLinkedQueue<RegisterOp>();
        r = new Random();
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
            pid[i] = (byte) (r.nextInt(256) - 128);
        }
        return pid;
    }

    /**
     * @return
     */
    public int getPort() {
        return serverCh.socket().getLocalPort();
    }

    public void registerNow(PeerConnection pc, int op) {
        RegisterOp o = new RegisterOp(pc, op);
        registered.add(o);
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
                PeerConnection po = ro.getPeerConnection();
                SocketChannel q = po.getChannel();
                SelectionKey w = q.keyFor(selector);
                try {
                    if (w != null && w.isValid()) {
                        q.register(selector, w.interestOps() | ro.getOp(), po);
                    } else if (w == null) {
                        q.register(selector, ro.getOp(), po);
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
                        PeerConnection con = (PeerConnection) key.attachment();
                        SocketChannel ch = con == null ? null : con.getChannel();
                        if (key.isReadable()) {
                            if (con.getChannel().isConnected()) {
                                ch.register(selector, key.interestOps() & ~SelectionKey.OP_READ, con);
                                con.readMessage();
                                ch.register(selector, key.interestOps() | SelectionKey.OP_READ, con);
                            }
                        }
                        if (key.isWritable()) {
                            ch.register(selector, key.interestOps() & ~SelectionKey.OP_WRITE, con);
                            if (!con.isUploadEmpty()) {
                                con.writeNext();
                                if (!con.isUploadEmpty()) {
                                    ch.register(selector, key.interestOps() | SelectionKey.OP_WRITE, con);
                                }
                            }
                        }
                        if (key.isAcceptable()) {
                            SocketChannel chan = serverCh.accept();
                            Socket s = chan.socket();
                            chan.configureBlocking(false);
                            Peer p = new Peer(s.getInetAddress().getHostAddress(), s.getPort(), null);
                            PeerConnection pc = new PeerConnection(p);
                            pc.setChannel(chan);
                            chan.register(selector, SelectionKey.OP_READ, pc);
                            logger.info("incoming connection " + s.getInetAddress().getHostAddress());
                        } else if (key.isConnectable()) {
                            try {
                                if (con.getChannel().finishConnect()) {
                                    ch.register(selector, (key.interestOps() | SelectionKey.OP_READ) & ~SelectionKey.OP_CONNECT, con);
                                    con.finishConnect();
                                }
                            } catch (IOException e) {
                                logger.info("connection failed " + e.getMessage());
                                con.close();
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

    private class RegisterOp {

        private PeerConnection pc;
        private int op;

        private RegisterOp(PeerConnection pc, int op) {
            this.pc = pc;
            this.op = op;
        }

        private PeerConnection getPeerConnection() {
            return pc;
        }

        private int getOp() {
            return op;
        }
    }
}
