package br.edu.unipampa.varzinho.ui;

import javax.swing.*;
import java.awt.*;

/** Lets desktop pages reflow to the viewport, with horizontal scrolling below a usable width. */
final class ScrollablePage extends JPanel implements Scrollable {
    private static final int MIN_WIDTH = 560;

    private ScrollablePage(JPanel content) {
        super(new BorderLayout());
        setOpaque(false);
        add(content, BorderLayout.CENTER);
    }

    static JScrollPane around(JPanel content) {
        JScrollPane scroll = new JScrollPane(new ScrollablePage(content));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        return scroll;
    }

    @Override public Dimension getPreferredSize() {
        return new Dimension(MIN_WIDTH, super.getPreferredSize().height);
    }

    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public boolean getScrollableTracksViewportWidth() { return getParent().getWidth() >= MIN_WIDTH; }
    @Override public boolean getScrollableTracksViewportHeight() {
        return getParent().getHeight() >= getPreferredSize().height;
    }
    @Override public int getScrollableUnitIncrement(Rectangle area, int orientation, int direction) { return 18; }
    @Override public int getScrollableBlockIncrement(Rectangle area, int orientation, int direction) {
        int extent = orientation == SwingConstants.VERTICAL ? area.height : area.width;
        return Math.max(18, extent - 18);
    }
}
