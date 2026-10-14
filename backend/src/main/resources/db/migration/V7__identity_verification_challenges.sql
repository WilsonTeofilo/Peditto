ALTER TABLE auth_challenges DROP CONSTRAINT auth_challenges_purpose_check;
ALTER TABLE auth_challenges ADD CONSTRAINT auth_challenges_purpose_check
    CHECK (purpose IN ('LOGIN','PASSWORD_RESET','CONTACT_CHANGE','MFA_RECOVERY','VERIFY_EMAIL','VERIFY_PHONE'));
ALTER TABLE auth_challenges ADD COLUMN version bigint NOT NULL DEFAULT 0;
