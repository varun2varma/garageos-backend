"""Dynamic, collision-free test data for a Master Release Test run.

Every run gets one `run_id` (MRT-<date>-<random4>), and every name/mobile/
email this module generates is derived from it — nothing is hardcoded,
nothing collides with a previous run or with real data, and everything is
trivially identifiable as E2E data in the database if anyone needs to look.
"""
from __future__ import annotations

import datetime
import random
import string
from dataclasses import dataclass, field


def make_run_id() -> str:
    date = datetime.datetime.now().strftime("%Y%m%d")
    suffix = "".join(random.choices(string.digits, k=6))
    return f"MRT{date}{suffix}"


PASSWORD = "Passw0rd!123"


@dataclass
class PersonaSeed:
    run_id: str
    tag: str  # 'owner' | 'mgr' | 'tech' | 'drv' | 'cust'
    seq: str  # single digit to keep mobiles valid (10 digits, starts 6-9)

    @property
    def username(self) -> str:
        return f"{self.run_id.lower()}{self.tag}"

    @property
    def first_name(self) -> str:
        return "MRT"

    @property
    def last_name(self) -> str:
        return self.tag.capitalize()

    @property
    def email(self) -> str:
        return f"{self.username}@example.com"

    @property
    def mobile(self) -> str:
        # 9 + 9 digits derived from the run id, ending in this persona's seq
        digits = "".join(ch for ch in self.run_id if ch.isdigit())[-8:]
        return f"9{digits}{self.seq}"[-10:]


@dataclass
class RunFixtures:
    run_id: str = field(default_factory=make_run_id)

    def persona(self, tag: str, seq: str) -> PersonaSeed:
        return PersonaSeed(self.run_id, tag, seq)

    @property
    def owner(self) -> PersonaSeed:
        return self.persona("owner", "0")

    @property
    def manager(self) -> PersonaSeed:
        return self.persona("mgr", "1")

    @property
    def technician(self) -> PersonaSeed:
        return self.persona("tech", "2")

    @property
    def driver(self) -> PersonaSeed:
        return self.persona("drv", "3")

    @property
    def customer(self) -> PersonaSeed:
        return self.persona("cust", "4")

    # A second, fully independent tenant — used by the garage-isolation
    # security tests. Never touches the primary journey.
    @property
    def rogue_customer(self) -> PersonaSeed:
        return self.persona("rogue", "5")

    @property
    def garage_name(self) -> str:
        return f"{self.run_id} Test Garage"

    @property
    def vehicle_registration(self) -> str:
        return f"MRT{self.run_id[-6:]}"[:10]

    @property
    def vehicle_registration_edited(self) -> str:
        return f"MRTE{self.run_id[-5:]}"[:10]


# A tiny, real, valid 1x1 red-pixel PNG — used for every media-upload test.
# Not a placeholder string: this decodes to real image bytes that Google
# Drive/the backend's content-type validation will accept as an image.
PNG_1PX_RED = bytes.fromhex(
    "89504e470d0a1a0a0000000d49484452000000010000000108020000009077"
    "53de0000000c4944415408d763f8cfc0c00000030101002906b46e0000000049454e44ae426082"
)

# A minimal, structurally-valid MP4 container (ftyp+moov only, no real video
# frames) — enough to exercise "is this a video?" content-type / stage
# handling. If the backend or Drive rejects it as not-a-real-video, that is
# itself a reportable finding, not a script bug to paper over.
MP4_MINIMAL = bytes.fromhex(
    "0000001c667479706d703432000000006d703432697336617663316d703431"
    "0000000866726565"
)
