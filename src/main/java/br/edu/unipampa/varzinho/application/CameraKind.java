package br.edu.unipampa.varzinho.application;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.PtzCamera;

/** The camera kinds the gym manager may install through the application. */
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
    };

    abstract Camera build(CameraSpec spec);
}
