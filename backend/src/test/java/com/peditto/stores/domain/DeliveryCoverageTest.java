package com.peditto.stores.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.ZoneId;
import com.peditto.identity.domain.DocumentType;
import com.peditto.organizations.domain.Organization;
import org.junit.jupiter.api.Test;

class DeliveryCoverageTest {
    @Test
    void includesPointsInsideAndOnTheConfiguredRadius() {
        assertTrue(DeliveryCoverage.includes(decimal("-23.550000"), decimal("-46.630000"),
                decimal("-23.550000"), decimal("-46.630000"), 100));
        assertTrue(DeliveryCoverage.includes(decimal("0"), decimal("0"),
                decimal("0"), decimal("0.008993"), 1_000));
        assertFalse(DeliveryCoverage.includes(decimal("0"), decimal("0"),
                decimal("0"), decimal("0.009100"), 1_000));
    }

    @Test
    void rejectsInvalidCoordinatesAndRadius() {
        assertThrows(IllegalArgumentException.class, () -> DeliveryCoverage.includes(
                decimal("91"), decimal("0"), decimal("0"), decimal("0"), 1_000));
        assertThrows(IllegalArgumentException.class, () -> DeliveryCoverage.includes(
                decimal("0"), decimal("0"), decimal("0"), decimal("0"), 99));
    }

    @Test
    void coverageChangesRequireExplicitRepublishing() {
        Organization organization = new Organization(DocumentType.CNPJ, "12345678000195", null, "Loja");
        Store store = new Store(organization, "loja-teste", "Loja teste", ZoneId.of("America/Sao_Paulo"));
        DeliveryZone zone = new DeliveryZone(organization, store, decimal("0"), decimal("0"), 1_000);

        assertFalse(zone.covers(decimal("0"), decimal("0")));
        zone.publish();
        assertTrue(zone.covers(decimal("0"), decimal("0")));

        zone.changeCoverage(decimal("-23.55"), decimal("-46.63"), 2_000);
        assertFalse(zone.isActive());
        assertFalse(zone.covers(decimal("-23.55"), decimal("-46.63")));
        zone.publish();
        assertTrue(zone.covers(decimal("-23.55"), decimal("-46.63")));
        zone.pause();
        assertFalse(zone.covers(decimal("-23.55"), decimal("-46.63")));
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
