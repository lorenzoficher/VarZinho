package br.edu.unipampa.varzinho.domain.structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import br.edu.unipampa.varzinho.domain.capture.Camera;
import br.edu.unipampa.varzinho.domain.people.Person;
import br.edu.unipampa.varzinho.exception.DuplicateCameraException;

public final class Gym {
    private final String name;
    private final String address;
    private final List<Person> people = new ArrayList<>();
    private final Map<Integer, Court> courts = new HashMap<>();

    public Gym(String name, String address) {
        if (name == null || name.isBlank() || address == null || address.isBlank()) {
            throw new IllegalArgumentException("a gym needs a name and address");
        }
        this.name = name;
        this.address = address;
    }

    public void addCourt(Court court) {
        if (courts.containsKey(court.getNumber())) {
            throw new IllegalArgumentException("court number is already registered");
        }

        courts.put(court.getNumber(), court);
    }

    /**
     * Mounts a camera on one of the gym's courts. Ids are unique across the whole gym,
     * not only within a court: clips are named after their camera, and the archive
     * tells cameras apart by id alone.
     *
     * @throws IllegalArgumentException if the gym has no court with that number
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

    public Court findCourt(int number) {
        return courts.get(number);
    }

    public List<Court> getCourts() {
        return List.copyOf(courts.values());
    }

    public void registerPerson(Person person) {
        if (person == null) throw new IllegalArgumentException("a gym cannot register a null person");
        if (findPerson(person.getDocument()).isPresent()) {
            throw new IllegalArgumentException("document " + person.getDocument() + " is already registered");
        }
        people.add(person);
    }

    public Optional<Person> findPerson(String document) {
        return people.stream().filter(person -> person.getDocument().equals(document)).findFirst();
    }

    public String getName() { return name; }
    public String getAddress() { return address; }
    public List<Person> getPeople() { return List.copyOf(people); }
}
