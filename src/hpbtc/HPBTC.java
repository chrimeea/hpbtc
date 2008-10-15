/*
 * Created on Jan 18, 2006
 *
 */
package hpbtc;

import hpbtc.desktop.HPBTCW;
import hpbtc.processor.Client;
import java.awt.EventQueue;
import java.io.File;
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
    public static void main(final String[] args) throws IOException,
            NoSuchAlgorithmException {
        String arg = getArg(args, "-log");
        final Handler fh =
                arg != null ? new FileHandler(arg) : new ConsoleHandler();
        fh.setFormatter(new SimpleFormatter());
        final Logger l = Logger.getLogger("hpbtc");
        l.addHandler(fh);
        l.setLevel(arg != null ? Level.ALL : Level.INFO);
        final String port = getArg(args, "-port");
        arg = getArg(args, "-cmd");
        final String tor = getArg(args, "-torrent");
        final String target = getArg(args, "-target");
        if (arg == null) {
            EventQueue.invokeLater(new Runnable() {

                public void run() {
                    try {
                        HPBTCW h = port != null ? new HPBTCW(Integer.parseInt(
                                port)) : new hpbtc.desktop.HPBTCW();
                        h.setVisible(true);
                        if (tor != null && target != null) {
                            h.startTorrent(new File(tor), new File(target));
                        }
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                }
            });
        } else if (tor == null || target == null) {
            logger.severe("Mandatory parameter missing");
        } else {
            final Client protocol = new Client();
            if (port != null) {
                protocol.startProtocol(Integer.parseInt(port));
            } else {
                protocol.startProtocol();
            }
            final FileInputStream fis = new FileInputStream(tor);
            protocol.download(fis, target);
            fis.close();
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

                public void run() {
                    protocol.stopProtocol();
                }
            }, "Shutdown"));
        }
    }

    private static String getArg(String[] args, String prefix) {
        for (String a : args) {
            if (a.startsWith(prefix)) {
                return a.substring(prefix.length());
            }
        }
        return null;
    }
}
