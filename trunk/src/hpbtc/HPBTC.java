/*
 * Created on Jan 18, 2006
 *
 */
package hpbtc;

import hpbtc.protocol.network.Client;
import hpbtc.protocol.torrent.TorrentInfo;
import java.util.logging.Logger;
import java.io.IOException;

/**
 * @author chris
 *
 */
public class HPBTC {

    private static Logger logger = Logger.getLogger(HPBTC.class.getName());

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            logger.severe("Mandatory parameter missing");
        } else {
            String name = args[0];
            if (!name.endsWith(".torrent")) {
                name += ".torrent";
            }
            TorrentInfo ti = new TorrentInfo(name);
            final Client client = new Client(ti.getInfoHash());
            client.connect();
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

                public void run() {
                //downloadItem().stopDownload();
                }
                }, "Shutdown"));
            client.listen();
        }
    }
}
