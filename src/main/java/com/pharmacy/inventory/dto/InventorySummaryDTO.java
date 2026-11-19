package com.pharmacy.inventory.dto;

public record InventorySummaryDTO(
    long totalMedicines,
    long inStockMedicines,
    long lowStockMedicines,
    long nearExpiryMedicines
) {}
