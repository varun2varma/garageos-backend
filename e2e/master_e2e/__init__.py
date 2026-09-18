"""GarageST Master Release Test — backend suite (Suite A).

Drives the real, running backend (real Postgres, real Google Drive) through
one continuous business journey via its actual REST API. No mocks, no direct
database writes, no bypassing ServiceWorkflow. See ../../MASTER_E2E_COVERAGE.md
for what this suite currently proves and what it deliberately leaves BLOCKED.
"""
