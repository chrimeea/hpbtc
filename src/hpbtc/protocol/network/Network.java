/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package hpbtc.protocol.network;

import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.torrent.Peer;
import java.io.IOException;

/**
 *
 * @author Administrator
 */
public interface Network {

    void cancelPieceMessage(int begin, int index, int length, Peer peer);

    void closeConnection(Peer peer) throws IOException;

    void connect() throws IOException;

    void disconnect();

    /**
     * @return
     */
    int getPort();

    boolean hasUnreadMessages();

    boolean isRunning();

    void postMessage(Peer peer, SimpleMessage message) throws IOException;

    RawMessage takeMessage();

}
