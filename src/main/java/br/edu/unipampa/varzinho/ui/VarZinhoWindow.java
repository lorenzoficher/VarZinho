package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.application.BrowseArchive;
import br.edu.unipampa.varzinho.application.CaptureHighlight;
import br.edu.unipampa.varzinho.application.Feed;
import br.edu.unipampa.varzinho.application.ManageGym;
import br.edu.unipampa.varzinho.application.ManageRecordings;
import br.edu.unipampa.varzinho.application.SignedInOperator;

import javax.swing.JOptionPane;
import javax.swing.JFrame;
import javax.swing.Timer;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The operator's window: the courts, the capture button, the archive and the operators,
 * kept moving by the feeds it is handed. It reaches the gym, the archive and the
 * operators only through the use cases it is given, and never holds a domain object. It builds none of what it shows —
 * {@link br.edu.unipampa.varzinho.bootstrap.Launcher} does.
 */
public final class VarZinhoWindow extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(VarZinhoWindow.class.getName());
    /** The smallest window every page is laid out for; it still fits a 1366×768 screen with a taskbar. */
    private static final int MINIMUM_WIDTH = 1024;
    private static final int MINIMUM_HEIGHT = 640;
    private static final int PREFERRED_WIDTH = 1460;
    private static final int PREFERRED_HEIGHT = 900;
    private transient final List<Timer> clocks = new ArrayList<>();
    private transient final CaptureActivity captureActivity = new CaptureActivity();
    private transient final MessageBar messages = new MessageBar();
    private transient final ArenaWorkspace workspace;
    private transient boolean phoneDropReported;

    /**
     * Creates the window and starts the feed timers.
     *
     * @param gymName       the gym's display name, shown in the title bar
     * @param operator      the signed-in operator; used by the arena workspace
     * @param management    use case for adding courts and managing cameras
     * @param capture       use case for triggering a capture
     * @param archive       use case for browsing saved highlights
     * @param feed          the clock that drives buffer and camera-status updates;
     *                      must not be {@code null}
     * @param bufferSeconds how many seconds of footage the circular buffer keeps
     * @throws IllegalArgumentException if {@code feed} is {@code null}
     */
    public VarZinhoWindow(String gymName, SignedInOperator operator, ManageGym management,
                          CaptureHighlight capture, BrowseArchive archive, Feed feed, int bufferSeconds) {
        super("VarZinho Arena — " + gymName);
        if (feed == null) throw new IllegalArgumentException("a window needs a feed");
        workspace = new ArenaWorkspace(gymName, operator, management, capture, archive,
                bufferSeconds, captureActivity, messages);
        setContentPane(workspace);
        messages.serve(getRootPane());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        java.awt.Rectangle bounds = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setMinimumSize(minimumSupportedSize());
        setPreferredSize(new java.awt.Dimension(
                Math.max(MINIMUM_WIDTH, Math.min(PREFERRED_WIDTH, bounds.width)),
                Math.max(MINIMUM_HEIGHT, Math.min(PREFERRED_HEIGHT, bounds.height))));
        pack();
        setLocationRelativeTo(null);
        clocks.add(clockFor(feed, captureActivity, this::showBufferStatus, messages));
        clocks.forEach(Timer::start);
    }

    static java.awt.Dimension minimumSupportedSize() {
        return new java.awt.Dimension(MINIMUM_WIDTH, MINIMUM_HEIGHT);
    }

    @Override public void dispose() {
        clocks.forEach(Timer::stop);
        super.dispose();
    }

    /**
     * Shows the warnings collected while starting up, such as records skipped from a corrupted file.
     *
     * @param warnings the messages to show; an empty list shows nothing
     */
    public void showStartupWarnings(List<String> warnings) {
        messages.showStartupWarnings(warnings);
    }

    /**
     * Wires the recording use case so the archive page can rename and delete saved highlights.
     *
     * @param manageRecordings the use case that renames and deletes recordings
     */
    public void enableRecordingEditing(ManageRecordings manageRecordings) {
        workspace.enableRecordingEditing(manageRecordings);
    }

    /**
     * Runs the phone's feed on a clock of its own, shows its court and offers the stream
     * in the browser.
     *
     * @param phoneFeed  the clock that drives the phone's buffer and status updates;
     *                   must not be {@code null}
     * @param phoneCourt the court number to select for the phone's view
     * @param streamUrl  the local-network stream address; must not be {@code null} or blank
     * @param publicUrl  the public viewer link to copy, or {@code null} when there is none
     * @throws IllegalArgumentException if {@code phoneFeed} is {@code null} or
     *                                  {@code streamUrl} is {@code null} or blank
     */
    public void followPhone(Feed phoneFeed, int phoneCourt, String streamUrl, String publicUrl) {
        if (phoneFeed == null) throw new IllegalArgumentException("a followed phone needs its feed");
        if (streamUrl == null || streamUrl.isBlank()) {
            throw new IllegalArgumentException("a followed phone needs its stream address");
        }
        Timer clock = clockFor(phoneFeed, captureActivity, this::showBufferStatus, messages);
        clocks.add(clock);
        clock.start();
        workspace.selectCourt(phoneCourt);
        Runnable copy = publicUrl == null ? null : () -> {
            try {
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                        new java.awt.datatransfer.StringSelection(publicUrl), null);
                messages.show("Link da partida copiado. Envie aos espectadores.");
            } catch (IllegalStateException busy) {
                messages.show("Não foi possível copiar. Abra a transmissão e copie o endereço no navegador.");
            }
        };
        workspace.liveActions(() -> {
            workspace.selectCourt(phoneCourt);
            openLiveView(publicUrl == null ? streamUrl : publicUrl);
        }, copy, publicUrl == null ? "Abrir a câmera na rede local e selecionar a quadra do celular"
                : "Abrir a página dos espectadores e selecionar a quadra do celular");
        messages.show("Vídeo recebido na quadra " + phoneCourt + ". Aguarde os primeiros segundos de gravação antes de salvar o lance. "
                + (publicUrl == null ? "Transmissão disponível na rede local." : "Link da partida disponível para copiar."));
    }

    /**
     * Enables the prompt that lets the operator connect a phone camera and, if an address
     * was stored from a previous session, tries to connect straight away.
     *
     * @param initialAddress the address to try on startup, or {@code null} to skip the auto-connect
     * @param prepareConnection builds the connection runnable for a given stream address
     */
    public void offerPhoneConnection(String initialAddress, java.util.function.Function<String, Runnable> prepareConnection) {
        enablePhonePrompt(initialAddress, prepareConnection);
        if (initialAddress != null && !initialAddress.isBlank()) connectToPhone(initialAddress, prepareConnection);
    }

    private void enablePhonePrompt(String previousAddress, java.util.function.Function<String, Runnable> prepareConnection) {
        workspace.liveActions(() -> {
            String address = (String) JOptionPane.showInputDialog(this,
                    "Cole o endereço do vídeo que abriu no navegador deste computador (termina em /video).",
                    "Conectar câmera do celular", JOptionPane.QUESTION_MESSAGE, null, null, previousAddress);
            if (address == null || address.isBlank()) return;
            connectToPhone(address, prepareConnection);
        }, null, "Conectar o IP Webcam para gravar vídeo real");
    }

    private void connectToPhone(String address, java.util.function.Function<String, Runnable> prepareConnection) {
        workspace.liveActions(null, null, "Conectando à câmera…");
        messages.show("Conectando ao celular e aguardando o primeiro trecho de vídeo…");
        new javax.swing.SwingWorker<Runnable, Void>() {
            @Override protected Runnable doInBackground() { return prepareConnection.apply(address.strip()); }
            @Override protected void done() {
                try {
                    Runnable attach = get();
                    captureActivity.whenIdle(() -> {
                        try { attach.run(); }
                        catch (IllegalStateException conflict) {
                            messages.show(conflict.getMessage());
                            enablePhonePrompt(address, prepareConnection);
                        }
                    });
                } catch (java.util.concurrent.ExecutionException failed) {
                    messages.show(PortugueseText.phoneConnectionFailure(failed.getCause()));
                    enablePhonePrompt(address, prepareConnection);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    enablePhonePrompt(address, prepareConnection);
                }
            }
        }.execute();
    }

    private void openLiveView(String streamUrl) {
        try {
            Desktop.getDesktop().browse(URI.create(streamUrl));
        } catch (IOException | IllegalArgumentException | UnsupportedOperationException failure) {
            // The stream address can carry credentials, even when URI parsing fails.
            LOGGER.log(Level.WARNING, "Could not open live stream: {0}",
                    failure.toString().replace(streamUrl, "[configured stream]"));
            messages.show("Não foi possível abrir a transmissão ao vivo.");
        }
    }

    /**
     * Tells the operator, once, that the phone's court can no longer capture. FFmpeg
     * exiting and the phone going silent are two signs of the same drop.
     *
     * @param disconnect the action that releases the phone's connection resources
     */
    public void phoneDropped(Runnable disconnect) {
        captureActivity.whenIdle(() -> {
            if (phoneDropReported) return;
            disconnect.run();
            workspace.refreshDevices();
            phoneDropReported = true;
            workspace.liveActions(null, null, "A câmera desconectou. Reinicie o VarZinho para reconectar.");
            showBufferStatus();
            messages.show("O celular parou de transmitir. "
                    + "Reinicie o VarZinho para reconectar.");
        });
    }

    static Timer clockFor(Feed feed, CaptureActivity activity, Runnable refresh, MessageBar messages) {
        return new Timer(feed.periodMillis(), new ActionListener() {
            private boolean readFailed;

            @Override
            public void actionPerformed(ActionEvent event) {
                activity.ifIdle(this::tick);
            }

            private void tick() {
                try {
                    feed.tick(Instant.now());
                    readFailed = false;
                    refresh.run();
                } catch (IOException failure) {
                    if (readFailed) return;
                    readFailed = true;
                    LOGGER.log(Level.WARNING, "Could not read footage from the feed", failure);
                    messages.show("Não foi possível ler as imagens do celular.");
                }
            }
        });
    }

    private void showBufferStatus() {
        workspace.refreshStatus();
    }
}
