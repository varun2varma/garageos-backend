# GarageST notifications (backend)

Push notifications via FCM. **PostgreSQL is the source of truth**; FCM is delivery only.

```
business service ──(same transaction)──> notification_outbox
                                              │  NotificationOutboxWorker (own thread, 5s poll)
                                              ▼
                         recipients resolved ─> notification (history, 1 row / recipient)
                                              └> notification_delivery (1 row / device) ─> FCM
```

* Event emission: `NotificationEvents.publish…` calls placed next to the business transition (see
  `git grep NotificationEvents`). The publisher only inserts an outbox row in the caller's transaction; a
  rollback leaves nothing behind. It never calls FCM.
* Idempotency: `notification_outbox.event_key` UNIQUE and `notification (event_key, user_id)` UNIQUE.
  Key = `TYPE:ENTITY_TYPE:ENTITY_ID:discriminator`; the discriminator is a transition timestamp (or
  assignment/handover id) so a state re-entered after e.g. a failed quality check is a new event.
* Recipients: `NotificationRecipientResolver` (one place). Staff must satisfy `users.garage_id == event.garageId`
  (owner via `garage.owner_user_id`); customers are matched by `customer.mobile_number == users.mobile`
  (role CUSTOMER, ACTIVE). The acting user is never notified of their own action.
* Retry: bounded exponential backoff (`notifications.worker.base-backoff-seconds`, doubling, max 1h,
  `max-attempts` 6). `UNREGISTERED` deactivates the token. Crashed `PROCESSING` rows are recovered after
  `stale-lock-minutes`.

## Configuration (environment variables — never commit values)

| Variable | Meaning |
|---|---|
| `NOTIFICATIONS_ENABLED` | `true` to enable. Default `false`: publisher is a no-op, worker idle. |
| `FIREBASE_CREDENTIALS_BASE64` | Base64 of the Firebase service-account JSON (Firebase console → Project settings → Service accounts → Generate new private key). Without it the app still starts; in-app history works but no push is sent. |

Optional tuning (`application.properties` keys): `notifications.worker.delay-ms`, `batch-size`,
`max-attempts`, `stale-lock-minutes`, `base-backoff-seconds`.

Encode the key for Render: `base64 -w0 service-account.json` (Linux/macOS) or
`[Convert]::ToBase64String([IO.File]::ReadAllBytes("service-account.json"))` (PowerShell).

## API (all authenticated, always scoped to the caller)

* `POST /api/v1/notifications/devices` `{token, platform, appVersion}` — upsert; reassigns a token held by another user.
* `POST /api/v1/notifications/devices/unregister` `{token}` · `DELETE /api/v1/notifications/devices/{id}`
* `GET /api/v1/notifications?page=0&size=10` (max 50) · `GET /api/v1/notifications/unread-count`
* `PATCH /api/v1/notifications/{id}/read` · `PATCH /api/v1/notifications/read-all`

## Tests

`./mvnw test` runs the unit tests. `NotificationPipelineIT` needs the local `garageos_test` database
(see `src/test/resources/application-test.properties`): `./mvnw test -Dtest=NotificationPipelineIT`.

## Rollback

Set `NOTIFICATIONS_ENABLED=false` (and restart). Tables are additive and can stay.
