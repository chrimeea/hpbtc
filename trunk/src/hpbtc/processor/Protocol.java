package hpbtc.processor;

import hpbtc.protocol.message.ProtocolMessage;
import hpbtc.protocol.network.Network;
import hpbtc.protocol.network.RawMessage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu
 */
public class Protocol {

    private static Logger logger = Logger.getLogger(Protocol.class.getName());
    
    private Network network;
    private Map<InetSocketAddress, RawMessageProcessor> peers;

    public Protocol() {
        this.network = new Network();
        peers = new HashMap<InetSocketAddress, RawMessageProcessor>();
    }

    public void startProtocol() throws IOException {
        network.connect();
        new Thread(new Runnable() {

            public void run() {
                synchronized (network) {
                    do {
                        do {
                            try {
                                network.wait();
                            } catch (InterruptedException e) {
                            }
                        } while (!network.hasUnreadMessages());
                        RawMessage message = null;
                        try {
                            message = network.takeMessage();
                            process(message);
                        } catch (IOException ioe) {
                            logger.warning(ioe.getLocalizedMessage());
                            try {
                                network.closeConnection(message.getPeer());
                            } catch (IOException e) {
                                logger.warning(e.getLocalizedMessage());
                            }
                        }
                    } while (network.hasUnreadMessages());
                }
            }
        }).start();
    }

    private void process(RawMessage data) throws IOException {
        RawMessageProcessor processor = peers.get(data.getPeer());
        if (processor == null) {
            processor = new RawMessageProcessor(data.getPeer());
            peers.put(data.getPeer(), processor);
        } else if (data.isDisconnect()) {
            peers.remove(processor);
            return;
        }
        processor.process(data.getMessage());
    }
    
    public void disconnectFromPeer(InetSocketAddress address) throws IOException {
        network.closeConnection(address);
    }
    
    public void sendMessage(InetSocketAddress address, ProtocolMessage message) throws IOException {
        network.postMessage(address, message.send());
    }
}
