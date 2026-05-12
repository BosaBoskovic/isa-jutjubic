package com.jutjubic.jutjubic_backend.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class GeoLocation {

    @Min(-90)
    @Max(90)
    private Double latitude;

    @Min(-180)
    @Max(180)
    private Double longitude;
}
