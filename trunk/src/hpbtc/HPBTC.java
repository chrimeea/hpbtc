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
import java.util.logging.ConsoleHandler;
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
        final Handler fh =
                args.length == 3 ? new FileHandler(args[2]) : new ConsoleHandler();
        fh.setFormatter(new SimpleFormatter());
        final Logger l = Logger.getLogger("hpbtc");
        l.addHandler(fh);
        l.setLevel(args.length == 3 ? Level.ALL : Level.INFO);
        if (args.length < 2) {
            logger.severe("Mandatory parameter missing");
        } else {
            final Client protocol = new Client();
            if (args.length == 4) {
                protocol.startProtocol(Integer.parseInt(args[3]));
            } else {
                protocol.startProtocol();
            }
            final FileInputStream fis = new FileInputStream(args[0]);
            protocol.download(fis, args[1]);
            fis.close();
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

                public void run() {
                    protocol.stopProtocol();
                }
            }, "Shutdown"));
        }
    }
}
