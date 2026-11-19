package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.PtzCamera;
import br.edu.unipampa.varzinho.domain.capture.StreamCamera;

import java.util.List;

/** Camera kinds shown by the application; a stream needs its own connection flow. */
public enum CameraKind {
    FIXED {
        @Override
        Camera build(CameraSpec spec) {
            return new FixedCamera(spec.getId(), spec.getModel(), spec.getResolution(),
                    spec.getBufferSeconds(), spec.getFixedAngle());
        }
    },
    PTZ {
        @Override
        Camera build(CameraSpec spec) {
            return new PtzCamera(spec.getId(), spec.getModel(), spec.getResolution(),
                    spec.getBufferSeconds());
        }
    },
    STREAM {
        @Override
        Camera build(CameraSpec spec) {
            throw new IllegalArgumentException("a stream camera must be connected with its stream and assembler");
        }
    };

    abstract Camera build(CameraSpec spec);

    /** @return the kinds that the manual installation form can build */
    public static List<CameraKind> installable() {
        return List.of(FIXED, PTZ);
    }

    static CameraKind from(Camera camera) {
        if (camera instanceof FixedCamera) return FIXED;
        if (camera instanceof PtzCamera) return PTZ;
        if (camera instanceof StreamCamera) return STREAM;
        throw new IllegalArgumentException("unknown camera kind: " + camera.getClass().getSimpleName());
    }
}
