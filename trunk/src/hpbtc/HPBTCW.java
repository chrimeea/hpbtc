/*
 * HPBTCW.java
 *
 * Created on 05 octombrie 2008, 19:39
 */
package hpbtc;

import hpbtc.processor.Client;
import hpbtc.protocol.torrent.Torrent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

/**
 *
 * @author  Cristian Mocanu <chrimeea@yahoo.com>
 */
public class HPBTCW extends JFrame {

    private static final long serialVersionUID = 6534802684324378898L;
    private static Logger logger = Logger.getLogger(HPBTCW.class.getName());
    private JTabbedPane tabbed;
    private JFileChooser fc = new JFileChooser();
    private List<JProgressBar> progress = new LinkedList<JProgressBar>();
    private List<Torrent> torrents = new LinkedList<Torrent>();
    private List<GraphComponent> upload = new LinkedList<GraphComponent>();
    private List<GraphComponent> download = new LinkedList<GraphComponent>();
    private File filetarget;
    private File filetorrent;
    private Timer timer = new Timer();
    private Client client;

    public HPBTCW() throws UnsupportedEncodingException, IOException {
        final JDialog popup = new JDialog();
        popup.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        popup.getContentPane().setLayout(new GridBagLayout());
        JLabel l = new JLabel("Torrent");
        GridBagConstraints c = new GridBagConstraints();
        c.weighty = 0.3;
        popup.add(l, c);
        final JLabel ltorrentpop = new JLabel();
        c.weightx = 1;
        c.gridx = 1;
        popup.add(ltorrentpop, c);
        JButton b = new JButton("...");
        b.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                final int returnVal = fc.showOpenDialog(HPBTCW.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    filetorrent = fc.getSelectedFile();
                    ltorrentpop.setText(filetorrent.getAbsolutePath());
                }
            }
        });
        c.weightx = 0;
        c.gridx = 2;
        popup.add(b, c);
        c.gridx = 0;
        c.gridy = 1;
        l = new JLabel("Target");
        popup.add(l, c);
        final JLabel ltargetpop = new JLabel();
        c.weightx = 1;
        c.gridx = 1;
        popup.add(ltargetpop, c);
        b = new JButton("...");
        b.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                final int returnVal = fc.showOpenDialog(HPBTCW.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    filetarget = fc.getSelectedFile();
                    ltargetpop.setText("Target " + filetarget.getAbsolutePath());
                }
            }
        });
        c.gridx = 2;
        popup.add(b, c);
        JButton button = new JButton("OK");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                popup.dispose();
                try {
                    FileInputStream fis = new FileInputStream(filetorrent);
                    Torrent t = client.download(fis,
                            filetarget.getAbsolutePath());
                    torrents.add(t);
                    fis.close();
                    JPanel panel = new JPanel();
                    panel.setLayout(new GridBagLayout());
                    JLabel l = new JLabel("Torrent");
                    GridBagConstraints c = new GridBagConstraints();
                    panel.add(l, c);
                    l = new JLabel(filetorrent.getAbsolutePath());
                    c.gridx = 1;
                    c.weightx = 1;
                    panel.add(l, c);
                    l = new JLabel("Target");
                    c.weightx = 0;
                    c.gridx = 0;
                    c.gridy = 1;
                    panel.add(l, c);
                    l = new JLabel(filetarget.getAbsolutePath());
                    c.weightx = 1;
                    c.gridx = 1;
                    panel.add(l, c);
                    l = new JLabel();
                    l.setText("Progress");
                    c.weightx = 0;
                    c.gridx = 0;
                    c.gridy = 2;
                    panel.add(l, c);
                    JProgressBar p = new JProgressBar(0, t.getNrPieces());
                    c.weightx = 1;
                    c.gridx = 1;
                    panel.add(p, c);
                    c.gridwidth = 2;
                    c.weighty = 1;
                    c.gridx = 0;
                    c.gridy = 3;
                    GraphComponent g1 = new GraphComponent(100, Color.BLUE);
                    download.add(g1);
                    GraphComponent g2 = new GraphComponent(100, Color.ORANGE);
                    upload.add(g2);
                    panel.add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                            g1, g2), c);
                    tabbed.addTab("1", panel);
                    pack();
                    progress.add(p);
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        });
        c.gridx = 1;
        c.gridy = 2;
        popup.add(button, c);
        popup.setSize(450, 100);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        tabbed = new JTabbedPane();
        add(tabbed);
        final JMenuBar bar = new JMenuBar();
        final JMenu menu = new JMenu();
        JMenuItem item = new JMenuItem();
        menu.setText("File");
        item.setText("New torrent");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                popup.setVisible(true);
            }
        });
        menu.add(item);
        item = new JMenuItem();
        item.setText("Stop this torrent");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                final int i = tabbed.getSelectedIndex();
                progress.remove(i);
                upload.remove(i);
                download.remove(i);
                final Component c = tabbed.getSelectedComponent();
                c.setVisible(false);
                tabbed.remove(c);
                pack();
                try {
                    client.stopTorrent(torrents.remove(i));
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        });
        menu.add(item);
        bar.add(menu);
        setJMenuBar(bar);
        setSize(400, 200);
        TimerTask tt = new TimerTask() {

            @Override
            public void run() {
                int i = tabbed.getSelectedIndex();
                if (i > -1) {
                    Torrent t = torrents.get(i);
                    progress.get(i).setValue(
                            t.getCompletePieces().cardinality());
                    download.get(i).pushValueToHistory(t.getDownloaded());
                    upload.get(i).pushValueToHistory(t.getUploaded());
                }
            }
        };
        timer.schedule(tt, 2000L, 2000L);
        client = new Client();
        client.startProtocol();
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {
                client.stopProtocol();
            }
        });
    }

    public static void main(String args[]) throws IOException {
        Handler fh =
                args.length == 1 ? new FileHandler(args[0]) : new ConsoleHandler();
        fh.setFormatter(new SimpleFormatter());
        Logger l = Logger.getLogger("hpbtc");
        l.addHandler(fh);
        l.setLevel(args.length == 1 ? Level.ALL : Level.INFO);
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                try {
                    new HPBTCW().setVisible(true);
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    private class GraphComponent extends JComponent {

        private static final long serialVersionUID = -2774542544931440878L;
        private int[] history;
        private short index;
        private long lastValue;
        private Color color;

        public GraphComponent(final int h, final Color c) {
            history = new int[h];
            this.color = c;
        }

        public void pushValueToHistory(final long value) {
            if (++index == history.length) {
                index = 0;
            }
            history[index] = (int) (value - lastValue);            
            lastValue = value;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(history.length * 2, 100);
        }

        @Override
        public void paint(Graphics arg0) {
            super.paint(arg0);
            int max = history[0];
            int min = max;
            for (int i = 1; i < history.length; i++) {
                if (history[i] > max) {
                    max = history[i];
                } else if (history[i] < min) {
                    min = history[i];
                }
            }
            Graphics2D g2d = (Graphics2D) arg0;
            Dimension d = getSize();
            float k = max == min ? 1f : (d.height - 1f) / (max - min);
            int jc = index == history.length - 1 ? 0 : index + 1;
            int jf = jc == history.length - 1 ? 0 : jc + 1;
            g2d.setColor(color);
            for (int i = 0; i < history.length - 1; i++) {
                g2d.drawLine(2 * i, (int) (d.height - (history[jc] - min) * k - 1),
                        2 * (i + 1), (int) (d.height - (history[jf] - min) * k - 1));
                jc = jf;
                jf = jc == history.length - 1? 0 : jc + 1;
            }
            g2d.setColor(Color.BLACK);
            g2d.drawString(getRepresentation(max), 0, 9);
            g2d.drawString(getRepresentation(min), 0, d.height - 1);
        }
        
        private String getRepresentation(final int value) {
            if (value < 1024) {
                return String.valueOf(value);
            } else if (value < 1048576) {
                return String.format("%1$.1fKb", value / 1024f);
            } else {
                return String.format("%1$.1fMb", value / 1048576f);
            }
        }
    }
}
