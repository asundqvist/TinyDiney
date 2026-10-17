package se.edugrade.java25.restaurant.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Stable identity for demo data, retained when an admin edits its public fields.
    @Column(unique = true, length = 64)
    private String demoKey;

    public void identifyDemo(String key) {
        if (demoKey == null) demoKey = key;
    }

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String cuisine;

    @Column(nullable = false)
    private int seats;

    public Restaurant(String name, String city, String cuisine, int seats) {
        this.name = name;
        this.city = city;
        this.cuisine = cuisine;
        this.seats = seats;
    }

    public void update(String name, String city, String cuisine, int seats) {
        this.name = name;
        this.city = city;
        this.cuisine = cuisine;
        this.seats = seats;
    }
}
