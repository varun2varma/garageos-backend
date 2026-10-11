# JWT signing secret: remediation plan and rotation runbook

Status: **code implemented (2026-10-11), not deployed.** The committed default is removed and the application refuses to
start without a strong `SECURITY_JWT_SECRET`. No deployed environment has been changed, rotated or checked by this work,
and no secret value appears in this document or in any file it asks you to create.

## 1. Finding

`src/main/resources/application.properties` defines `security.jwt.secret` as a literal, not as an environment
placeholder. The same literal was copied into `src/test/resources/application-test.properties`. Anyone who can read the
repository (or a build artifact, or a transcript that displayed the file) can sign a token for any user, including a
`SUPER_ADMIN`, in every environment that does not override the property. `JwtServiceImpl` reads it with
`@Value("${security.jwt.secret}")` and builds the key with `Keys.hmacShaKeyFor(secret.getBytes())`.

The value must be treated as **compromised** (it was also displayed once in an audit session transcript). It cannot be
made safe by deleting the line: it stays in Git history. The only fix is to rotate every environment that may have used
it.

## 2. Done in this batch

- The automated tests now use a dedicated, obviously test-only signing key in `application-test.properties` that is
  different from the committed application default (verified by comparison; no value printed). Tests that mint or parse
  tokens (`UserAdministrationSecurityIT` logs in and uses the application's own tokens) are unaffected.

## 3. Implemented code change

- `application.properties`: `security.jwt.secret=${SECURITY_JWT_SECRET:}`. There is **no default value**.
- `JwtSecretPolicy` + `@PostConstruct` in `JwtServiceImpl`: startup fails, with a message that never repeats the
  value, when the secret is missing or blank, shorter than 32 bytes, has fewer than 10 distinct characters, or equals a
  value that was ever committed to the repository. Previously committed values are recognised by SHA-256 fingerprint
  only (the repository history contains exactly one such value; its fingerprint is in `JwtSecretPolicy`). The check has
  no profile bypass: tests satisfy it with their own key instead of being exempted.
- Token creation, parsing and validation are unchanged (`JwtSecretStartupTest` asserts the same claims, expiry and
  signature behaviour with a valid secret).
- Tests: `application-test.properties` carries an independent test-only key; `JwtSecretStartupTest` (11 tests) covers
  missing, blank, short, repetitive, previously committed (by fingerprint, and, when a local file is supplied through
  `GARAGEST_EXPOSED_JWT_FILE`, against the real old value), valid configuration, foreign-signed tokens, and that the
  committed `application.properties` holds no literal secret.
- Local E2E: `garagest-e2e.env.example` and `LOCAL_MASTER_RELEASE_TEST.md` now document the new required variable.
  The developer's own `garagest-e2e.env` (ignored by `.gitignore` but still tracked in Git from an early commit, so bulk staging would commit it) must get its own `SECURITY_JWT_SECRET` line (see the
  checklist below); it was not edited by this change.

## 3a. Deployment checklist (order matters)

1. **Before deploying this code** set `SECURITY_JWT_SECRET` in every environment that will run it: production,
   staging, preview/branch deployments, any shared E2E or demo server, CI jobs that boot the application. The
   container image (`Dockerfile`, `CMD java -jar ...`) takes no configuration of its own, so the hosting platform's
   environment is the only source. Without the variable the new version fails at startup and the previous version keeps
   running only until the platform replaces it.
2. Use a **different** random value per environment and do not reuse the E2E or developer values.
3. Local developers and the E2E harness: add a locally generated `SECURITY_JWT_SECRET=` line to `garagest-e2e.env` (or
   the IDE run configuration). The E2E harness starts a backend from that file's variables, so without the line the local
   backend will not start.
4. Deploy. If the platform keeps the old instance until the new one is healthy, a missing variable shows up as a failed
   deploy with the message `JWT signing secret is not configured ...`, not as an outage.
5. Then rotate, if the environment used the exposed default (section 5): setting a new value in step 1 already
   **is** the rotation for that environment, and forces every user to sign in again (section 7).

