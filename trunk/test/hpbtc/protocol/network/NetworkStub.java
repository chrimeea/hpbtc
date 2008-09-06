package hpbtc.protocol.network;

import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.network.Network;
import hpbtc.protocol.network.RawMessage;
import hpbtc.protocol.torrent.Peer;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Chris
 */
public class NetworkStub implements Network {

    List<RawMessage> posted;

    public NetworkStub() {
        posted = new LinkedList<RawMessage>();
    }
    
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

    public void postMessage(SimpleMessage arg1) {
        RawMessage rm = new RawMessage(arg1.getDestination(),
                arg1.send().array());
        posted.add(rm);
    }

    public RawMessage takeMessage() {
        return posted.remove(0);
    }

}
