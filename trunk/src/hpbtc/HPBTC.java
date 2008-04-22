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
    public static void main(String[] args) {
        if (args.length != 1) {
            logger.severe("Mandatory parameter missing");
        } else {
            String name = args[0];
            if (!name.endsWith(".torrent")) {
                name += ".torrent";
            }
            TorrentInfo ti = new TorrentInfo(name);
            final Client client = new Client(ti.getInfoHash());
            try {
                client.connect();
                Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                    /* (non-Javadoc)
                     * @see java.lang.Runnable#run()
                     */
                    public void run() {
                        client.getDownloadItem().stopDownload();
                    }
                }, "Shutdown"));
            } catch (IllegalStateException e) {
                logger.warning("Can not register shutdown hook " + e.getMessage());
            } catch (IOException e) {
                logger.severe("Can not start server " + e.getMessage());
                return;
            }
            client.listen();
        }
    }
}
