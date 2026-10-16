-- GarageST - stable staff references (D10 Quality Check inspector, D11 responsible-staff attribution).
--
-- ADDITIVE and NULLABLE: historical rows keep exactly what they had (a free-text inspector name, no creator).
-- Nothing is back-filled by guessing - a legacy row simply has no reliable attribution and is reported as such.
--
--  * quality_check.inspected_by_user_id : the user who performed the check (the existing inspected_by text stays as a
--    name snapshot so reports survive later renames / deactivation of the user).
--  * job_card.created_by_user_id / created_by_roles / created_by_name : who opened the job card, with the roles they
--    acted in and a name snapshot, recorded at creation time from the authenticated principal only.
--  * estimate.generated_by_user_id / invoice.generated_by_user_id : who prepared those documents (creator only -
--    NOT the approver, which is a separate fact).

ALTER TABLE quality_check
    ADD COLUMN inspected_by_user_id BIGINT,
    ADD CONSTRAINT fk_quality_check_inspector FOREIGN KEY (inspected_by_user_id) REFERENCES users (id);

ALTER TABLE job_card
    ADD COLUMN created_by_user_id BIGINT,
    ADD COLUMN created_by_roles VARCHAR(100),
    ADD COLUMN created_by_name VARCHAR(200),
    ADD CONSTRAINT fk_job_card_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id);

ALTER TABLE estimate
    ADD COLUMN generated_by_user_id BIGINT,
    ADD CONSTRAINT fk_estimate_generated_by FOREIGN KEY (generated_by_user_id) REFERENCES users (id);

ALTER TABLE invoice
    ADD COLUMN generated_by_user_id BIGINT,
    ADD CONSTRAINT fk_invoice_generated_by FOREIGN KEY (generated_by_user_id) REFERENCES users (id);
