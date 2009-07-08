/*
 * Created on 15.10.2008
 */

package hpbtc.desktop;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JComponent;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class GraphComponent extends JComponent {

        private static final long serialVersionUID = -2774542544931440878L;
        private int[] history;
        private short index;
        private long lastValue;
        private Color color;
        private long total;

        public GraphComponent(final int h, final Color c) {
            history = new int[h];
            this.color = c;
        }

        public float getAverage() {
            return total / history.length;
        }
        
        public void pushValueToHistory(final long value) {
            if (++index == history.length) {
                index = 0;
            }
            total -= history[index];
            history[index] = (int) (value - lastValue);
            lastValue = value;
            total += history[index];
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
            float kw = (float) d.width / history.length;
            int jc = index == history.length - 1 ? 0 : index + 1;
            int jf = jc == history.length - 1 ? 0 : jc + 1;
            g2d.setColor(color);
            for (int i = 0; i < history.length - 1; i++) {
                int x1 = Math.round(kw * i);
                int x2 = Math.round(kw * (i + 1));
                g2d.fillPolygon(new int[] {x1, x1, x2, x2},
                    new int[] {d.height - 1,
                        Math.round(d.height - (history[jc] - min) * k - 1),
                        Math.round(d.height - (history[jf] - min) * k - 1),
                        d.height - 1}, 4);
                jc = jf;
                jf = jc == history.length - 1 ? 0 : jc + 1;
            }
            g2d.setColor(Color.BLACK);
            g2d.drawString(DesktopUtil.getRepresentation(max), 0, 9);
            g2d.drawString(DesktopUtil.getRepresentation(min), 0, d.height - 1);
        }
}
