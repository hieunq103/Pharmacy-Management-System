package com.pharmacy.inventory;

import com.pharmacy.inventory.dto.BatchImportItemDTO;

import java.util.List;

/** Cầu nối cho test ở package khác gọi hàm validate package-private. */
public final class InventoryServiceImplAccess {
    private InventoryServiceImplAccess() {
    }

    public static void validate(List<BatchImportItemDTO> items) {
        InventoryServiceImpl.validateImportItems(items);
    }
}
