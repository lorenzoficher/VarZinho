package br.edu.unipampa.varzinho.ui;

import javax.swing.JPanel;
import java.awt.*;

/** The court markings scale with the capture card and the clip cover. */
final class PitchPanel extends JPanel {
    PitchPanel(LayoutManager layout) { super(layout); setOpaque(false); }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            int w = getWidth(), h = getHeight();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, VarZinhoTheme.SIDEBAR, w, h, new Color(15, 77, 66)));
            g.fillRoundRect(0, 0, w, h, 24, 24);
            int x = w / 3, y = 14, width = w - x - 14, height = h - 28;
            g.setColor(new Color(255, 255, 255, 38));
            g.drawRoundRect(x, y, width, height, 8, 8);
            int middle = x + width / 2;
            g.drawLine(middle, y, middle, y + height);
            g.drawOval(middle - 24, h / 2 - 24, 48, 48);
            g.drawRect(x, h / 3, width / 12, h / 3);
            g.drawRect(x + width - width / 12, h / 3, width / 12, h / 3);
        } finally { g.dispose(); }
    }
}
