package com.pharmacy.infra.db;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "code_sequences")
public class CodeSequence implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "name", length = 30)
    private String name;

    @Column(name = "current_number", nullable = false)
    private long currentNumber;

    public CodeSequence() {}

    public CodeSequence(String name, long currentNumber) {
        this.name = name;
        this.currentNumber = currentNumber;
    }

    public long getCurrentNumber() {
        return currentNumber;
    }

    public void setCurrentNumber(long currentNumber) {
        this.currentNumber = currentNumber;
    }
}
