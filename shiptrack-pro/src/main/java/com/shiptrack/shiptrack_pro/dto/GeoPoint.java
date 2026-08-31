package com.shiptrack.shiptrack_pro.dto;

import java.math.BigDecimal;

/**
 * A single coordinate pair, plus the address Google matched it to
 * (useful for showing the user how their typed address was understood).
 */
public record GeoPoint(BigDecimal latitude, BigDecimal longitude, String formattedAddress) {

    public String toQueryValue() {
        return latitude.toPlainString() + "," + longitude.toPlainString();
    }
}
