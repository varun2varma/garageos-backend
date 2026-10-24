#!/usr/bin/env bash
# Local QA only: starts the backend on the disposable database garageos_qa with OTP routed to the local fake provider
# (docs/qa/fake_message_central.py). All values are local throw-away QA values; never use them anywhere shared.
cd "$(dirname "$0")/../.." || exit 1
KEY="$(python -c "import os,base64;print(base64.b64encode(os.urandom(32)).decode())")"
export DB_HOST=localhost DB_PORT=5432 DB_NAME=garageos_qa DB_USERNAME=postgres
export DB_PASSWORD="${QA_DB_PASSWORD:?set QA_DB_PASSWORD to the local test database password}"
export GOOGLE_DRIVE_APPLICATION_NAME=qa GOOGLE_DRIVE_CLIENT_ID=qa GOOGLE_DRIVE_CLIENT_SECRET=qa
export GOOGLE_DRIVE_REDIRECT_URI=http://localhost/qa GOOGLE_DRIVE_ROOT_FOLDER_ID=qa
export GOOGLE_MAPS_ROUTES_API_KEY=qa GOOGLE_MAPS_PLACES_API_KEY=qa
export SECURITY_JWT_SECRET='qa-local-only-signing-key-7f3a9c1e5b2d48a6b0e9d4c7a1f83e52'
export GARAGEST_CORS_ALLOWED_ORIGINS=http://localhost:8081
export GARAGEST_FIELD_ENCRYPTION_KEY="$KEY"
export OTP_ENABLED=true OTP_PROVIDER=SMS OTP_SMS_BASE_URL=http://localhost:9099
export OTP_SMS_CUSTOMER_ID=qa OTP_SMS_PASSWORD=qa OTP_HASH_SECRET=qa-local
exec mvn -o spring-boot:run
