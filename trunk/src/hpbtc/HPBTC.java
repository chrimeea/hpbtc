/*
 * Created on Jan 18, 2006
 *
 */
package hpbtc;

import hpbtc.protocol.network.Client;
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
        if (args.length != 1) {
            logger.severe("Mandatory parameter missing");
        } else {
            String name = args[0];
            if (!name.endsWith(".torrent")) {
                name += ".torrent";
            }
            FileInputStream fis = new FileInputStream(name);
            TorrentInfo ti = new TorrentInfo(fis);
            fis.close();
            final Client client = new Client();
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

                public void run() {
                //TODO: stop download
                }
                }, "Shutdown"));
            client.connect();
        }
    }
}
