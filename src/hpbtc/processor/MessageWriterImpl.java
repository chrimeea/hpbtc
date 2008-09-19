package hpbtc.processor;

import hpbtc.protocol.message.LengthPrefixMessage;
import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.network.Register;
import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.torrent.Torrent;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu
 */
public class MessageWriterImpl implements MessageWriter {

    private static Logger logger = Logger.getLogger(MessageWriterImpl.class.
            getName());
    private ByteBuffer currentWrite;
    private Register register;
    private Timer timer;

    public MessageWriterImpl(final Register register, final Timer timer) {
        this.register = register;
        this.timer = timer;
    }

    public void disconnect(final Peer peer) throws IOException {
        register.disconnect(peer);
        peer.disconnect();
    }
    
    private void keepAliveWrite(final Peer peer) {
        peer.cancelKeepAliveWrite();
        TimerTask tt = new TimerTask() {

            @Override
            public void run() {
                try {
                    postMessage(new LengthPrefixMessage(0, peer));
                } catch (IOException e) {
                    logger.log(Level.WARNING, e.getLocalizedMessage(), e);
                }
            }
        };
        timer.schedule(tt, 60000);
        peer.setKeepAliveWrite(tt);        
    }
    
    public void writeNext(final Peer peer) throws IOException {
        keepAliveWrite(peer);
        if (currentWrite == null || currentWrite.remaining() == 0) {
            LengthPrefixMessage sm = null;
            sm = peer.getMessageToSend();
            currentWrite = sm.send();
            currentWrite.rewind();
            logger.fine("Sending: " + sm);
        }
        peer.upload(currentWrite);
        if (currentWrite.remaining() > 0 || peer.isMessagesToSendEmpty()) {
            register.clearWrite(peer);
        }
    }
    
    public void cancelPieceMessage(final int begin, final int index,
            final int length, final Peer peer) {
        Iterable<LengthPrefixMessage> q = peer.listMessagesToSend();
        if (q != null) {
            Iterator<LengthPrefixMessage> i = q.iterator();
            while (i.hasNext()) {
                LengthPrefixMessage m = i.next();
                if (m instanceof PieceMessage) {
                    PieceMessage pm = (PieceMessage) m;
                    if (pm.getIndex() == index && pm.getBegin() == begin &&
                            pm.getLength() == length) {
                        i.remove();
                        Peer p = pm.getDestination();
                        Torrent t = p.getTorrent();
                        p.removeRequest(pm.getIndex(), pm.getBegin(),
                                t.getChunkSize());
                    }
                }
            }
        }
    }
    
    public void cancelPieceMessage(final Peer peer) {
        Iterable<LengthPrefixMessage> q = peer.listMessagesToSend();
        if (q != null) {
            Iterator<LengthPrefixMessage> i = q.iterator();
            while (i.hasNext()) {
                LengthPrefixMessage m = i.next();
                if (m instanceof PieceMessage) {
                    i.remove();
                    PieceMessage pm = (PieceMessage) m;
                    Peer p = pm.getDestination();
                    Torrent t = p.getTorrent();
                    peer.removeRequest(pm.getIndex(), pm.getBegin(),
                            t.getChunkSize());
                }
            }
        }
    }
    
    public void postMessage(final LengthPrefixMessage message) throws IOException {
        message.getDestination().addMessageToSend(message);
        register.registerWrite(message.getDestination());
    }
}
