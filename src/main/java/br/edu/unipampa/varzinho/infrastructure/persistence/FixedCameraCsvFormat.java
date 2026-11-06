package br.edu.unipampa.varzinho.infrastructure.persistence;

import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.enums.Resolution;

import java.util.List;

/** A fixed camera's line carries the one setting it has: its angle. */
public final class FixedCameraCsvFormat implements CameraCsvFormat<FixedCamera> {

    @Override
    public String kind() {
        return "fixed";
    }

    @Override
    public Class<FixedCamera> cameraClass() {
        return FixedCamera.class;
    }

    @Override
    public int ownColumns() {
        return 1;
    }

    @Override
    public List<String> ownFields(FixedCamera camera) {
        return List.of(String.valueOf(camera.getAngle()));
    }

    @Override
    public FixedCamera build(String id, String model, Resolution resolution, int bufferSeconds,
                             List<String> ownFields) {
        return new FixedCamera(id, model, resolution, bufferSeconds, Integer.parseInt(ownFields.get(0)));
    }
}
