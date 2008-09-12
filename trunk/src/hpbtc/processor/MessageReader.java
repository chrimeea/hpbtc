package hpbtc.processor;

import hpbtc.protocol.network.Network;
import hpbtc.protocol.torrent.Peer;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Cristian Mocanu
 */
public interface MessageReader extends Network {

    void readMessage(Peer peer)
            throws IOException, NoSuchAlgorithmException;
}
