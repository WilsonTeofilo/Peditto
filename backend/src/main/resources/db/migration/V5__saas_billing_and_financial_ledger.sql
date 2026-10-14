CREATE TABLE plans (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    code varchar(40) NOT NULL UNIQUE, name varchar(100) NOT NULL, active boolean NOT NULL DEFAULT true,
    version bigint NOT NULL DEFAULT 0
);
CREATE TABLE plan_price_versions (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    plan_id uuid NOT NULL REFERENCES plans(id) ON DELETE RESTRICT,
    amount numeric(12,2) NOT NULL CHECK (amount >= 0), currency char(3) NOT NULL DEFAULT 'BRL' CHECK(currency='BRL'),
    interval_unit varchar(12) NOT NULL CHECK (interval_unit IN ('MONTH','YEAR')),
    effective_from timestamptz NOT NULL, effective_until timestamptz,
    trial_days smallint NOT NULL DEFAULT 30 CHECK (trial_days BETWEEN 0 AND 90),
    max_products integer CHECK (max_products IS NULL OR max_products > 0),
    max_orders_month integer CHECK (max_orders_month IS NULL OR max_orders_month > 0),
    entitlements jsonb NOT NULL DEFAULT '{}'::jsonb, version bigint NOT NULL DEFAULT 0,
    CONSTRAINT ck_plan_price_dates CHECK (effective_until IS NULL OR effective_until > effective_from),
    CONSTRAINT uk_plan_price_start UNIQUE(plan_id,effective_from)
);
CREATE INDEX ix_plan_price_effective ON plan_price_versions(plan_id,effective_from,effective_until);

CREATE TABLE organization_subscriptions (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    organization_id uuid NOT NULL UNIQUE REFERENCES organizations(id) ON DELETE RESTRICT,
    plan_price_version_id uuid NOT NULL REFERENCES plan_price_versions(id) ON DELETE RESTRICT,
    status varchar(24) NOT NULL CHECK (status IN ('TRIAL','ACTIVE','PAST_DUE_GRACE','SUSPENDED','CANCELED')),
    current_period_start timestamptz NOT NULL, current_period_end timestamptz NOT NULL,
    trial_ends_at timestamptz, canceled_at timestamptz, grace_ends_at timestamptz,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT ck_subscription_period CHECK (current_period_end > current_period_start)
);
CREATE INDEX ix_subscriptions_status_period ON organization_subscriptions(status,current_period_end);

ALTER TABLE billing_invoices
    ADD CONSTRAINT uk_billing_invoices_tenant_id UNIQUE (organization_id,id),
    ADD COLUMN plan_price_version_id uuid REFERENCES plan_price_versions(id) ON DELETE RESTRICT,
    ADD COLUMN currency char(3) NOT NULL DEFAULT 'BRL' CHECK (currency='BRL'),
    ADD COLUMN description_snapshot varchar(200) NOT NULL DEFAULT 'Assinatura Peditto';
CREATE INDEX ix_billing_invoice_status_due ON billing_invoices(status,due_at);

CREATE TABLE invoice_payments (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    organization_id uuid NOT NULL, invoice_id uuid NOT NULL,
    method varchar(12) NOT NULL CHECK (method IN ('PIX','CARD')),
    status varchar(20) NOT NULL CHECK (status IN ('PENDING','AUTHORIZED','PAID','FAILED','CANCELLED','REFUNDED','PARTIALLY_REFUNDED')),
    amount numeric(12,2) NOT NULL CHECK(amount >= 0), currency char(3) NOT NULL DEFAULT 'BRL' CHECK(currency='BRL'),
    provider varchar(40) NOT NULL, provider_payment_reference varchar(180), provider_idempotency_key varchar(160) NOT NULL,
    paid_at timestamptz, refunded_amount numeric(12,2) NOT NULL DEFAULT 0 CHECK (refunded_amount BETWEEN 0 AND amount),
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT fk_invoice_payment_tenant FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_invoice_payment_invoice_tenant FOREIGN KEY (organization_id,invoice_id) REFERENCES billing_invoices(organization_id,id) ON DELETE RESTRICT,
    CONSTRAINT uk_invoice_payment_provider_key UNIQUE(provider,provider_idempotency_key)
);
CREATE UNIQUE INDEX uk_invoice_payment_provider_ref ON invoice_payments(provider,provider_payment_reference) WHERE provider_payment_reference IS NOT NULL;
CREATE INDEX ix_invoice_payments_invoice ON invoice_payments(organization_id,invoice_id,status);

CREATE TABLE provider_events (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    provider varchar(40) NOT NULL, provider_event_id varchar(180) NOT NULL,
    event_type varchar(100) NOT NULL, payload_fingerprint char(64) NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('RECEIVED','PROCESSING','PROCESSED','FAILED','IGNORED')),
    received_at timestamptz NOT NULL, processed_at timestamptz, attempts smallint NOT NULL DEFAULT 0 CHECK(attempts BETWEEN 0 AND 100),
    last_error_code varchar(80), resource_reference varchar(180),
    CONSTRAINT uk_provider_event UNIQUE(provider,provider_event_id)
);
CREATE INDEX ix_provider_events_pending ON provider_events(status,received_at) WHERE status IN ('RECEIVED','FAILED');

CREATE TABLE financial_ledger_entries (
    id uuid PRIMARY KEY, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL,
    organization_id uuid REFERENCES organizations(id) ON DELETE RESTRICT,
    source_type varchar(24) NOT NULL CHECK(source_type IN ('INVOICE_PAYMENT','ORDER_PAYMENT','REFUND','FEE','ADJUSTMENT')),
    source_id uuid NOT NULL, entry_type varchar(40) NOT NULL,
    amount numeric(14,2) NOT NULL CHECK(amount <> 0), currency char(3) NOT NULL DEFAULT 'BRL' CHECK(currency='BRL'),
    occurred_at timestamptz NOT NULL, idempotency_key varchar(180) NOT NULL UNIQUE,
    metadata_redacted jsonb NOT NULL DEFAULT '{}'::jsonb,
    version bigint NOT NULL DEFAULT 0
);
CREATE INDEX ix_ledger_org_occurred ON financial_ledger_entries(organization_id,occurred_at DESC);
CREATE INDEX ix_ledger_source ON financial_ledger_entries(source_type,source_id);
CREATE TRIGGER trg_financial_ledger_immutable BEFORE UPDATE OR DELETE ON financial_ledger_entries FOR EACH ROW EXECUTE FUNCTION peditto_reject_immutable_mutation();
