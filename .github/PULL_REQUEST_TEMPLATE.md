## What changed

<!-- One or two sentences. What does the system do now that it did not before? -->

Closes #

## Spec

<!-- Which spec and which acceptance criteria does this satisfy? e.g. SPEC-02 AC-1.5, AC-1.6 -->

## How it was verified

<!-- Which tests cover it. Paste the relevant `mvn test` output if useful. -->

```
mvn test
```

## Checklist

- [ ] Tests were written before the code (TDD)
- [ ] `mvn test` passes locally
- [ ] Every acceptance criterion in scope has a test
- [ ] Code and comments are in English
- [ ] Fields are private; no setters added without a real use case
- [ ] No class under `domain/` imports from `repository/`
- [ ] Documentation updated if behaviour changed
- [ ] Only files belonging to my aggregate were touched

## Notes for the reviewer

<!-- Anything you are unsure about, or a decision worth a second opinion. -->
