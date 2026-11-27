package br.edu.unipampa.varzinho.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * The Swing thread lends the model to one worker at a time: a capture, or a gym change
 * being stored. Feeds skip their ticks meanwhile; a phone drop waits until the worker
 * has returned the model.
 * Every method is called on the Swing thread, never by the worker itself.
 */
final class CaptureActivity {
    private final List<Runnable> waiting = new ArrayList<>();
    private boolean running;

    boolean isRunning() {
        return running;
    }

    void begin() {
        if (running) throw new IllegalStateException("a capture is already running");
        running = true;
    }

    void finish() {
        running = false;
        List<Runnable> ready = List.copyOf(waiting);
        waiting.clear();
        ready.forEach(Runnable::run);
    }

    void ifIdle(Runnable action) {
        if (!running) action.run();
    }

    void whenIdle(Runnable action) {
        if (running) {
            waiting.add(action);
        } else {
            action.run();
        }
    }
}
