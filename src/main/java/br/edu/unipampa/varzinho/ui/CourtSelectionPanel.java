package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CourtView;
import br.edu.unipampa.varzinho.application.ManageGym;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Chooses the current court and opens a new one. */
final class CourtSelectionPanel extends JPanel {
    private final ManageGym management;
    private final Consumer<String> errorSink;
    private final JComboBox<Integer> courtSelector = new JComboBox<>();
    private final JTextField courtNumberField = new JTextField(4);
    private final JButton addCourt = new JButton("Adicionar quadra");
    private final List<Runnable> courtAddedListeners = new ArrayList<>();

    CourtSelectionPanel(ManageGym management, Consumer<String> errorSink) {
        super(new BorderLayout(12, 0));
        if (management == null) throw new IllegalArgumentException("court selection needs the gym's management");
        if (management.courts().isEmpty()) throw new IllegalArgumentException("court selection needs a gym with a court");
        if (errorSink == null) throw new IllegalArgumentException("court selection needs somewhere to report errors");
        this.management = management;
        this.errorSink = errorSink;
        VarZinhoTheme.styleCard(this);
        for (CourtView court : management.courts()) courtSelector.addItem(court.getNumber());
        JPanel adder = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        adder.setOpaque(false);
        adder.add(new JLabel("Número da nova quadra"));
        adder.add(courtNumberField);
        adder.add(addCourt);
        add(courtSelector, BorderLayout.CENTER);
        add(adder, BorderLayout.EAST);
        courtSelector.setPreferredSize(new java.awt.Dimension(240, 38));
        addCourt.setPreferredSize(new java.awt.Dimension(150, 38));
        addCourt.addActionListener(event -> addCourt());
    }

    int selectedCourtNumber() { return (Integer) courtSelector.getSelectedItem(); }

    void selectCourt(int number) {
        boolean added = ((javax.swing.DefaultComboBoxModel<Integer>) courtSelector.getModel()).getIndexOf(number) < 0
                && management.courts().stream().anyMatch(court -> court.getNumber() == number);
        if (added) insertInOrder(number);
        courtSelector.setSelectedItem(number);
        if (added) courtAddedListeners.forEach(Runnable::run);
    }

    void onCourtChange(Runnable listener) { courtSelector.addActionListener(event -> listener.run()); }

    void onCourtAdded(Runnable listener) { courtAddedListeners.add(listener); }

    JTextField courtNumberField() { return courtNumberField; }

    JButton addCourtButton() { return addCourt; }

    private void addCourt() {
        int number;
        try {
            number = Integer.parseInt(courtNumberField.getText().trim());
        } catch (NumberFormatException notANumber) {
            errorSink.accept("Digite o número da quadra usando algarismos.");
            return;
        }
        if (!GymChangeReporter.attempt(() -> management.addCourt(number),
                errorSink)) return;
        insertInOrder(number);
        courtSelector.setSelectedItem(number);
        courtNumberField.setText("");
        courtAddedListeners.forEach(Runnable::run);
    }

    private void insertInOrder(int number) {
        int position = 0;
        while (position < courtSelector.getItemCount() && courtSelector.getItemAt(position) < number) position++;
        courtSelector.insertItemAt(number, position);
    }
}
