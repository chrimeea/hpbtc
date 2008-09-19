package hpbtc.processor;

import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.network.Register;
import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.torrent.Torrent;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
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

    public MessageWriterImpl(final Register register) {
        this.register = register;
    }

    public void disconnect(final Peer peer) throws IOException {
        register.disconnect(peer);
        peer.disconnect();
    }
    
    public void writeNext(final Peer peer) throws IOException {
        if (currentWrite == null || currentWrite.remaining() == 0) {
            SimpleMessage sm = null;
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
        Iterable<SimpleMessage> q = peer.listMessagesToSend();
        if (q != null) {
            Iterator<SimpleMessage> i = q.iterator();
            while (i.hasNext()) {
                SimpleMessage m = i.next();
                if (m.getMessageType() == SimpleMessage.TYPE_PIECE) {
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
        Iterable<SimpleMessage> q = peer.listMessagesToSend();
        if (q != null) {
            Iterator<SimpleMessage> i = q.iterator();
            while (i.hasNext()) {
                SimpleMessage m = i.next();
                if (m.getMessageType() == SimpleMessage.TYPE_PIECE) {
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
    
    public void postMessage(final SimpleMessage message) throws IOException {
        message.getDestination().addMessageToSend(message);
        register.registerWrite(message.getDestination());
    }
}
