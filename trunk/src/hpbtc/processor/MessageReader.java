package hpbtc.processor;

import hpbtc.protocol.torrent.Peer;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Cristian Mocanu
 */
public interface MessageReader {

    void readMessage(Peer peer)
            throws IOException, NoSuchAlgorithmException;
    
    void disconnect(Peer peer) throws IOException;
}
