/*
 * HPBTCW.java
 *
 * Created on 05 octombrie 2008, 19:39
 */
package hpbtc;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
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
    private JPanel panel;
    private JProgressBar progress;
    private JTabbedPane tabbed;
    private JFileChooser fc = new JFileChooser();
    private File filetarget;
    private File filetorrent;

    public HPBTCW() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        tabbed = new JTabbedPane();
        add(tabbed);
        final JMenuBar bar = new JMenuBar();
        final JMenu menu = new JMenu();
        JMenuItem item = new JMenuItem();
        menu.setText("File");
        item.setText("New torrent");
        final JDialog popup = new JDialog();
        popup.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        Container cp = popup.getContentPane();
        cp.setLayout(new GridLayout(3, 3));
        JLabel l = new JLabel();
        l.setText("Torrent");
        popup.add(l);
        final JLabel ltorrentpop = new JLabel();
        popup.add(ltorrentpop);
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
        popup.add(b);
        l = new JLabel();
        l.setText("Target");
        popup.add(l);
        final JLabel ltargetpop = new JLabel();
        popup.add(ltargetpop);
        b = new JButton("...");
        b.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                final int returnVal = fc.showOpenDialog(HPBTCW.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    filetarget = fc.getSelectedFile();
                    ltargetpop.setText(filetarget.getAbsolutePath());
                }
            }
        });
        popup.add(b);
        JButton button = new JButton("OK");
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                popup.dispose();
                panel = new JPanel();
                panel.setLayout(new GridLayout(4, 2));
                JLabel l = new JLabel();
                l.setText("Torrent " + filetorrent.getAbsolutePath());
                panel.add(l);
                l = new JLabel();
                l.setText("Target " + filetarget.getAbsolutePath());
                panel.add(l);
                l = new JLabel();
                l.setText("Progress");
                panel.add(l);
                progress = new JProgressBar();
                panel.add(progress);
                panel.add(new JSplitPane());
                tabbed.addTab("1", panel);
                pack();
            }
        });
        popup.add(button);
        popup.pack();
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
                final Component c = tabbed.getSelectedComponent();
                c.setVisible(false);
                tabbed.remove(c);
                pack();
            }
        });
        menu.add(item);
        bar.add(menu);
        setJMenuBar(bar);
        pack();
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new HPBTCW().setVisible(true);
            }
        });
    }
}
