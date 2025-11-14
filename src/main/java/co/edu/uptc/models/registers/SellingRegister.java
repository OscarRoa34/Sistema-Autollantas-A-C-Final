package co.edu.uptc.models.registers;

import java.util.List;

public class SellingRegister extends Register {
    private String customerName;

    public SellingRegister(String invoiceNumber, String date, String status, String customerName,
            List<RegisterItem> items) {
        super(invoiceNumber, date, status, items);
        this.customerName = customerName;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public double getTotalAmount() {
        if (getItems() == null)
            return 0.0;
        return getItems().stream()
                .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
                .sum();
    }
}