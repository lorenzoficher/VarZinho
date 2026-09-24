package br.edu.unipampa.varzinho.domain.structure;

import br.edu.unipampa.varzinho.domain.people.Person;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Gym {
    private final String name;
    private final String address;
    private final List<Court> courts = new ArrayList<>();
    private final List<Person> people = new ArrayList<>();

    public Gym(String name, String address) {
        if (name == null || name.isBlank() || address == null || address.isBlank()) {
            throw new IllegalArgumentException("a gym needs a name and address");
        }
        this.name = name;
        this.address = address;
    }

    public void addCourt(Court court) {
        if (court == null) throw new IllegalArgumentException("a gym cannot add a null court");
        if (courts.stream().anyMatch(item -> item.getNumber() == court.getNumber())) {
            throw new IllegalArgumentException("court " + court.getNumber() + " already exists");
        }
        courts.add(court);
    }

    public Court findCourt(int number) {
        return courts.stream().filter(court -> court.getNumber() == number).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("court " + number + " was not found"));
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
    public List<Court> getCourts() { return List.copyOf(courts); }
    public List<Person> getPeople() { return List.copyOf(people); }
}
