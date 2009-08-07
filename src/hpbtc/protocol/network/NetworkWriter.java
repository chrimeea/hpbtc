package hpbtc.protocol.network;

import hpbtc.protocol.processor.MessageWriter;
import hpbtc.protocol.torrent.InvalidPeerException;
import hpbtc.protocol.torrent.Peer;
import hpbtc.util.IOUtil;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 *
 * @author Cristian Mocanu
 */
public class NetworkWriter extends NetworkLoop {

    private MessageWriter writer;
    private long uploaded;
    private long lastUploaded;
    private long timestamp;
    private AtomicLong limit;
    private Timer timer;
    private Timer writeTimer = new Timer("WRITE");

    public NetworkWriter(final MessageWriter writer, final Register register,
            final Timer timer) {
        super(register);
        this.stype = Register.SELECTOR_TYPE.TCP_WRITE;
        this.writer = writer;
        this.timestamp = System.currentTimeMillis();
        this.limit = new AtomicLong(Long.MAX_VALUE);
        this.timer = timer;
    }

    /**
     * Set the maximum number of uploaded bytes per second
     */
    public void setLimit(long l) {
        limit.set(l);
    }

    protected void processKey(final SelectionKey key) throws IOException,
            NoSuchAlgorithmException {
        final SocketChannel ch = (SocketChannel) key.channel();
        final Peer peer = (Peer) key.attachment();
        if (key.isConnectable() && ch.finishConnect()) {
            SelectableChannel pChannel = (SelectableChannel) peer.getChannel();
            register.registerNow(pChannel, Register.SELECTOR_TYPE.TCP_READ,
                    SelectionKey.OP_READ, peer);
            pChannel.register(selector, SelectionKey.OP_WRITE, peer);
            writer.connect(peer);
            logger.info("Connected to " + peer + ", local port: " +
                    ch.socket().getLocalPort());
        }
        if (key.isWritable() && ch.isConnected() && !peer.isWriting()) {
            writeNext(key.channel(), key.interestOps(), (Peer) key.attachment());
        }
    }

    private void writeNext(final SelectableChannel channel, final int ops,
            final Peer peer) throws IOException {
        peer.setWriting(true);
        channel.register(selector, ops ^ SelectionKey.OP_WRITE, peer);
        writeTimer.schedule(new TimerTask() {

            public void run() {
                try {
                    while (writeNextInternal(peer));
                    peer.setWriting(false);
                    if (peer.hasMoreMessages()) {
                        register.registerNow(channel, stype,
                                ops | SelectionKey.OP_WRITE, peer);
                    }
                } catch (IOException ex1) {
                    logger.log(Level.INFO, ex1.getLocalizedMessage(), ex1);
                    try {
                        writer.disconnect(peer);
                    } catch (Exception ex3) {
                        logger.log(Level.INFO, ex3.getLocalizedMessage(), ex3);
                    }
                } catch (Exception ex2) {
                    logger.log(Level.INFO, ex2.getLocalizedMessage(), ex2);
                }
            }
        }, 0L);
    }

    @Override
    protected void registerOperation(final SelectableChannel channel,
            int op, final Object peer) {
        final Peer p = (Peer) peer;
        final SocketChannel ch = (SocketChannel) channel;
        if (p.getUploadLimitTask() == null) {
            try {
                if (p.isWriting()) {
                    op ^= SelectionKey.OP_WRITE;
                }
                if (ch != null && ch.isConnected() &&
                        (op & SelectionKey.OP_WRITE) != 0) {
                    writeNext(channel, op, p);
                } else if (ch != null && !ch.isConnected()) {
                    super.registerOperation(channel, op, peer);
                }
            } catch (Exception ex) {
                try {
                    writer.disconnect(p);
                } catch (Exception ex1) {
                }
                logger.log(Level.INFO, ex.getLocalizedMessage(), ex);
            }
        }
    }

    @Override
    protected void disconnect(final SelectionKey key) throws IOException {
        final Peer peer = (Peer) key.attachment();
        try {
            writer.disconnect(peer);
        } catch (InvalidPeerException ex) {
            throw new IOException("Invalid peer " + peer);
        }
    }

    private int uploadToPeer(final Peer peer, final ByteBuffer bb)
            throws InvalidPeerException, IOException {
        if (!peer.isValid()) {
            throw new InvalidPeerException();
        }
        final int i = IOUtil.writeToChannel(peer.getChannel(), bb);
        peer.incrementUploaded(i);
        return i;
    }

    private boolean writeNextInternal(final Peer peer)
            throws IOException, InvalidPeerException {
        final long t = System.currentTimeMillis();

        //check if more than 1 second passed since we measured
        // the bytes uploaded
        final long passed = t - timestamp;
        if (passed > 1000L) {

            //memorize the bytes uploaded so far
            lastUploaded = uploaded;
            timestamp = t;
        }

        // limit - (uploaded - lastUploaded)
        // computes how many bytes we can still upload this second without
        // going over the limit
        final long l = limit.get() - uploaded + lastUploaded;

        // ignore the call to writeNext if the bytes uploaded in this second
        // are more than the upload limit
        // we will continue to ignore the writeNext calls until
        // a second has passed and we can upload again
        if (l > 0) {
            int i = 0;

            //we still may upload maximum l bytes until we reach the limit
            final ByteBuffer currentWrite = peer.getCurrentWrite();
            if (currentWrite != null && currentWrite.remaining() > 0) {
                writer.keepAliveWrite(peer);

                //limit the message so that we don't go over the upload limit
                if (currentWrite.remaining() > l) {
                    currentWrite.limit(currentWrite.position() + (int) l);
                }

                //upload the message
                i = uploadToPeer(peer, currentWrite);
                uploaded += i;

                //clear the limit
                currentWrite.limit(currentWrite.capacity());
            }
            return peer.hasMoreMessages();
        } else {
            final TimerTask tt = new TimerTask() {

                @Override
                public void run() {
                    try {
                        peer.setUploadLimitTask(null);
                        if (peer.hasMoreMessages()) {
                            register.registerNow((SelectableChannel) peer.getChannel(),
                                    Register.SELECTOR_TYPE.TCP_WRITE,
                                    SelectionKey.OP_WRITE, peer);
                        }
                    } catch (IOException ex) {
                        logger.log(Level.INFO, ex.getLocalizedMessage(), ex);
                        try {
                            writer.disconnect(peer);
                        } catch (Exception ex1) {
                            logger.log(Level.INFO, ex1.getLocalizedMessage(), ex1);
                        }
                    }
                }
            };
            peer.setUploadLimitTask(tt);
            timer.schedule(tt, 1000L - passed);
            return false;
        }
    }
}
