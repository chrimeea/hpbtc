/*
 * Created on Jan 18, 2006
 *
 */
package hpbtc;

import hpbtc.processor.Client;
import java.io.FileInputStream;
import java.util.logging.Logger;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

/**
 * @author Cristian Mocanu
 *
 */
public class HPBTC {

    private static Logger logger = Logger.getLogger(HPBTC.class.getName());

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException,
            NoSuchAlgorithmException {
        Handler fh = new FileHandler("D:\\Documents and Settings\\Administrator\\Desktop\\hpbtc.log");
        fh.setFormatter(new SimpleFormatter());
        Logger l = Logger.getLogger("hpbtc");
        l.addHandler(fh);
        l.setLevel(Level.ALL);
        if (args.length < 2) {
            logger.severe("Mandatory parameter missing");
        } else {
            final Client protocol = new Client();
            protocol.startProtocol();
            String d = args[0];
            for (int i = 1; i < args.length; i++) {
                FileInputStream fis = new FileInputStream(args[i]);
                protocol.download(fis, d);
                fis.close();
            }
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

                public void run() {
                    protocol.stopProtocol();
                }
            }, "Shutdown"));
        }
    }
}
