/*
 * Created on Jan 18, 2006
 *
 */
package hpbtc;

import hpbtc.processor.Protocol;
import java.io.File;
import java.util.logging.Logger;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
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
        Logger.getLogger("").addHandler(fh);
        if (args.length < 2) {
            logger.severe("Mandatory parameter missing");
        } else {
            final Protocol protocol = new Protocol();
            protocol.startProtocol();
            String d = args[0];
            for (int i = 1; i < args.length; i++) {
                protocol.download(new File(args[i]), d);
            }
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

                public void run() {
                    protocol.stopProtocol();
                }
            }, "Shutdown"));
        }
    }
}
