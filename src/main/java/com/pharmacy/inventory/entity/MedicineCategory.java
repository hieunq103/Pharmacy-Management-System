package com.pharmacy.inventory.entity;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "medicine_categories")
public class MedicineCategory implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    public MedicineCategory() {
    }

    public MedicineCategory(String name) {
        this.name = name;
    }
}

