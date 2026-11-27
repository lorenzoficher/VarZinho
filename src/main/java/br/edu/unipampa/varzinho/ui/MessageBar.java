package br.edu.unipampa.varzinho.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;

/** The single, persistent destination for operational feedback in the main window. */
final class MessageBar extends JPanel {
    private static final String READY = "Pronto para operar.";
    private final JLabel text = new JLabel(READY);

    MessageBar() {
        super(new BorderLayout());
        putClientProperty("FlatLaf.style", "background: $VarZinho.sidebarBackground; arc: 14");
        text.setForeground(VarZinhoTheme.SIDEBAR_FOREGROUND);
        setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        text.setIcon(VarZinhoIcons.highlights(VarZinhoTheme.ACCENT));
        text.setIconTextGap(10);
        add(text, BorderLayout.CENTER);
    }

    void show(String message) {
        text.setText(message == null || message.isBlank() ? "Não foi possível concluir a operação." : message);
        text.setToolTipText(text.getText());
    }

    String message() {
        return text.getText();
    }

    /** Makes this bar the one that work started anywhere in {@code root}, or in its dialogs, reports to. */
    void serve(javax.swing.JRootPane root) {
        root.putClientProperty(MessageBar.class, this);
    }

    /**
     * Shows what the work started from {@code source} is doing, if a bar serves its window
     * or the window that owns its dialog.
     *
     * @return withdraws the note, giving back the previous message unless something newer
     *     has replaced it meanwhile
     */
    static Runnable busyFor(java.awt.Component source, String progress) {
        java.awt.Window window = source instanceof java.awt.Window own
                ? own : javax.swing.SwingUtilities.getWindowAncestor(source);
        while (window != null) {
            if (window instanceof javax.swing.RootPaneContainer container
                    && container.getRootPane().getClientProperty(MessageBar.class) instanceof MessageBar bar) {
                return bar.busy(progress);
            }
            window = window.getOwner();
        }
        return () -> { };
    }

    private Runnable busy(String progress) {
        String previous = message();
        show(progress);
        return () -> {
            if (progress.equals(message())) show(previous);
        };
    }

    void showStartupWarnings(List<String> warnings) {
        if (warnings.isEmpty()) return;
        String combined = String.join(" ", warnings);
        show(READY.equals(message()) ? combined : combined + " " + message());
    }

    void saved() {
        javax.swing.JRootPane root = javax.swing.SwingUtilities.getRootPane(this);
        if (root == null) return;
        javax.swing.JLayeredPane surface = root.getLayeredPane();
        JLabel toast = new JLabel("✓   Melhor momento salvo com sucesso");
        toast.setOpaque(true);
        toast.setBackground(VarZinhoTheme.SIDEBAR);
        toast.setForeground(VarZinhoTheme.SIDEBAR_FOREGROUND);
        toast.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        java.awt.Dimension size = toast.getPreferredSize();
        toast.setBounds(Math.max(0, surface.getWidth() - size.width - 20),
                Math.max(0, surface.getHeight() - size.height - 58), size.width, size.height);
        surface.add(toast, javax.swing.JLayeredPane.POPUP_LAYER);
        javax.swing.Timer timeout = new javax.swing.Timer(2600, event -> {
            surface.remove(toast);
            surface.repaint(toast.getBounds());
        });
        timeout.setRepeats(false);
        timeout.start();
    }
}
