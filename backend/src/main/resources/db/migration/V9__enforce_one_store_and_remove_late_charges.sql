DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM stores
        GROUP BY organization_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot enforce one store per organization: duplicate stores require reconciliation.';
    END IF;

    IF EXISTS (SELECT 1 FROM late_charges) THEN
        RAISE EXCEPTION 'Cannot remove legacy late charges: records require reconciliation first.';
    END IF;
END
$$;

ALTER TABLE stores
    ADD CONSTRAINT uk_stores_one_per_organization UNIQUE (organization_id);

DROP TABLE late_charges;