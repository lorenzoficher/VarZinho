# Triage Labels

The skills speak in terms of five canonical triage roles. This file maps those roles to the actual label strings used in this repo's issue tracker.

| Label in mattpocock/skills | Label in our tracker | Meaning                                  |
| -------------------------- | -------------------- | ---------------------------------------- |
| `needs-triage`             | `needs-triage`       | Maintainer needs to evaluate this issue  |
| `needs-info`               | `needs-info`         | Waiting on reporter for more information |
| `ready-for-agent`          | `ready-for-agent`    | Fully specified, ready for an AFK agent  |
| `ready-for-human`          | `ready-for-human`    | Requires human implementation            |
| `wontfix`                  | `wontfix`            | Will not be actioned                     |

When a skill mentions a role (e.g. "apply the AFK-ready triage label"), use the corresponding label string from this table.

Edit the right-hand column to match whatever vocabulary you actually use.

## Status in this repository

**Only `wontfix` exists on GitHub today.** The other four have not been created, so
`gh issue edit --add-label needs-triage` will fail until they are. Create them when
you first need `/triage`:

```bash
gh label create needs-triage    --description "Maintainer needs to evaluate"
gh label create needs-info      --description "Waiting on reporter"
gh label create ready-for-agent --description "Fully specified, ready for an AFK agent"
gh label create ready-for-human --description "Requires human implementation"
```

These are orthogonal to the repository's own labels: the aggregate labels
(`capture`, `structure`, `highlight`, `people`, `persistence`, `shared`, `console`,
`docs`) say *where* the work is, the triage labels say *what state* it is in. An issue
carries one of each.

`ready-for-agent` has a higher bar here than elsewhere: an issue is AFK-ready only if
it stays inside one aggregate and its acceptance criteria are behaviours a test can
check.
