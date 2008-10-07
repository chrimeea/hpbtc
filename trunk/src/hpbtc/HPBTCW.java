/*
 * HPBTCW.java
 *
 * Created on 05 octombrie 2008, 19:39
 */
package hpbtc;

import hpbtc.processor.Client;
import hpbtc.protocol.torrent.Torrent;
import java.awt.Component;
import java.awt.EventQueue;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
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
                    popup.pack();
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
                    popup.pack();
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
                    panel.add(new JSplitPane(), c);
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
        popup.pack();
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
        pack();
        TimerTask tt = new TimerTask() {

            @Override
            public void run() {
                int i = tabbed.getSelectedIndex();
                progress.get(i).setValue(
                        torrents.get(i).getCompletePieces().cardinality());
            }
        };
        timer.schedule(tt, 6000L, 6000L);
        client = new Client();
        client.startProtocol();
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {
                client.stopProtocol();
            }
        });
    }

    public static void main(String args[]) {
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
}