/*
 * Created on Jan 18, 2006
 *
 */
package hpbtc;

import hpbtc.processor.Protocol;
import hpbtc.protocol.network.Network;
import hpbtc.protocol.torrent.TorrentInfo;
import java.io.FileInputStream;
import java.util.logging.Logger;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * @author chris
 *
 */
public class HPBTC {

    private static Logger logger = Logger.getLogger(HPBTC.class.getName());

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        if (args.length == 0) {
            logger.severe("Mandatory parameter missing");
        } else {
            final Protocol protocol = new Protocol();
            protocol.startProtocol();
            for (String torrent: args) {
                protocol.download(torrent);
            }
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

                public void run() {
                    protocol.stopProtocol();
                }
                }, "Shutdown"));
        }
    }
}
