package br.edu.unipampa.varzinho.ui;

import javax.swing.JPanel;
import java.awt.*;

/** The court markings scale with the capture card and the clip cover. */
final class PitchPanel extends JPanel {
    private final boolean centered;

    PitchPanel(LayoutManager layout) {
        this(layout, false);
    }

    private PitchPanel(LayoutManager layout, boolean centered) {
        super(layout);
        this.centered = centered;
        setOpaque(false);
    }

    static PitchPanel clipCover() {
        return new PitchPanel(new GridBagLayout(), true);
    }

    Rectangle fieldBounds() {
        int w = getWidth(), h = getHeight();
        if (centered) {
            int height = Math.max(0, Math.min(h - 32, (w - 32) / 2));
            int width = 2 * height;
            return new Rectangle((w - width) / 2, (h - height) / 2, width, height);
        }
        int left = w / 3;
        return new Rectangle(left, 14, w - left - 14, h - 28);
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            int w = getWidth(), h = getHeight();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setPaint(new GradientPaint(0, 0, VarZinhoTheme.SIDEBAR, w, h, new Color(15, 77, 66)));
            g.fillRoundRect(0, 0, w, h, 24, 24);
            Rectangle field = fieldBounds();
            int x = field.x, y = field.y, width = field.width, height = field.height;
            g.setColor(new Color(255, 255, 255, 38));
            g.drawRoundRect(x, y, width, height, 8, 8);
            int middle = x + width / 2;
            g.drawLine(middle, y, middle, y + height);
            g.drawOval(middle - 24, h / 2 - 24, 48, 48);
            int boxHeight = centered ? height / 3 : h / 3;
            int boxTop = (h - boxHeight) / 2;
            g.drawRect(x, boxTop, width / 12, boxHeight);
            g.drawRect(x + width - width / 12, boxTop, width / 12, boxHeight);
        } finally { g.dispose(); }
    }
}