## 4. Configuration to supply

| Property | Environment variable | Notes |
|---|---|---|
| `security.jwt.secret` | `SECURITY_JWT_SECRET` | at least 32 random bytes, 64 or more characters recommended; unique per environment |
| `security.jwt.access-token-expiration` | `SECURITY_JWT_ACCESS_TOKEN_EXPIRATION` | currently `1h`, keep |
| `security.jwt.refresh-token-expiration` | `SECURITY_JWT_REFRESH_TOKEN_EXPIRATION` | currently `7d`, keep |

Spring relaxed binding already lets `SECURITY_JWT_SECRET` override the property today, so an environment that sets it is
not using the committed default.

## 5. Which environments may be using the exposed value (without revealing it)

1. **Presence check.** In the hosting dashboard or secret manager look only at whether `SECURITY_JWT_SECRET` exists
   for each service (production, staging, preview, E2E, any developer machine that talks to a shared database). Absent
   means the committed default is in use: treat as exposed.
2. **Behavioural probe** (for environments where the variable is present but its origin is unknown, for example copied
   from the repository). Use a throw-away smoke account that exists in that environment. From a trusted machine, with the
   committed value read from a local file (never typed into a shell, never echoed):

   ```python
   # probe_jwt.py  <base-url> <smoke-username> <secret-file>
   import sys, time, json, hmac, hashlib, base64, urllib.request
   base, user, path = sys.argv[1:4]
   key = open(path, 'rb').read().strip()
   b64 = lambda b: base64.urlsafe_b64encode(b).rstrip(b'=')
   head = b64(json.dumps({"alg": "HS256", "typ": "JWT"}).encode())
   body = b64(json.dumps({"sub": user, "exp": int(time.time()) + 300}).encode())
   sig = b64(hmac.new(key, head + b'.' + body, hashlib.sha256).digest())
   req = urllib.request.Request(base + '/api/v1/auth/me',
                                headers={'Authorization': 'Bearer ' + (head + b'.' + body + b'.' + sig).decode()})
   try:
       print('ACCEPTED (environment uses the exposed secret)' if urllib.request.urlopen(req).status == 200 else 'rejected')
   except Exception as e:
       print('rejected', getattr(e, 'code', ''))
   ```

   `ACCEPTED` means that environment must be rotated. The script prints only the verdict. Use only accounts you
   own, and only against environments you are authorized to test.
3. Treat any environment whose database or logs were reachable by people outside the team as exposed regardless of the
   result.

## 6. Rotation procedure (secret manager)

1. Generate a new value on a trusted machine: `openssl rand -base64 48` (about 64 characters). Do not paste it into
   chat, tickets or shell history; write it directly into the secret manager.
2. Store it as `SECURITY_JWT_SECRET` in the environment's secret store (for example the Render environment or an
   environment group). One value per environment.
3. Redeploy or restart the service so it picks the value up.
4. Optional but recommended: revoke stored refresh sessions so that nothing issued under the old key lingers in the
   database (`user_sessions.revoked = true` for all rows). Run it from your approved database tooling, not from this
   task.
5. Record the rotation date and who did it. Schedule the next rotation (for example every 6 to 12 months).

## 7. Expected session behaviour

The signing key is the only thing that validates a token. After rotation every previously issued access token and
refresh token fails signature validation. Users get `401` on their next call, the Flutter app returns to the login
screen (there is no automatic refresh that could succeed, because the refresh token is signed with the same key), and
they sign in again with their normal credentials. Passwords, accounts, roles and data are untouched. Plan the change
for a quiet period and tell staff to expect one forced sign-in. Push-notification device tokens are unaffected.

## 8. Verifying the old secret is no longer accepted

1. Run the probe in section 5 against the rotated environment with the old value: expected `rejected 401`.
2. Sign in normally and call `GET /api/v1/auth/me`: expected `200`.
3. Confirm a token captured before the rotation now returns `401`.
4. After the code change in section 3, start the service with the variable unset (on a throw-away copy only): expected
   startup failure with a clear message, never a silent fallback.
