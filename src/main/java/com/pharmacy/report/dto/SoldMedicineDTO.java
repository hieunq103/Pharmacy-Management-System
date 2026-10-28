package com.pharmacy.report.dto;

import com.pharmacy.common.FormatUtils;
import java.math.BigDecimal;

public class SoldMedicineDTO {
    private final Integer medicineId;
    private final String medicineCode;
    private final String medicineName;
    private final String categoryName;
    private final String unit;
    private final long quantitySold;
    private final BigDecimal totalRevenue;

    public SoldMedicineDTO(Integer medicineId, String medicineCode, String medicineName,
                           String categoryName, String unit, long quantitySold, BigDecimal totalRevenue) {
        this.medicineId = medicineId;
        this.medicineCode = medicineCode;
        this.medicineName = medicineName;
        this.categoryName = categoryName != null ? categoryName : "Chưa phân loại";
        this.unit = unit != null ? unit : "";
        this.quantitySold = quantitySold;
        this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
    }

    public Integer getMedicineId() { return medicineId; }
    public String getMedicineCode() { return medicineCode; }
    public String getMedicineName() { return medicineName; }
    public String getCategoryName() { return categoryName; }
    public String getUnit() { return unit; }
    public long getQuantitySold() { return quantitySold; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }

    public String getFormattedQuantity() {
        return String.format("%,d %s", quantitySold, unit);
    }

    public String getFormattedRevenue() {
        return FormatUtils.formatCurrency(totalRevenue) + "đ";
    }
}
