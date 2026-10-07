package com.pharmacy.catalog;

public class MedicineCategory {

    private int id;
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