# SPEC-04 — People

**Aggregate:** `Person`, `Athlete`, `Operator` · **Milestone:** Phase 1 — Domain

The people around the system: the athlete who plays and presses the button, and
the operator who holds the archive and finds the clip when asked for it.

Neither is recorded on a highlight. They are registered at the gym (SPEC-01
B-5), which is the only place in the model that refers to them.

## Contract

```java
public abstract class Person {
    protected Person(String name, String document, LocalDate birthDate);
    public int age();
    public abstract String identify();
    public String getName();
    public String getDocument();
}

public class Athlete extends Person {
    public Athlete(String name, String document, LocalDate birthDate,
                   int shirtNumber, String position);
    public String identify();
}

public class Operator extends Person {
    public Operator(String name, String document, LocalDate birthDate,
                    String badge, String shift);
    public String identify();
}
```

## Behaviours

### B-1 — A person carries shared identity

State and behaviour common to everyone live in the superclass, written once.

- **AC-1.1** A person exposes name and document
- **AC-1.2** `age()` returns full years elapsed since the birth date
- **AC-1.3** `age()` is correct on the day before a birthday
- **AC-1.4** `age()` is correct on the birthday itself
- **AC-1.5** A blank name is rejected
- **AC-1.6** A blank document is rejected
- **AC-1.7** A birth date in the future is rejected
- **AC-1.8** `Person` cannot be instantiated directly

### B-2 — Each kind identifies itself differently

This is the polymorphism in this aggregate: the same call, different answers,
no type checks at the call site.

- **AC-2.1** An athlete identifies by shirt number
- **AC-2.2** An operator identifies by badge
- **AC-2.3** Both are usable through a `Person` reference, each producing its
  own identification

### B-3 — An athlete has playing details

- **AC-3.1** An athlete exposes shirt number and position
- **AC-3.2** A shirt number outside 1–99 is rejected
- **AC-3.3** Two athletes with the same document are equal
- **AC-3.4** An athlete's document is what the gym register keys on

### B-4 — An operator has work details

- **AC-4.1** An operator exposes badge and shift
- **AC-4.2** A blank badge is rejected

**Nobody in this aggregate is recorded on a highlight** — not the operator, and
not the athlete who pressed the button. The button carries no identity, and
inventing one would contradict the domain. These classes exist because a gym
has members and staff, not because captures are attributable.

The operator is the one who retrieves clips from the archive, but that happens
in the console: `domain/` must not import `repository/`, so `Operator` holds no
repository and has no `listHighlights()`.

## Errors

| Condition | Exception |
|---|---|
| Blank name, document or badge | `IllegalArgumentException` |
| Future birth date | `IllegalArgumentException` |
| Shirt number outside 1–99 | `IllegalArgumentException` |
| Null birth date | `IllegalArgumentException` |

## Dependencies

None. This aggregate can be built first and in isolation — a good place to
start if you want a quick win on day one.

## Notes

Override `equals()` and `hashCode()` on `Person`, keyed on the document. The
gym register rejects a document it already holds, and that check is the
equality doing its job.

Keep the hierarchy honest: if `Person` ends up with nothing but three fields
and no behaviour, the inheritance is decorative. `age()` and the abstract
`identify()` are what justify it — do not remove them.

## Out of scope

Teams, contracts, statistics, careers, authentication. Nobody logs in to this
system.

Any link from a person to a highlight. That association does not exist — see
[CONTEXT.md](../../CONTEXT.md), *A highlight has no author*.
