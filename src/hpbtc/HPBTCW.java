/*
 * HPBTCW.java
 *
 * Created on 05 octombrie 2008, 19:39
 */
package hpbtc;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
                panel = new JPanel();
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
                progress = new JProgressBar();
                c.weightx = 1;
                c.gridx = 1;
                panel.add(progress, c);
                c.gridwidth = 2;
                c.weighty = 1;
                c.gridx = 0;
                c.gridy = 3;
                panel.add(new JSplitPane(), c);
                tabbed.addTab("1", panel);
                pack();
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
