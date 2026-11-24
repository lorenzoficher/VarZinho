package br.edu.unipampa.varzinho.infrastructure.persistence;

import br.edu.unipampa.varzinho.domain.capture.PtzCamera;
import br.edu.unipampa.varzinho.enums.Resolution;

import java.util.List;

/** Rebuilds the aim through moveTo so stored values obey the camera's own rules. */
public final class PtzCameraCsvFormat implements CameraCsvFormat<PtzCamera> {
    /** Creates the format; it keeps no state, so one instance serves every gym. */
    public PtzCameraCsvFormat() { }

    @Override
    public String kind() { return "ptz"; }

    @Override
    public Class<PtzCamera> cameraClass() { return PtzCamera.class; }

    @Override
    public int ownColumns() { return 3; }

    @Override
    public List<String> ownFields(PtzCamera camera) {
        return List.of(String.valueOf(camera.getPan()), String.valueOf(camera.getTilt()),
                String.valueOf(camera.getZoom()));
    }

    @Override
    public PtzCamera build(String id, String model, Resolution resolution, int bufferSeconds,
                          List<String> ownFields) {
        PtzCamera camera = new PtzCamera(id, model, resolution, bufferSeconds);
        camera.moveTo(Integer.parseInt(ownFields.get(0)), Integer.parseInt(ownFields.get(1)),
                Integer.parseInt(ownFields.get(2)));
        return camera;
    }
}
