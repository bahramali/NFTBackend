package se.hydroleaf.store.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddress {

    @Column(name = "ship_name", nullable = false)
    private String name;

    @Column(name = "ship_line1", nullable = false)
    private String line1;

    @Column(name = "ship_line2")
    private String line2;

    @Column(name = "ship_city", nullable = false)
    private String city;

    @Column(name = "ship_state")
    private String state;

    @Column(name = "ship_postal", nullable = false)
    private String postalCode;

    @Column(name = "ship_country", nullable = false)
    private String country;

    @Column(name = "ship_phone")
    private String phone;
}
