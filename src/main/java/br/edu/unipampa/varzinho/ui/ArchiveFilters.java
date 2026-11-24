package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.CourtView;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Search, court and calendar controls for the archive. */
final class ArchiveFilters extends JPanel {
    private static final String ALL_COURTS = "Todas as quadras";
    private static final String ALL_DAYS = "Todos os dias";
    private static final String TODAY = "Hoje";
    private static final String YESTERDAY = "Ontem";
    private static final String CHOSEN_DATE = "Escolher data";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private final Supplier<List<CourtView>> courts;
    private final Clock clock;
    private final ZoneId zone;
    private final List<Integer> courtNumbers = new ArrayList<>();
    private final JComboBox<String> filter = new JComboBox<>();
    private final JComboBox<String> dayFilter = new JComboBox<>(new String[] {ALL_DAYS, TODAY, YESTERDAY, CHOSEN_DATE});
    private final JTextField dateField = new JTextField(8);
    private final JTextField search = new JTextField(12);

    ArchiveFilters(Supplier<List<CourtView>> courts, Clock clock, ZoneId zone, JButton openClip,
                   ArchiveTable recordings, Runnable refresh) {
        super(new BorderLayout());
        this.courts = courts;
        this.clock = clock;
        this.zone = zone;
        search.putClientProperty("JTextField.placeholderText", "Buscar gravação");
        search.putClientProperty("JTextField.showClearButton", true);
        search.setToolTipText("Buscar gravação (Ctrl+F). Enter seleciona o primeiro resultado.");
        search.getAccessibleContext().setAccessibleName("Buscar gravação");
        search.addActionListener(event -> recordings.selectFirst());
        registerKeyboardAction(event -> {
            search.requestFocusInWindow();
            search.selectAll();
        }, KeyStroke.getKeyStroke("ctrl F"), WHEN_IN_FOCUSED_WINDOW);
        search.registerKeyboardAction(event -> search.setText(""), KeyStroke.getKeyStroke("ESCAPE"), WHEN_FOCUSED);
        Runnable applyFilters = () -> recordings.search(search.getText());
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilters.run(); }
            public void removeUpdate(DocumentEvent e) { applyFilters.run(); }
            public void changedUpdate(DocumentEvent e) { applyFilters.run(); }
        });
        filter.addItem(ALL_COURTS);
        refreshCourts();
        filter.addActionListener(event -> refresh.run());
        dateField.setToolTipText("dd/mm/aaaa");
        dateField.setEnabled(false);
        dayFilter.addActionListener(event -> {
            dateField.setEnabled(CHOSEN_DATE.equals(dayFilter.getSelectedItem()));
            // A chosen date starts from today, so the rows shown always match a day the filter names.
            if (dateField.isEnabled() && dateField.getText().isBlank()) {
                dateField.setText(DATE_FORMAT.format(LocalDate.now(clock.withZone(zone))));
            }
            refresh.run();
        });
        dateField.addActionListener(event -> refresh.run());
        AdaptivePair heading = new AdaptivePair(780, "[grow,fill][]");
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        JLabel eyebrow = new JLabel("GRAVAÇÕES RECENTES");
        VarZinhoTheme.styleEyebrow(eyebrow);
        JLabel title = new JLabel("Histórico de melhores momentos");
        VarZinhoTheme.styleTitle(title);
        titles.add(eyebrow);
        titles.add(title);
        heading.add(titles);
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
        filterRow.setOpaque(false);
        filterRow.add(search);
        filterRow.add(filter);
        filterRow.add(new JLabel("Dia:"));
        filterRow.add(dayFilter);
        filterRow.add(dateField);

        filterRow.add(openClip);
        heading.add(filterRow);
        add(heading);
    }

    JComboBox<String> filter() { return filter; }

    JComboBox<String> dayFilter() { return dayFilter; }

    JTextField dateField() { return dateField; }

    JTextField searchField() { return search; }
    /**
     * Offers every court the gym has now as a filter, keeping the current choice. Called
     * when a court is added after the window opened.
     */
    void refreshCourts() {
        courts.get().stream().map(CourtView::getNumber)
                .filter(number -> !courtNumbers.contains(number))
                .forEach(number -> {
                    courtNumbers.add(number);
                    filter.addItem("Quadra " + number);
                });
    }

    Optional<LocalDate> chosenDay() {
        LocalDate today = LocalDate.now(clock.withZone(zone));
        Object chosen = dayFilter.getSelectedItem();
        if (TODAY.equals(chosen)) return Optional.of(today);
        if (YESTERDAY.equals(chosen)) return Optional.of(today.minusDays(1));
        if (CHOSEN_DATE.equals(chosen)) return Optional.of(LocalDate.parse(dateField.getText().trim(), DATE_FORMAT));
        return Optional.empty();
    }
    boolean includesAll() {
        return filter.getSelectedIndex() <= 0 && ALL_DAYS.equals(dayFilter.getSelectedItem());
    }

    Optional<Integer> courtNumber() {
        int index = filter.getSelectedIndex();
        return index <= 0 ? Optional.empty() : Optional.of(courtNumbers.get(index - 1));
    }
}
