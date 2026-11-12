package com.pharmacy.inventory;

import com.pharmacy.inventory.dto.BatchImportItemDTO;

import java.util.List;

public final class InventoryServiceImplAccess {
    private InventoryServiceImplAccess() {
    }

    public static void validate(List<BatchImportItemDTO> items) {
        InventoryService.validateImportItems(items);
    }
}
