package co.edu.uptc.models.registers;

import java.util.List;

public class PurchaseRegister extends Register {
    private String supplierName;

    public PurchaseRegister(String invoiceNumber, String date, String status, String supplierName,
            List<RegisterItem> items) {
        super(invoiceNumber, date, status, items);
        this.supplierName = supplierName;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }
}