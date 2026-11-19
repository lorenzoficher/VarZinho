package br.edu.unipampa.varzinho.infrastructure.persistence;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.enums.Resolution;

import java.util.List;

/** The CSV fields specific to one exact camera class, after the seven shared columns. */
public interface CameraCsvFormat<C extends Camera> {
    String kind();

    Class<C> cameraClass();

    int ownColumns();

    List<String> ownFields(C camera);

    C build(String id, String model, Resolution resolution, int bufferSeconds, List<String> ownFields);
}
