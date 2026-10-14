-- Use variable-length strings for hashes/currency so JDBC/Hibernate mappings agree with PostgreSQL types.
ALTER TABLE user_sessions ALTER COLUMN refresh_token_hash TYPE varchar(64);
ALTER TABLE password_reset_tokens ALTER COLUMN token_hash TYPE varchar(64);
ALTER TABLE api_rate_limit_buckets ALTER COLUMN bucket_key TYPE varchar(64);
ALTER TABLE store_assets ALTER COLUMN sha256 TYPE varchar(64);
ALTER TABLE shopping_carts ALTER COLUMN anonymous_token_hash TYPE varchar(64);
ALTER TABLE cart_items ALTER COLUMN configuration_hash TYPE varchar(64);
ALTER TABLE customer_orders ALTER COLUMN currency TYPE varchar(3);
ALTER TABLE order_payments ALTER COLUMN currency TYPE varchar(3);
ALTER TABLE idempotency_records ALTER COLUMN actor_fingerprint TYPE varchar(64);
ALTER TABLE idempotency_records ALTER COLUMN request_fingerprint TYPE varchar(64);
ALTER TABLE plan_price_versions ALTER COLUMN currency TYPE varchar(3);
ALTER TABLE billing_invoices ALTER COLUMN currency TYPE varchar(3);
ALTER TABLE invoice_payments ALTER COLUMN currency TYPE varchar(3);
ALTER TABLE provider_events ALTER COLUMN payload_fingerprint TYPE varchar(64);
ALTER TABLE financial_ledger_entries ALTER COLUMN currency TYPE varchar(3);
