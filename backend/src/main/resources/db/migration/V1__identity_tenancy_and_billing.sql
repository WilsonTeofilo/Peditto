CREATE TABLE organizations (
 id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
 document_type varchar(4) NOT NULL CHECK(document_type IN ('CPF','CNPJ')),
 document_normalized varchar(14) NOT NULL UNIQUE CHECK(document_normalized ~ '^[0-9]+$'), legal_name varchar(160), trade_name varchar(120) NOT NULL,
 status varchar(32) NOT NULL CHECK(status IN ('PENDING_VERIFICATION','ACTIVE','SUSPENDED','CLOSED')),
 version bigint NOT NULL DEFAULT 0,
 CONSTRAINT ck_organization_document_length CHECK((document_type='CPF' AND length(document_normalized)=11) OR (document_type='CNPJ' AND length(document_normalized)=14))
);
CREATE TABLE app_users (
 id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
 email_normalized varchar(254) NOT NULL UNIQUE, phone_e164 varchar(16) NOT NULL UNIQUE,
 password_hash varchar(255) NOT NULL, email_verified_at timestamptz, phone_verified_at timestamptz,
 enabled boolean NOT NULL DEFAULT false, version bigint NOT NULL DEFAULT 0,
 CONSTRAINT ck_app_users_phone_e164 CHECK(phone_e164 ~ '^\+[1-9][0-9]{7,14}$')
);
CREATE INDEX ix_app_users_enabled ON app_users(enabled);
CREATE TABLE user_documents (
 id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
 user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
 document_type varchar(4) NOT NULL CHECK(document_type IN ('CPF','CNPJ')),
 document_normalized varchar(14) NOT NULL UNIQUE CHECK(document_normalized ~ '^[0-9]+$'), verified_at timestamptz,
 CONSTRAINT ck_user_document_length CHECK((document_type='CPF' AND length(document_normalized)=11) OR (document_type='CNPJ' AND length(document_normalized)=14))
);
CREATE INDEX ix_user_documents_user_id ON user_documents(user_id);
CREATE TABLE memberships (
 id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
 organization_id uuid NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
 user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
 role varchar(20) NOT NULL CHECK(role IN ('OWNER','MANAGER','STAFF')),
 status varchar(20) NOT NULL CHECK(status IN ('INVITED','ACTIVE','SUSPENDED','REVOKED')),
 CONSTRAINT uk_memberships_organization_user UNIQUE(organization_id,user_id)
);
CREATE UNIQUE INDEX uk_memberships_active_owner ON memberships(organization_id) WHERE role='OWNER' AND status='ACTIVE';
CREATE INDEX ix_memberships_org_status ON memberships(organization_id,status);
CREATE INDEX ix_memberships_user_status ON memberships(user_id,status);
CREATE TABLE stores (
 id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
 organization_id uuid NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
 slug varchar(63) NOT NULL UNIQUE CHECK(slug = lower(slug) AND slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'), display_name varchar(120) NOT NULL,
 status varchar(32) NOT NULL CHECK(status IN ('SETUP_REQUIRED','DRAFT','ACTIVE','SUSPENDED','CLOSED')),
 timezone varchar(64) NOT NULL, version bigint NOT NULL DEFAULT 0,
 CONSTRAINT uk_stores_organization_id UNIQUE(organization_id,id)
);
CREATE INDEX ix_stores_organization_id ON stores(organization_id);
CREATE TABLE trials (
 id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
 organization_id uuid NOT NULL UNIQUE REFERENCES organizations(id) ON DELETE RESTRICT,
 starts_at timestamptz NOT NULL, ends_at timestamptz NOT NULL,
 status varchar(20) NOT NULL CHECK(status IN ('SCHEDULED','ACTIVE','EXPIRED','CONVERTED','REVOKED')),
 rule_version varchar(32) NOT NULL, CONSTRAINT ck_trials_dates CHECK(ends_at>starts_at)
);
CREATE TABLE auth_challenges (
 id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
 user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
 purpose varchar(24) NOT NULL CHECK(purpose IN ('LOGIN','PASSWORD_RESET','CONTACT_CHANGE','MFA_RECOVERY')),
 channel varchar(8) NOT NULL CHECK(channel IN ('EMAIL','SMS')), code_hash varchar(255) NOT NULL,
 expires_at timestamptz NOT NULL, attempts smallint NOT NULL DEFAULT 0 CHECK(attempts BETWEEN 0 AND 10),
 consumed_at timestamptz, CONSTRAINT ck_auth_challenge_expiry CHECK(expires_at>created_at)
);
CREATE INDEX ix_auth_challenges_user_purpose ON auth_challenges(user_id,purpose,expires_at);
CREATE INDEX ix_auth_challenges_live ON auth_challenges(expires_at) WHERE consumed_at IS NULL;
CREATE TABLE store_domains (
 id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
 store_id uuid NOT NULL REFERENCES stores(id) ON DELETE RESTRICT,
 hostname_normalized varchar(253) NOT NULL UNIQUE, verified_at timestamptz,
 active boolean NOT NULL DEFAULT false, version bigint NOT NULL DEFAULT 0
);
CREATE INDEX ix_store_domains_store_id ON store_domains(store_id);
CREATE TABLE billing_invoices (
 id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
 organization_id uuid NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
 period_start timestamptz NOT NULL, period_end timestamptz NOT NULL, due_at timestamptz NOT NULL,
 amount numeric(12,2) NOT NULL CHECK(amount>=0),
 status varchar(20) NOT NULL CHECK(status IN ('OPEN','PAID','VOID','PAST_DUE','WRITTEN_OFF')),
 paid_at timestamptz, provider_reference varchar(160), version bigint NOT NULL DEFAULT 0,
 CONSTRAINT ck_invoice_period CHECK(period_end>period_start),
 CONSTRAINT uk_billing_invoice_org_period UNIQUE(organization_id,period_start,period_end)
);
CREATE INDEX ix_billing_invoices_org_due ON billing_invoices(organization_id,due_at,status);
CREATE UNIQUE INDEX uk_billing_invoice_provider_reference ON billing_invoices(provider_reference) WHERE provider_reference IS NOT NULL;
CREATE TABLE late_charges (
 id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
 invoice_id uuid NOT NULL REFERENCES billing_invoices(id) ON DELETE RESTRICT,
 charge_date date NOT NULL, amount numeric(8,2) NOT NULL CHECK(amount>=0), rule_version varchar(32) NOT NULL,
 CONSTRAINT uk_late_charges_invoice_date UNIQUE(invoice_id,charge_date)
);
CREATE INDEX ix_late_charges_invoice ON late_charges(invoice_id);
CREATE TABLE audit_events (
 id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
 organization_id uuid REFERENCES organizations(id) ON DELETE RESTRICT,
 actor_user_id uuid REFERENCES app_users(id) ON DELETE SET NULL,
 action varchar(80) NOT NULL, resource_type varchar(80) NOT NULL, resource_id uuid,
 request_id varchar(80), metadata_redacted jsonb NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX ix_audit_events_org_created ON audit_events(organization_id,created_at DESC);
CREATE INDEX ix_audit_events_actor_created ON audit_events(actor_user_id,created_at DESC);

