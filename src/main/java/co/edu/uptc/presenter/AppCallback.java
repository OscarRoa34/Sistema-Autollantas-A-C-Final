package co.edu.uptc.presenter;

import co.edu.uptc.models.products.Product;

public interface AppCallback {
    void onStockAlert(Product product, String alertLevel);

    void onDataChanged();
}