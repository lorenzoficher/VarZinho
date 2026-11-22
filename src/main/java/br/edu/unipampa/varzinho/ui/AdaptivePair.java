package br.edu.unipampa.varzinho.ui;

import net.miginfocom.swing.MigLayout;
import javax.swing.JPanel;

/** Keeps the same controls and keyboard focus when two sections need to stack. */
final class AdaptivePair extends JPanel {
    private final int breakpoint;
    private final String wideColumns;
    private boolean stacked;

    AdaptivePair(int breakpoint, String wideColumns) {
        super(new MigLayout("insets 0, fillx, gap 12, wrap 2", wideColumns));
        this.breakpoint = breakpoint;
        this.wideColumns = wideColumns;
        setOpaque(false);
    }

    @Override public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        boolean compact = width > 0 && width < breakpoint;
        if (compact == stacked) return;
        stacked = compact;
        MigLayout layout = (MigLayout) getLayout();
        layout.setColumnConstraints(compact ? "[0:pref,grow,fill]" : wideColumns);
        layout.setLayoutConstraints("insets 0, fillx, gap 12, wrap " + (compact ? 1 : 2));
        // Stacking changes preferred height even when the surrounding viewport keeps its size.
        for (java.awt.Container ancestor = this; ancestor != null; ancestor = ancestor.getParent()) {
            ancestor.invalidate();
            if (ancestor instanceof javax.swing.JViewport) break;
        }
        revalidate();
    }
}
