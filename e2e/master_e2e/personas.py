"""Role-scoped sessions for the Master Release Test.

Each PersonaSession wraps one MasterE2EClient + one bearer token, and knows
how to refresh itself after a role/garage change (registration only ever
grants USER; every real role — OWNER, MANAGER, TECHNICIAN, DRIVER,
CUSTOMER — is granted by a real backend action, exactly as a genuine user
would acquire it, never injected).
"""
from __future__ import annotations

from dataclasses import dataclass

from .client import MasterE2EClient
from .fixtures import PASSWORD, PersonaSeed


@dataclass
class PersonaSession:
    client: MasterE2EClient
    seed: PersonaSeed
    token: str = ""
    user: dict | None = None

    @property
    def label(self) -> str:
        return self.seed.tag.upper()

    def register(self) -> None:
        self.client.call(
            "POST",
            "/auth/register",
            {
                "firstName": self.seed.first_name,
                "lastName": self.seed.last_name,
                "mobile": self.seed.mobile,
                "email": self.seed.email,
                "username": self.seed.username,
                "password": PASSWORD,
            },
            actor=self.label,
            expect=(201,),
        )

    def login(self) -> "PersonaSession":
        body = self.client.post(
            "/auth/login",
            {"username": self.seed.username, "password": PASSWORD},
            actor=self.label,
            expect=(200,),
        )
        self.token = body["accessToken"]
        self.user = body["user"]
        return self

    def register_and_login(self) -> "PersonaSession":
        self.register()
        return self.login()

    def refresh(self) -> "PersonaSession":
        """Re-login to pick up a role/garageId granted since the last login."""
        return self.login()

    # Convenience wrappers so call sites read as `manager.get(...)` etc.
    def get(self, path: str, **kw):
        return self.client.get(path, token=self.token, actor=self.label, **kw)

    def post(self, path: str, body=None, **kw):
        return self.client.post(path, body, token=self.token, actor=self.label, **kw)

    def put(self, path: str, body=None, **kw):
        return self.client.put(path, body, token=self.token, actor=self.label, **kw)

    def call(self, method: str, path: str, body=None, **kw):
        return self.client.call(method, path, body, token=self.token, actor=self.label, **kw)

    def multipart(self, path: str, fields: dict, file_field: str, filename: str,
                  file_bytes: bytes, content_type: str, **kw):
        return self.client.multipart(
            path, fields, file_field, filename, file_bytes, content_type,
            token=self.token, actor=self.label, **kw
        )
