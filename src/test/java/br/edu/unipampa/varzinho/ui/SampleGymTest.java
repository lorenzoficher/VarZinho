package br.edu.unipampa.varzinho.ui;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.capture.FixedCamera;
import br.edu.unipampa.varzinho.domain.capture.PtzCamera;
import br.edu.unipampa.varzinho.domain.structure.Court;
import br.edu.unipampa.varzinho.domain.structure.Gym;
import br.edu.unipampa.varzinho.enums.CameraStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleGymTest {
    @Test
    void courtOneHoldsBothKindsOfCamera() {
        List<Camera> cameras = SampleGym.build().findCourt(1).getCameras();

        assertEquals(2, cameras.size());
        assertTrue(cameras.stream().anyMatch(FixedCamera.class::isInstance));
        assertTrue(cameras.stream().anyMatch(PtzCamera.class::isInstance));
    }

    @Test
    void courtTwoHoldsOneFixedCamera() {
        List<Camera> cameras = SampleGym.build().findCourt(2).getCameras();

        assertEquals(1, cameras.size());
        assertTrue(cameras.get(0) instanceof FixedCamera);
    }

    @Test
    void everyCameraStartsActive() {
        Gym gym = SampleGym.build();

        for (Court court : gym.getCourts()) {
            for (Camera camera : court.getCameras()) {
                assertEquals(CameraStatus.ACTIVE, camera.getStatus(), camera.getId());
            }
        }
        assertTrue(gym.findCourt(1).hasActiveCamera());
        assertTrue(gym.findCourt(2).hasActiveCamera());
    }
}
