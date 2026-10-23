package com.pharmacy.catalog;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "medicine_categories")
public class MedicineCategory implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    public MedicineCategory() {
    }

    public MedicineCategory(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public MedicineCategory(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}