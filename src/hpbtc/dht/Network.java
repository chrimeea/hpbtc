/*
 * Created on 18.10.2008
 */

package hpbtc.dht;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class Network {

    private static Logger logger = Logger.getLogger(Network.class.getName());
    private DatagramChannel channel;
    private boolean running;
    private Selector selector;

    public Network() {
    }

    public void connect(final int port) throws IOException {
        channel = DatagramChannel.open();
        channel.socket().bind(new InetSocketAddress(
                        InetAddress.getLocalHost(), port));
        startListen();
    }

    public int connect() throws IOException {
        channel = DatagramChannel.open();
        DatagramSocket s = channel.socket();
        s.bind(null);
        startListen();
        return s.getPort();
    }
    
    public void disconnect() {
        running = false;
        selector.wakeup();
    }
    
    private void startListen() {
        running = true;
        new Thread(new Runnable() {

            public void run() {
                try {
                    listen();
                } catch (Exception e) {
                    running = false;
                    logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
                }
                try {
                    selector.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
                }
                try {
                    channel.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
                }
            }
        }).start();
    }
    
    private void listen() throws IOException {
        while (running) {
            if (selector.select() > 0) {
                final Iterator<SelectionKey> i = selector.selectedKeys().
                        iterator();
                while (i.hasNext()) {
                    final SelectionKey key = i.next();
                    i.remove();
                }
            }
        }
    }
}
