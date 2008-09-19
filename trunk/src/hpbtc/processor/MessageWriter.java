package hpbtc.processor;

import hpbtc.protocol.message.LengthPrefixMessage;
import hpbtc.protocol.torrent.Peer;
import java.io.IOException;

/**
 *
 * @author Cristian Mocanu
 */
public interface MessageWriter {
    
    void writeNext(Peer peer) throws IOException;
    
    void disconnect(Peer peer) throws IOException;
    
    void postMessage(LengthPrefixMessage message) throws IOException;
    
    void cancelPieceMessage(Peer peer);
    
    void cancelPieceMessage(int begin, int index, int length, Peer peer);
}
