package br.edu.unipampa.varzinho.domain.structure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.exception.DuplicateCameraException;

/**
 * The sports venue VarZinho is installed in: its courts, and through them every camera
 * it owns.
 *
 * <p>The gym is where rules that span courts live. Court numbers are unique within it,
 * and so are camera ids, because the archive tells cameras apart by id alone. It keeps
 * no highlights; the archive does.
 */
public final class Gym {
    private final String name;
    private final String address;
    private final Map<Integer, Court> courts = new HashMap<>();

    /**
     * Opens a gym with no courts yet.
     *
     * @param name what the gym is called
     * @param address where it is
     * @throws IllegalArgumentException if the name or the address is missing or blank
     */
    public Gym(String name, String address) {
        if (name == null || name.isBlank() || address == null || address.isBlank()) {
            throw new IllegalArgumentException("a gym needs a name and address");
        }
        this.name = name;
        this.address = address;
    }

    /**
     * Adds a court the caller built, keeping the number it already has.
     *
     * @param court the court to add
     * @throws IllegalArgumentException if the gym already has a court with that number
     */
    public void addCourt(Court court) {
        if (courts.containsKey(court.getNumber())) {
            throw new IllegalArgumentException("court number is already registered");
        }

        courts.put(court.getNumber(), court);
    }

    /**
     * Opens the court numbered one past the gym's highest-numbered court, or court 1
     * when the gym has none.
     *
     * @return the new court, already added to this gym
     */
    public Court openNextCourt() {
        int nextNumber = courts.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
        Court court = new Court(nextNumber);
        addCourt(court);
        return court;
    }

    /**
     * Mounts a camera on one of the gym's courts. Ids are unique across the whole gym,
     * not only within a court: clips are named after their camera, and the archive
     * tells cameras apart by id alone.
     *
     * @param courtNumber the court to mount the camera on
     * @param camera the camera to mount
     * @throws IllegalArgumentException if the gym has no court with that number, or the
     *         camera is null
     * @throws DuplicateCameraException if any court already holds a camera with that id
     */
    public void installCamera(int courtNumber, Camera camera) {
        Court court = courts.get(courtNumber);
        if (court == null) throw new IllegalArgumentException("court " + courtNumber + " is not registered");
        if (camera == null) throw new IllegalArgumentException("a court cannot install a null camera");
        for (Court other : courts.values()) {
            if (other.hasCamera(camera.getId())) {
                throw new DuplicateCameraException(
                        "camera " + camera.getId() + " is already installed on court " + other.getNumber());
            }
        }
        court.installCamera(camera);
    }

    /**
     * Takes a camera off its court and mounts it on another. A camera is equipment, not
     * part of the court, so it can change courts and keep its id.
     *
     * <p>A camera that was recording arrives switched off. Switching it on in its new
     * court starts an empty buffer, so no highlight there holds the old court's footage.
     * A camera under maintenance stays under maintenance.
     *
     * @param camera the camera to move, already installed somewhere in this gym
     * @param toCourt the number of the court it moves to
     * @throws IllegalArgumentException if the gym does not hold the camera, has no court
     *         with that number, or the camera is already on it; nothing moves
     */
    public void moveCamera(Camera camera, int toCourt) {
        if (camera == null) throw new IllegalArgumentException("there is no camera to move");
        Court from = courts.values().stream()
                .filter(court -> court.getCameras().contains(camera))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "camera " + camera.getId() + " is not installed in this gym"));
        Court to = courts.get(toCourt);
        if (to == null) throw new IllegalArgumentException("court " + toCourt + " is not registered");
        if (to == from) {
            throw new IllegalArgumentException("camera " + camera.getId() + " is already on court " + toCourt);
        }
        if (camera.isRecording()) camera.stopRecording();
        from.removeCamera(camera);
        to.installCamera(camera);
    }

    /**
     * Takes an empty court out of the gym. Cameras are equipment that outlives the
     * court, so a court still holding one is refused rather than emptied: the operator
     * removes or moves them first. Highlights already captured there are untouched.
     *
     * @param number the number of the court to remove
     * @throws IllegalArgumentException if the gym has no court with that number
     * @throws IllegalStateException if the court still holds a camera; nothing changes
     */
    public void removeCourt(int number) {
        Court court = courts.get(number);
        if (court == null) throw new IllegalArgumentException("court " + number + " is not registered");
        if (!court.getCameras().isEmpty()) {
            throw new IllegalStateException("court " + number + " still holds cameras");
        }
        courts.remove(number);
    }

    /**
     * Looks up one court by its number.
     *
     * @param number the court's number
     * @return the court, or {@code null} if the gym has no court with that number
     */
    public Court findCourt(int number) {
        return courts.get(number);
    }

    /** {@return an unmodifiable copy of the gym's courts, in no particular order} */
    public List<Court> getCourts() {
        return List.copyOf(courts.values());
    }

    /** {@return what the gym is called, never blank} */
    public String getName() { return name; }

    /** {@return where the gym is, never blank} */
    public String getAddress() { return address; }
}
