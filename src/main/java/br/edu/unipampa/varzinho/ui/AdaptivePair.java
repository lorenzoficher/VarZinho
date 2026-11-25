package br.edu.unipampa.varzinho.ui;

import net.miginfocom.swing.MigLayout;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;

/**
 * Keeps the same controls and keyboard focus when two sections need to stack.
 *
 * <p>The sections stack below the breakpoint, and also whenever they would not fit side by side:
 * a fixed breakpoint alone goes stale as soon as a label or a control grows.
 */
final class AdaptivePair extends JPanel {
    private static final int GAP = 12;
    private final int breakpoint;
    private final String wideColumns;
    private boolean stacked;
    /** Measured while side by side, since the stacked layout cannot tell what side by side needs. */
    private int sideBySideMinimumWidth;

    AdaptivePair(int breakpoint, String wideColumns) {
        super(new MigLayout("insets 0, fillx, gap " + GAP + ", wrap 2", wideColumns));
        this.breakpoint = breakpoint;
        this.wideColumns = wideColumns;
        setOpaque(false);
    }

    /** Stacks only when the two sections do not fit side by side. */
    AdaptivePair(String wideColumns) {
        this(0, wideColumns);
    }

    /**
     * Reports the stacked width as the minimum: a parent that believed the side-by-side minimum
     * would never hand this pair the narrow width that makes it stack.
     */
    @Override public Dimension getMinimumSize() {
        Dimension minimum = super.getMinimumSize();
        if (isMinimumSizeSet()) return minimum;
        int widestSection = 0;
        for (Component section : getComponents()) {
            if (section.isVisible()) widestSection = Math.max(widestSection, section.getMinimumSize().width);
        }
        return new Dimension(Math.min(minimum.width, widestSection), minimum.height);
    }

    @Override public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (!stacked) sideBySideMinimumWidth = sideBySideMinimumWidth();
        boolean compact = width > 0 && (width < breakpoint || width < sideBySideMinimumWidth);
        if (compact == stacked) return;
        stacked = compact;
        MigLayout layout = (MigLayout) getLayout();
        layout.setColumnConstraints(compact ? "[0:pref,grow,fill]" : wideColumns);
        layout.setLayoutConstraints("insets 0, fillx, gap " + GAP + ", wrap " + (compact ? 1 : 2));
        // Stacking changes preferred height even when the surrounding viewport keeps its size.
        for (java.awt.Container ancestor = this; ancestor != null; ancestor = ancestor.getParent()) {
            ancestor.invalidate();
            if (ancestor instanceof javax.swing.JViewport) break;
        }
        revalidate();
    }

    /**
     * The sections' own minimum widths side by side. The layout's minimum is not enough: a
     * {@code 0:} column reports zero there, yet the layout never shrinks a section below its minimum.
     */
    private int sideBySideMinimumWidth() {
        int sections = 0;
        int total = 0;
        for (Component section : getComponents()) {
            if (!section.isVisible()) continue;
            total += section.getMinimumSize().width;
            sections++;
        }
        return Math.max(getLayout().minimumLayoutSize(this).width, total + GAP * Math.max(0, sections - 1));
    }
}
