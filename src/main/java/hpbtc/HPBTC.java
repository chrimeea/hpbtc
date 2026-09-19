/*
 * Created on Jan 18, 2006
 *
 */
package hpbtc;

import hpbtc.desktop.HPBTCW;
import hpbtc.protocol.processor.Client;
import hpbtc.util.CLIParser;

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
        CLIParser p = new CLIParser(args);
        String logArg = p.getArgValue("--log");
        final Handler fh =
                logArg != null ? new FileHandler(logArg) : new ConsoleHandler();
        final Logger l = Logger.getLogger("hpbtc");
        l.setUseParentHandlers(false);
        l.addHandler(fh);
        l.setLevel(logArg != null ? Level.ALL : Level.INFO);
        if (p.hasArg("--help")) {
          logger.info(String.format("Usage: java HPBTC [--cmd][--port 1234][--torrent path][--target path][--help]"));
        }
        final String port = p.getArgValue("--port");
        final String tor = p.getArgValue("--torrent");
        final String target = p.getArgValue("--target");
        if (!p.hasArg("--cmd")) {
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
}
