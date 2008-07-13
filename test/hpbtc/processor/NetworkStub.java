package hpbtc.processor;

import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.network.Network;
import hpbtc.protocol.network.RawMessage;
import hpbtc.protocol.torrent.Peer;

/**
 *
 * @author Administrator
 */
public class NetworkStub implements Network {

    public void cancelPieceMessage(int arg0, int arg1, int arg2, Peer arg3) {
    }

    public void closeConnection(Peer arg0) {
    }

    public void connect() {
    }

    public void disconnect() {
    }

    public int getPort() {
        return 0;
    }

    public boolean hasUnreadMessages() {
        return false;
    }

    public boolean isRunning() {
        return false;
    }

    public void postMessage(Peer arg0, SimpleMessage arg1) {
    }

    public RawMessage takeMessage() {
        return null;
    }

}
