/*
 * Created on 15.10.2008
 */
package hpbtc.desktop;

import hpbtc.protocol.processor.Client;
import hpbtc.protocol.torrent.Torrent;
import java.awt.Color;
import java.awt.Component;
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
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class HPBTCW extends JFrame {

    private static final long serialVersionUID = 6534802684324378898L;
    private static Logger logger = Logger.getLogger(HPBTCW.class.getName());
    private static ResourceBundle rb =
            ResourceBundle.getBundle("hpbtc/desktop/hpbtc");
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
    private JMenuItem stopTorrent;

    private void initComponents() {
        setTitle(rb.getString("title.main"));
        final JDialog popup = new JDialog();
        popup.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        popup.getContentPane().setLayout(new GridBagLayout());
        popup.setTitle(rb.getString("title.popup"));
        JLabel l = new JLabel(rb.getString("label.torrent"));
        GridBagConstraints c = new GridBagConstraints();
        popup.add(l, c);
        final JLabel ltorrentpop = new JLabel();
        c.weightx = 1;
        c.gridx = 1;
        popup.add(ltorrentpop, c);
        final JButton button = new JButton(rb.getString("label.ok"));
        JButton b = new JButton(rb.getString("label.browse"));
        b.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                final int returnVal = fc.showOpenDialog(HPBTCW.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    filetorrent = fc.getSelectedFile();
                    ltorrentpop.setText(filetorrent.getAbsolutePath());
                    if (filetarget != null) {
                        button.setEnabled(true);
                    }
                }
            }
        });
        c.weightx = 0;
        c.gridx = 2;
        popup.add(b, c);
        c.gridx = 0;
        c.gridy = 1;
        l = new JLabel(rb.getString("label.target"));
        popup.add(l, c);
        final JLabel ltargetpop = new JLabel();
        c.weightx = 1;
        c.gridx = 1;
        popup.add(ltargetpop, c);
        b = new JButton(rb.getString("label.browse"));
        b.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                final int returnVal = fc.showOpenDialog(HPBTCW.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    filetarget = fc.getSelectedFile();
                    ltargetpop.setText(filetarget.getAbsolutePath());
                    if (filetorrent != null) {
                        button.setEnabled(true);
                    }
                }
            }
        });
        c.weightx = 0;
        c.gridx = 2;
        popup.add(b, c);
        button.setEnabled(false);
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                popup.dispose();
                try {
                    startTorrent(filetorrent, filetarget);
                    filetorrent = null;
                    ltorrentpop.setText("");
                    filetarget = null;
                    ltargetpop.setText("");
                    button.setEnabled(false);
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        });
        c.gridx = 1;
        c.gridy = 2;
        popup.add(button, c);
        popup.setSize(450, 120);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        tabbed = new JTabbedPane();
        add(tabbed);
        final JMenuBar bar = new JMenuBar();
        final JMenu menu = new JMenu(rb.getString("menu.file"));
        JMenuItem item = new JMenuItem(rb.getString("menu.new"));
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                popup.setVisible(true);
            }
        });
        menu.add(item);
        stopTorrent = new JMenuItem(rb.getString("menu.stop"));
        stopTorrent.setEnabled(false);
        stopTorrent.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                final int i = tabbed.getSelectedIndex();
                progress.remove(i);
                upload.remove(i);
                download.remove(i);
                final Component c = tabbed.getSelectedComponent();
                tabbed.remove(c);
                if (tabbed.getTabCount() == 0) {
                    stopTorrent.setEnabled(false);
                }
                try {
                    client.stopTorrent(torrents.remove(i));
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        });
        menu.add(stopTorrent);
        menu.addSeparator();
        item = new JMenuItem(rb.getString("menu.about"));
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                JOptionPane.showMessageDialog(HPBTCW.this,
                        rb.getString("label.about"), rb.getString("title.about"),
                        JOptionPane.INFORMATION_MESSAGE);
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
                Torrent t;
                if (i > -1) {
                    t = torrents.get(i);
                    progress.get(i).setValue(
                            t.getCompletePieces().cardinality());
                    download.get(i).pushValueToHistory(t.getDownloaded());
                    upload.get(i).pushValueToHistory(t.getUploaded());
                }
                for (int j = 0; j < tabbed.getTabCount(); j++) {
                    t = torrents.get(j);
                    if (t.countRemainingPieces() == 0) {
                        tabbed.setTitleAt(j, rb.getString("label.seed"));
                    } else {
                        tabbed.setTitleAt(j, DesktopUtil.getETA(
                                t.getFileLength(),
                                download.get(j).getAverage() / 2f));
                    }
                }
            }
        };
        timer.schedule(tt, 2000L, 2000L);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {
                client.stopProtocol();
            }
        });
    }

    public HPBTCW() throws UnsupportedEncodingException, IOException {
        initComponents();
        client = new Client();
        client.startProtocol();
    }

    public HPBTCW(final int port) throws UnsupportedEncodingException,
            IOException {
        initComponents();
        client = new Client();
        client.startProtocol(port);
    }

    public void startTorrent(final File ftorrent, final File ftarget)
            throws IOException, NoSuchAlgorithmException {
        FileInputStream fis = new FileInputStream(ftorrent);
        Torrent t = client.download(fis, ftarget.getAbsolutePath());
        fis.close();
        if (!torrents.contains(t)) {
            stopTorrent.setEnabled(true);
            torrents.add(t);
            JPanel panel = new JPanel();
            panel.setLayout(new GridBagLayout());
            JLabel l = new JLabel(rb.getString("label.torrent"));
            GridBagConstraints c = new GridBagConstraints();
            panel.add(l, c);
            l = new JLabel(ftorrent.getAbsolutePath());
            c.gridx = 1;
            c.weightx = 1;
            panel.add(l, c);
            l = new JLabel(rb.getString("label.target"));
            c.weightx = 0;
            c.gridx = 0;
            c.gridy = 1;
            panel.add(l, c);
            l = new JLabel(ftarget.getAbsolutePath());
            c.weightx = 1;
            c.gridx = 1;
            panel.add(l, c);
            l = new JLabel();
            l.setText(rb.getString("label.progress"));
            c.weightx = 0;
            c.gridx = 0;
            c.gridy = 2;
            panel.add(l, c);
            JProgressBar p = new JProgressBar(0, t.getNrPieces());
            p.setStringPainted(true);
            c.weightx = 1;
            c.gridx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            panel.add(p, c);
            c.gridwidth = 2;
            c.weighty = 1;
            c.gridx = 0;
            c.gridy = 3;
            c.fill = GridBagConstraints.BOTH;
            GraphComponent g1 = new GraphComponent(100, Color.BLUE);
            download.add(g1);
            GraphComponent g2 =
                    new GraphComponent(100, Color.ORANGE);
            upload.add(g2);
            JSplitPane jsp = new JSplitPane(
                    JSplitPane.HORIZONTAL_SPLIT, g1, g2);
            jsp.setResizeWeight(0.5f);
            panel.add(jsp, c);
            tabbed.addTab(rb.getString("label.eta"), panel);
            pack();
            progress.add(p);
        }
    }
}
