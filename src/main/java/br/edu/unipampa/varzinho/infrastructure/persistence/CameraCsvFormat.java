package br.edu.unipampa.varzinho.infrastructure.persistence;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.enums.Resolution;

import java.util.List;

/**
 * How one kind of camera is written to, and read back from, a line of the gym file.
 *
 * <p>Every camera line starts with the same columns — kind, court, id, model,
 * resolution, buffer seconds and status — which {@link CsvGymRepository} handles. A
 * format owns only what follows them: the settings that belong to its kind alone. So a
 * new kind of camera is stored by adding a format, without editing the repository.
 *
 * @param <C> the kind of camera this format stores
 */
public interface CameraCsvFormat<C extends Camera> {

    /** The word in the first column that marks a line of this kind. */
    String kind();

    /** The class of camera this format writes; a camera of any other class is not its own. */
    Class<C> cameraClass();

    /** How many columns of its own a line of this kind carries after the shared ones. */
    int ownColumns();

    /** The kind's own settings, in the order {@link #build} reads them back. */
    List<String> ownFields(C camera);

    /**
     * Builds the camera a line describes, with its own settings already applied.
     *
     * @param ownFields exactly {@link #ownColumns()} values
     * @throws IllegalArgumentException if a value is not a number or the camera refuses it
     */
    C build(String id, String model, Resolution resolution, int bufferSeconds, List<String> ownFields);
}
