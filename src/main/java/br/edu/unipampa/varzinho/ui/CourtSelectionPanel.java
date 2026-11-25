package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CourtView;
import br.edu.unipampa.varzinho.application.ManageGym;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

/** Chooses the current court, opens a new one and removes an empty one. */
final class CourtSelectionPanel extends JPanel {
    private final ManageGym management;
    private final GymChanges changes;
    private final Consumer<String> errorSink;
    private final JComboBox<Integer> courtSelector = new JComboBox<>();
    private final JTextField courtNumberField = new JTextField(4);
    private final JButton addCourt = new JButton("Adicionar quadra");
    private final JButton removeCourt = new JButton("Remover quadra");
    private final List<Runnable> courtAddedListeners = new ArrayList<>();
    private final List<IntConsumer> courtRemovedListeners = new ArrayList<>();

    CourtSelectionPanel(ManageGym management, GymChanges changes, Consumer<String> errorSink) {
        super(new BorderLayout(12, 0));
        if (management == null) throw new IllegalArgumentException("court selection needs the gym's management");
        if (management.courts().isEmpty()) throw new IllegalArgumentException("court selection needs a gym with a court");
        if (changes == null) throw new IllegalArgumentException("court selection needs a way to change the gym");
        if (errorSink == null) throw new IllegalArgumentException("court selection needs somewhere to report errors");
        this.management = management;
        this.changes = changes;
        this.errorSink = errorSink;
        VarZinhoTheme.styleCard(this);
        for (CourtView court : management.courts()) courtSelector.addItem(court.getNumber());
        JPanel adder = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        adder.setOpaque(false);
        adder.add(new JLabel("Número da nova quadra"));
        adder.add(courtNumberField);
        adder.add(addCourt);
        adder.add(removeCourt);
        add(courtSelector, BorderLayout.CENTER);
        add(adder, BorderLayout.EAST);
        courtSelector.setPreferredSize(new java.awt.Dimension(240, 38));
        addCourt.setPreferredSize(new java.awt.Dimension(150, 38));
        addCourt.addActionListener(event -> addCourt());
        EnterKey.submits(addCourt, courtNumberField);
        removeCourt.setPreferredSize(new java.awt.Dimension(150, 38));
        removeCourt.addActionListener(event -> removeSelectedCourt(this::confirmRemoval));
    }

    /**
     * Removes the selected court once the operator confirms. A refusal — the court still
     * holds cameras, or it is the only one — is reported and the court stays selected.
     * The removal itself runs on a worker; the selector follows once it returns.
     */
    void removeSelectedCourt(IntPredicate confirm) {
        int number = selectedCourtNumber();
        if (!confirm.test(number)) return;
        changes.attempt(removeCourt, () -> management.removeCourt(number), errorSink, () -> {
            courtSelector.removeItem(number);
            courtRemovedListeners.forEach(listener -> listener.accept(number));
        });
    }

    void onCourtRemoved(IntConsumer listener) { courtRemovedListeners.add(listener); }

    List<Integer> courtNumbers() {
        List<Integer> numbers = new ArrayList<>();
        for (int index = 0; index < courtSelector.getItemCount(); index++) numbers.add(courtSelector.getItemAt(index));
        return numbers;
    }

    JButton removeCourtButton() { return removeCourt; }

    private boolean confirmRemoval(int number) {
        String prompt = "Remover a quadra " + number + "?\nOs lances já gravados nela continuam no acervo.";
        Object[] options = {"Cancelar", "Remover quadra"};
        return JOptionPane.showOptionDialog(this, prompt, "Confirmar remoção", JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[0]) == 1;
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
        changes.attempt(addCourt, () -> management.addCourt(number), errorSink, () -> {
            insertInOrder(number);
            courtSelector.setSelectedItem(number);
            courtNumberField.setText("");
            courtAddedListeners.forEach(Runnable::run);
        });
    }

    private void insertInOrder(int number) {
        int position = 0;
        while (position < courtSelector.getItemCount() && courtSelector.getItemAt(position) < number) position++;
        courtSelector.insertItemAt(number, position);
    }
}
