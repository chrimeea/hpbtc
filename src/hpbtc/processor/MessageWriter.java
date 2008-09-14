package hpbtc.processor;

import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.torrent.Peer;
import java.io.IOException;

/**
 *
 * @author Cristian Mocanu
 */
public interface MessageWriter {

    void postMessage(SimpleMessage message) throws IOException;
    
    void writeNext(Peer peer) throws IOException;
    
    void cancelPieceMessage(Peer peer);
    
    void cancelPieceMessage(int begin, int index, int length, Peer peer);
    
    boolean isEmpty(Peer peer);
    
    void disconnect(final Peer peer) throws IOException;
}
