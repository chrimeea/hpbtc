package hpbtc.processor;

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
    
    private Network client;
    private Map<InetSocketAddress, RawMessageProcessor> peers;

    public Protocol(Network client) {
        this.client = client;
        peers = new HashMap<InetSocketAddress, RawMessageProcessor>();
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
                        RawMessage message = null;
                        try {
                            message = client.takeMessage();
                            process(message);
                        } catch (IOException ioe) {
                            logger.warning(ioe.getLocalizedMessage());
                            try {
                                client.closeConnection(message.getPeer());
                            } catch (IOException e) {
                                logger.warning(e.getLocalizedMessage());
                            }
                        }
                    } while (client.hasUnreadMessages());
                }
            }
        }).start();
    }

    private void process(RawMessage data) throws IOException {
        RawMessageProcessor processor = peers.get(data.getPeer());
        if (processor == null) {
            processor = new RawMessageProcessor(client, data.getPeer());
            peers.put(data.getPeer(), processor);
        }
        processor.process(data.getMessage());
    }
}
