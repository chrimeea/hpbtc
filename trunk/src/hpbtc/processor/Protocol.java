package hpbtc.processor;

import hpbtc.protocol.network.*;
import hpbtc.protocol.torrent.Peer;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Cristian Mocanu
 */
public class Protocol {

    private Client client;
    private Map<InetSocketAddress, ClientMessageProcessor> peers;

    public Protocol(Client client) {
        this.client = client;
        peers = new HashMap<InetSocketAddress, ClientMessageProcessor>();
    }

    public void startProtocol() {
        new Thread(new Runnable() {

            public void run() {
                synchronized (client) {
                    do {
                        do {
                            try {
                                client.wait();
                            } catch (InterruptedException e) {
                            }
                        } while (!client.hasUnreadMessages());
                        process(client.takeMessage());
                    } while (client.hasUnreadMessages());
                }
            }
        }).start();
    }

    private void process(ClientProtocolMessage data) {
        ClientMessageProcessor processor = peers.get(data.getPeer());
        if (processor == null) {
            processor = new ClientMessageProcessor(client, data.getPeer());
            peers.put(data.getPeer(), processor);
        }
        processor.process(data.getMessage());
    }
    
    public void connect(Peer peer) {
        ClientMessageProcessor processor = new ClientMessageProcessor(client, peer);
        peers.put(peer.getAddress(), processor);
        processor.connect();
    }
}
