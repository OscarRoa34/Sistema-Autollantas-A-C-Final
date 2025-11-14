package co.edu.uptc.models.registers;

import java.util.List;

public abstract class Register {
    private String invoiceNumber;
    private String date;
    private double total;
    private String status;
    private List<RegisterItem> items;

    public Register(String invoiceNumber, String date, String status, List<RegisterItem> items) {
        this.invoiceNumber = invoiceNumber;
        this.date = date;
        this.status = status;
        this.items = items;
        this.total = this.calculateTotal();
    }

    private double calculateTotal() {
        if (this.items == null) {
            return 0.0;
        }
        double sum = 0;
        for (RegisterItem item : this.items) {
            sum += item.getSubtotal();
        }
        return sum;
    }

    public void setItems(List<RegisterItem> items) {
        this.items = items;
        this.total = calculateTotal();
    }

    public List<RegisterItem> getItems() {
        return items;
    }

    public double getTotal() {
        return total;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}