package com.peditto.stores.domain;

import java.math.BigDecimal;

/** Great-circle distance check used by both public availability and checkout. */
public final class DeliveryCoverage {
    private static final double EARTH_MEAN_RADIUS_METERS = 6_371_008.8;
    private static final BigDecimal MIN_LATITUDE = new BigDecimal(StorePolicy.MIN_LATITUDE);
    private static final BigDecimal MAX_LATITUDE = new BigDecimal(StorePolicy.MAX_LATITUDE);
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal(StorePolicy.MIN_LONGITUDE);
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal(StorePolicy.MAX_LONGITUDE);

    private DeliveryCoverage() {}

    public static boolean includes(BigDecimal centerLatitude, BigDecimal centerLongitude,
            BigDecimal pointLatitude, BigDecimal pointLongitude, int radiusMeters) {
        validateCoordinate(centerLatitude, MIN_LATITUDE, MAX_LATITUDE, "center latitude");
        validateCoordinate(pointLatitude, MIN_LATITUDE, MAX_LATITUDE, "point latitude");
        validateCoordinate(centerLongitude, MIN_LONGITUDE, MAX_LONGITUDE, "center longitude");
        validateCoordinate(pointLongitude, MIN_LONGITUDE, MAX_LONGITUDE, "point longitude");
        if (radiusMeters < StorePolicy.MIN_DELIVERY_RADIUS_METERS
                || radiusMeters > StorePolicy.MAX_DELIVERY_RADIUS_METERS) {
            throw new IllegalArgumentException("Raio de entrega fora do limite permitido.");
        }

        double latitudeDelta = Math.toRadians(pointLatitude.doubleValue() - centerLatitude.doubleValue());
        double longitudeDelta = Math.toRadians(pointLongitude.doubleValue() - centerLongitude.doubleValue());
        double centerLatitudeRadians = Math.toRadians(centerLatitude.doubleValue());
        double pointLatitudeRadians = Math.toRadians(pointLatitude.doubleValue());

        double haversine = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(centerLatitudeRadians) * Math.cos(pointLatitudeRadians)
                * Math.pow(Math.sin(longitudeDelta / 2), 2);
        double distanceMeters = 2 * EARTH_MEAN_RADIUS_METERS
                * Math.asin(Math.sqrt(Math.min(1, haversine)));
        return distanceMeters <= radiusMeters;
    }

    private static void validateCoordinate(BigDecimal value, BigDecimal minimum, BigDecimal maximum, String name) {
        if (value == null || value.scale() > StorePolicy.COORDINATE_DECIMAL_PLACES
                || value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new IllegalArgumentException("Coordenada inválida: " + name + ".");
        }
    }
}
