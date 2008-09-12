package hpbtc.processor;

import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.network.Network;
import hpbtc.protocol.torrent.Peer;
import java.io.IOException;

/**
 *
 * @author Chris
 */
public interface MessageWriter extends Network {

    void postMessage(SimpleMessage message) throws IOException;
    
    void writeNext(Peer peer) throws IOException;
    
    void closeConnection(Peer peer) throws IOException;
    
    void cancelPieceMessage(int begin, int index, int length, Peer peer);
    
    boolean isEmpty(Peer peer);
}
