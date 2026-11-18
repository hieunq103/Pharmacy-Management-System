package com.pharmacy.inventory.entity;

import com.pharmacy.employee.Employee;
import com.pharmacy.sales.entity.SalesOrder;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transactions")
public class InventoryTransaction implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        IMPORT, EXPORT_SALE, EXPORT_DISPOSAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private Type type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id", unique = true)
    private SalesOrder salesOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "transaction_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime transactionAt;

    @Column(name = "note", length = 255)
    private String note;

    public InventoryTransaction() {
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public void setSalesOrder(SalesOrder salesOrder) {
        this.salesOrder = salesOrder;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public void setNote(String note) {
        this.note = note;
    }
}

