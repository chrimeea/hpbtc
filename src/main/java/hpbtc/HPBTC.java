/*
 * Created on Jan 18, 2006
 *
 */
package hpbtc;

import hpbtc.desktop.HPBTCW;
import hpbtc.protocol.processor.Client;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Logger;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.help.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

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
    CommandLineParser parser = new DefaultParser();
    Options options = new Options();
    options.addOption(Option.builder("h")
        .longOpt("help")
        .desc("Show help")
        .get());
    options.addOption(Option.builder("l")
        .longOpt("log")
        .hasArg()
        .desc("Log file")
        .get());
    options.addOption(Option.builder("p")
        .longOpt("port")
        .hasArg()
        .desc("Port number")
        .get());
    options.addOption(Option.builder("t")
        .longOpt("torrent")
        .hasArg()
        .desc("Torrent file")
        .get());
    options.addOption(Option.builder("f")
        .longOpt("folder")
        .hasArg()
        .desc("Output folder")
        .get());
    options.addOption(Option.builder("c")
        .longOpt("cmd")
        .desc("Run in commmand line")
        .get());
    HelpFormatter helpFormatter = HelpFormatter.builder().get();
    try {
      CommandLine cli = parser.parse(options, args);
      if (cli.hasOption("help")) {
        helpFormatter.printHelp("java HPBTC", null, options, null, false);
      }
      String logArg = cli.getOptionValue("log");
      final Handler fh = logArg != null ? new FileHandler(logArg) : new ConsoleHandler();
      final Logger l = Logger.getLogger("hpbtc");
      l.setUseParentHandlers(false);
      l.addHandler(fh);
      l.setLevel(logArg != null ? Level.ALL : Level.INFO);
      final String port = cli.getOptionValue("port");
      final String tor = cli.getOptionValue("torrent");
      final String target = Objects.requireNonNullElse(cli.getOptionValue("folder"), ".");
      if (!cli.hasOption("cmd")) {
        EventQueue.invokeLater(new Runnable() {

          public void run() {
            try {
              HPBTCW h = new HPBTCW(port == null ? null : Integer.parseInt(
                  port));
              h.setVisible(true);
              if (tor != null) {
                h.startTorrent(new File(tor), new File(target));
              }
            } catch (IOException | NoSuchAlgorithmException ex) {
              logger.log(Level.SEVERE, null, ex);
            }
          }
        });
      } else if (tor == null) {
        logger.severe("Mandatory parameter missing");
      } else {
        final Client protocol = new Client();
        protocol.startProtocol(port == null ? null : Integer.parseInt(port));
        final FileInputStream fis = new FileInputStream(tor);
        protocol.download(fis, target);
        fis.close();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

          public void run() {
            protocol.stopProtocol();
          }
        }, "Shutdown"));
      }
    } catch (ParseException e) {
      logger.log(Level.SEVERE, "Wrong arguments", e);
    }
  }
}
