package com.pharmacy.inventory.entity;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "suppliers")
public class Supplier implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "tax_code", unique = true, length = 30)
    private String taxCode;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(name = "phone", length = 15)
    private String phone;

    public Supplier() {}

    public Supplier(String name) {
        this.name = name;
    }
}
