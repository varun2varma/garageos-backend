# GarageST reliability implementation: status

Statuses: NOT STARTED, IN PROGRESS, IMPLEMENTED, TESTED, BLOCKED. Final state of this pass is below; the behaviour is documented in `GARAGEST_DEVELOPER_HANDBOOK.md` section 27H.

Baseline before this work (reported): 548 backend unit tests, 68 integration tests. Re-verified at the end of this pass (see "Verification").

| # | Workstream | Status | Root cause | Remaining |
|---|---|---|---|---|
| 1 | Job-card closure and customer self-pickup | TESTED | The close endpoint and the self-pickup delivery already existed. Gaps: closure only trusted the status machine (no delivery/invoice check) and a repeated close failed with a state error and could have re-fired events | Physical walk-through of both flows |
| 2 | Driver photos/videos to the cloud, booking -> job card | TESTED (unit + Flutter); cloud write itself needs R2 credentials | Trip evidence was written to the app server's local disk, never to R2; videos were rejected; retries duplicated rows | Real R2 upload check with credentials |
| 3 | Image orientation | TESTED (fixtures); device check pending | Server-side evidence/thumbnail rendering used `ImageIO.read` (ignores EXIF orientation) and wrote JPEGs without EXIF | Real camera photos on a device; old derived images are not regenerated |
| 4 | Fleet map by driver | TESTED | Map was fed by the active-trip list, so drivers without a trip were absent | Real-device map check |
| 5 | Live marker | TESTED (pure geometry + ordering) | Marker drawn at raw GPS beside the route; an older REST response could overwrite a newer WebSocket fix | Real-GPS drive test |
| 6 | Chat and calling | TESTED (unit + real DB + Flutter) | No chat existed; no call action existed | Provider-backed VoIP not built (dialer only) |
| 7 | Notifications and deep links | TESTED (resolver, template, outbox, routing) | Chat needed an event + explicit recipients + a route | Physical push delivery (foreground/background/terminated) |
| 8 | Handbook, progress doc, verification | DONE | | |

## Decisions
- No second storage system: driver evidence uses the existing Cloudflare R2 store; local disk only when R2 is not configured.
- No second notification framework: one new event type through the existing outbox/worker/FCM path; the push never carries message text.
- Chat participants are derived live from booking/job-card/trip/assignment relationships on every call; nothing is cached or stored, so reassignment takes effect immediately.
- Calling is the device dialer. No VoIP provider is configured or invented.
- Migrations: V71 (nullable `client_upload_id` + partial unique index), V72 (three chat tables). Both are additive.

## Verification (this pass)
- Backend: `./mvnw.cmd -q compile` clean; `./mvnw.cmd test` 627 tests, 0 failures; `./mvnw.cmd test '-Dtest=*IT'` 69 tests, 0 failures (11 classes, includes `ChatIT`, `EstimateLifecycleConcurrencyIT` 7, `EstimateLineClassificationIT` 7).
- Flutter: `flutter analyze` 0 errors (the info/warning lines are pre-existing patterns); `flutter test` all tests passed (695 at the last full run, before the final small additions, which were re-run individually); `flutter build apk --debug` built `app-debug.apk`.
- Test database: only the local `garageos_test`; marker-named fixtures removed (a final query shows 0 leftover rows for the chat, estimate-lifecycle, correction and final-report fixtures).

## Not verified by automated checks
Camera orientation on real phones, R2 upload with credentials, GPS/marker behaviour while driving, push notification delivery and taps in all three app states, the dialer on a real device, iOS (no Apple toolchain here).
