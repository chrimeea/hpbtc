/*
 * HPBTCW.java
 *
 * Created on 05 octombrie 2008, 19:39
 */
package hpbtc;

import java.awt.EventQueue;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 *
 * @author  Cristian Mocanu <chrimeea@yahoo.com>
 */
public class HPBTCW {

    private static Logger logger = Logger.getLogger(HPBTCW.class.getName());
    
    public static void main(final String args[]) throws IOException {
        final Handler fh =
                args.length == 1 ? new FileHandler(args[0]) : new ConsoleHandler();
        fh.setFormatter(new SimpleFormatter());
        final Logger l = Logger.getLogger("hpbtc");
        l.addHandler(fh);
        l.setLevel(args.length == 1 ? Level.ALL : Level.INFO);
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                try {
                    hpbtc.desktop.HPBTCW h = args.length == 2 ?
                        new hpbtc.desktop.HPBTCW(Integer.parseInt(args[1])) :
                        new hpbtc.desktop.HPBTCW();
                    h.setVisible(true);
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        });
    }
}
