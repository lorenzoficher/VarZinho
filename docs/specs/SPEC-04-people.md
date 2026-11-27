# SPEC-04 — People

**Aggregate:** `Operator` · **Milestone:** Phase 1 — Domain

`Person` and `Athlete` have left the code: nothing ever read the gym's register
of people, and the athlete stays only as a role in the glossary. The reasoning
is in [CONTEXT.md](../../CONTEXT.md), *Operators are accounts, apart from the gym*.

The one person the system knows is the operator: the staff account that signs
in before the window opens. It is not recorded on a highlight, and no other
class of the domain refers to it.

## Contract

```java
public final class Operator {
    public Operator(String name, String username, String password);
    public static Operator restore(String name, String username, String passwordHash);
    public boolean checksPassword(String password);
    public String getName();
    public String getUsername();
    public String getPasswordHash();
}
```

Operators are kept by their own port, `OperatorStore` in `application/port/`,
apart from the gym's structure; this spec covers only the account.

## Behaviours

### B-1 — An operator is created with a strong password

The constructor takes the plain password, checks it and keeps only its hash.

- **AC-1.1** A blank name is rejected
- **AC-1.2** A blank username, or one containing whitespace, is rejected
- **AC-1.3** A password shorter than 8 characters is rejected
- **AC-1.4** A password with no uppercase letter, no lowercase letter, no digit
  or no symbol is rejected, and the message names the rule that was broken

### B-2 — The plain password is never retained

- **AC-2.1** The stored hash does not contain the plain password
- **AC-2.2** Two operators with the same password receive different hashes,
  because each hash has its own random salt
- **AC-2.3** The hash is PBKDF2 with HMAC-SHA256, written as
  `pbkdf2-sha256$<iterations>$<salt>$<hash>`

### B-3 — An operator checks a password

- **AC-3.1** `checksPassword` accepts the password the operator was created with
- **AC-3.2** It refuses a wrong password
- **AC-3.3** It refuses `null`, rather than throwing

### B-4 — A stored operator is restored without rehashing

`restore` is how a repository rebuilds an account: it receives the hash, not
the password.

- **AC-4.1** A restored operator keeps exactly the stored hash
- **AC-4.2** A restored operator still accepts its original password
- **AC-4.3** A hash in any other format, or with invalid parameters, is
  rejected

### B-5 — An operator is identified by its username

- **AC-5.1** The username is stored in lowercase
- **AC-5.2** Two operators whose usernames differ only in case are equal and
  have the same hash code

Equality ignores the name and the password hash: a username names one account.
Keeping usernames unique is the store's job, through `OperatorStore.save`.

**No operator is recorded on a highlight.** The account opens the window; the
capture button still carries no identity — see [CONTEXT.md](../../CONTEXT.md),
*A highlight has no author*.

## Errors

| Condition | Exception |
|---|---|
| Blank name | `IllegalArgumentException` |
| Blank username, or one containing whitespace | `IllegalArgumentException` |
| Password missing, shorter than 8 characters, or without an uppercase letter, a lowercase letter, a digit or a symbol | `IllegalArgumentException` |
| Stored hash missing, in an unsupported format or with invalid parameters | `IllegalArgumentException` |
| PBKDF2 unavailable in the Java runtime | `IllegalStateException` |

## Dependencies

None inside the project. The hashing uses the JDK's own `javax.crypto`, so no
library is added.

## Notes

`Operator` is immutable, like `Highlight`. A new password does not change an
account: `OperatorAccess` builds a new `Operator` and `OperatorStore.replace`
puts it in place of the old one.

The constructor that takes a ready hash is `private`; outside the class, only
`restore` reaches it, and `restore` validates the hash first.

## Out of scope

Roles and permissions: any signed-in operator can register another. A real
password-recovery e-mail: the recovery in `OperatorAccess` is simulated, and no
message leaves the machine.

## Verification

`OperatorTest` covers AC-1.2 to AC-1.4, AC-2.1, AC-2.2, B-3, B-4 and B-5;
for AC-4.3 it tests a hash in an unsupported format. AC-1.1 (blank name),
AC-2.3 (the hash format) and the invalid-parameter half of AC-4.3 are enforced
in the code but have no dedicated test. Username uniqueness and the stores are covered by
`OperatorStoreContractTest`; sign-in, registration and password reset by
`OperatorAccessTest`.
