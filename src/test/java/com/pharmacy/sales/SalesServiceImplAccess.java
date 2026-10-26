package com.pharmacy.sales;

import com.pharmacy.sales.dto.SalesOrderRequest;

/** Cầu nối cho test ở package khác gọi hàm validate package-private. */
public final class SalesServiceImplAccess {
    private SalesServiceImplAccess() {
    }

    public static void validate(SalesOrderRequest request) {
        SalesServiceImpl.validateCheckout(request);
    }
}
