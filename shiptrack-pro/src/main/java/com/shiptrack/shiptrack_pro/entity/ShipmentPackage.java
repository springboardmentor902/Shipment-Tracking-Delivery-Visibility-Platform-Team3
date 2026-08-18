package com.shiptrack.shiptrack_pro.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * One physical package inside a shipment (PACKAGES table in the ER diagram).
 *
 * Named ShipmentPackage rather than Package because java.lang.Package already
 * occupies that simple name and would be shadowed in every importing class.
 */
@Entity
@Table(name = "packages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Per-shipment package number: 1, 2, 3 ... Assigned by the service layer. */
    @Column(name = "package_no", nullable = false)
    private Integer packageNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "weight_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal weightKg;

    @Column(name = "length_cm", precision = 10, scale = 2)
    private BigDecimal lengthCm;

    @Column(name = "width_cm", precision = 10, scale = 2)
    private BigDecimal widthCm;

    @Column(name = "height_cm", precision = 10, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "declared_value", precision = 12, scale = 2)
    private BigDecimal declaredValue;

    @Builder.Default
    @Column(name = "fragile", nullable = false)
    private Boolean fragile = false;
}