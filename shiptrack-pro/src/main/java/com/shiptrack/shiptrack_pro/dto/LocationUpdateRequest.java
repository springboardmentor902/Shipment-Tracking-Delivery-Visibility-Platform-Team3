package com.shiptrack.shiptrack_pro.dto;

import jakarta.validation.constraints.NotNull;

public class LocationUpdateRequest {

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    private String locationName;

    private String notes;

    public LocationUpdateRequest() {
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}