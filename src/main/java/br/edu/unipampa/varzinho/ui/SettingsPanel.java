package br.edu.unipampa.varzinho.ui;

import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

/** Operator-controlled visual preferences that apply immediately to every window. */
final class SettingsPanel extends JPanel {
    SettingsPanel() {
        super(new MigLayout("insets 24, fillx, wrap 1", "[grow,fill]", "[]8[]24[]"));
        VarZinhoTheme.styleCard(this);
        JLabel title = new JLabel("Aparência");
        VarZinhoTheme.styleTitle(title);
        add(title);
        add(new JLabel("Escolha o tema mais confortável para operar durante a partida."));
        add(VarZinhoWindow.buildThemeButton(), "w 220!, h 44!");
    }
}
