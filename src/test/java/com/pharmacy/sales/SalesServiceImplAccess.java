package com.pharmacy.sales;

import com.pharmacy.sales.dto.SalesOrderRequest;

public final class SalesServiceImplAccess {
    private SalesServiceImplAccess() {
    }

    public static void validate(SalesOrderRequest request) {
        SalesServiceImpl.validateCheckout(request);
    }
}
