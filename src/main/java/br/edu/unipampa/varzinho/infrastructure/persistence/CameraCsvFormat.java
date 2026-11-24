package br.edu.unipampa.varzinho.infrastructure.persistence;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.enums.Resolution;

import java.util.List;

/**
 * The CSV fields specific to one exact camera class, after the seven shared columns.
 *
 * <p>{@link CsvGymRepository} knows only the shared columns; each camera class brings its
 * own format, so a new kind of camera is stored without touching the repository.
 *
 * @param <C> the exact camera class this format writes and reads back
 */
public interface CameraCsvFormat<C extends Camera> {
    /**
     * The word that marks this camera's lines in the file. It must be unique among the
     * registered formats and can be neither {@code gym} nor {@code court}.
     *
     * @return the kind written in the first column of the camera's line
     */
    String kind();

    /**
     * The exact class this format handles. A subclass of it is not handled: it needs a
     * format of its own, or saving it fails.
     *
     * @return the camera class this format writes
     */
    Class<C> cameraClass();

    /**
     * How many columns follow the shared ones on this camera's line. Loading checks every
     * line against it before calling {@link #build}.
     *
     * @return the number of fields {@link #ownFields} returns, never negative
     */
    int ownColumns();

    /**
     * The values specific to this camera, in the order {@link #build} reads them back.
     *
     * @param camera the camera being saved
     * @return exactly {@link #ownColumns()} fields, none containing a comma or a line break
     */
    List<String> ownFields(C camera);

    /**
     * Rebuilds a camera from its stored fields, through the domain's own constructors and
     * behaviour, so a stored value the camera would refuse is refused again here.
     *
     * @param id the camera's identifier
     * @param model the camera's model
     * @param resolution the resolution it records at
     * @param bufferSeconds the length of its circular buffer
     * @param ownFields the fields {@link #ownFields} wrote, already counted
     * @return the rebuilt camera, still switched off
     * @throws IllegalArgumentException if a field is not a value the camera accepts
     */
    C build(String id, String model, Resolution resolution, int bufferSeconds, List<String> ownFields);
}
