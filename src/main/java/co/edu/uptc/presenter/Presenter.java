package co.edu.uptc.presenter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import co.edu.uptc.models.products.*;
import co.edu.uptc.models.registers.*;
import co.edu.uptc.persistence.Persistence;
import co.edu.uptc.persistence.Persistence.RuntimeTypeAdapterFactory;
import co.edu.uptc.view.utils.PDFReportGenerator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.json.JSONObject;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

public class Presenter {
    private Persistence<Product> tiresPersistence;
    private Persistence<Product> lubFiltersPersistence;
    private Persistence<Product> batteriesPersistence;
    private Persistence<Product> brakePadsPersistence;
    private Persistence<Register> purchasesPersistence;
    private Persistence<Register> salesPersistence;
    private List<Product> tireInventory;
    private List<Product> lubFilterInventory;
    private List<Product> batteryInventory;
    private List<Product> brakePadInventory;
    private List<Register> purchaseHistory;
    private List<Register> saleHistory;
    private final Gson gsonConverter = new GsonBuilder().create();
    private AppCallback viewCallback;
    private int warningThreshold = 10;
    private int criticalThreshold = 3;

    public Presenter() {
        tireInventory = new ArrayList<>();
        lubFilterInventory = new ArrayList<>();
        batteryInventory = new ArrayList<>();
        brakePadInventory = new ArrayList<>();
        purchaseHistory = new ArrayList<>();
        saleHistory = new ArrayList<>();
        initializePersistence();
        loadInitialData();
    }

    public void setViewCallback(AppCallback callback) {
        this.viewCallback = callback;
    }

    private void initializePersistence() {
        RuntimeTypeAdapterFactory<Product> productAdapter = RuntimeTypeAdapterFactory
                .of(Product.class, "productType")
                .registerSubtype(Tire.class).registerSubtype(Lubricant.class)
                .registerSubtype(Filters.class).registerSubtype(Battery.class)
                .registerSubtype(BrakePad.class);
        Type productListType = new TypeToken<List<Product>>() {
        }.getType();
        tiresPersistence = new Persistence<>("data/tires.json", productListType, productAdapter);
        lubFiltersPersistence = new Persistence<>("data/lubricants_filters.json", productListType, productAdapter);
        batteriesPersistence = new Persistence<>("data/batteries.json", productListType, productAdapter);
        brakePadsPersistence = new Persistence<>("data/brakepads.json", productListType, productAdapter);
        RuntimeTypeAdapterFactory<Register> registerAdapter = RuntimeTypeAdapterFactory
                .of(Register.class, "registerType")
                .registerSubtype(PurchaseRegister.class)
                .registerSubtype(SellingRegister.class);
        Type registerListType = new TypeToken<List<Register>>() {
        }.getType();
        purchasesPersistence = new Persistence<>("data/purchases.json", registerListType, registerAdapter);
        salesPersistence = new Persistence<>("data/sales.json", registerListType, registerAdapter);
    }

    public void loadInitialData() {
        try {
            tireInventory = tiresPersistence.loadList();
            lubFilterInventory = lubFiltersPersistence.loadList();
            batteryInventory = batteriesPersistence.loadList();
            brakePadInventory = brakePadsPersistence.loadList();
            purchaseHistory = purchasesPersistence.loadList();
            saleHistory = salesPersistence.loadList();
        } catch (IOException e) {
            e.printStackTrace();
            if (tireInventory == null)
                tireInventory = new ArrayList<>();
            if (lubFilterInventory == null)
                lubFilterInventory = new ArrayList<>();
            if (batteryInventory == null)
                batteryInventory = new ArrayList<>();
            if (brakePadInventory == null)
                brakePadInventory = new ArrayList<>();
            if (purchaseHistory == null)
                purchaseHistory = new ArrayList<>();
            if (saleHistory == null)
                saleHistory = new ArrayList<>();
        }
    }

    private <T> boolean saveItemToList(List<T> list, T item, java.util.function.Predicate<T> idMatcher,
            Persistence<T> persistence) {
        Optional<T> existing = list.stream().filter(idMatcher).findFirst();
        if (existing.isPresent()) {
            int index = list.indexOf(existing.get());
            list.set(index, item);
        } else {
            list.add(0, item);
        }
        return saveListToPersistence(list, persistence);
    }

    private <T> boolean deleteItemFromList(List<T> list, java.util.function.Predicate<T> idMatcher,
            Persistence<T> persistence) {
        boolean removed = list.removeIf(idMatcher);
        if (removed) {
            return saveListToPersistence(list, persistence);
        }
        return false;
    }

    private <T> boolean saveListToPersistence(List<T> list, Persistence<T> persistence) {
        try {
            persistence.saveList(list);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Optional<Product> findProductById(String productId) {
        Optional<Product> product = tireInventory.stream().filter(p -> p.getId().equals(productId)).findFirst();
        if (product.isPresent())
            return product;
        product = lubFilterInventory.stream().filter(p -> p.getId().equals(productId)).findFirst();
        if (product.isPresent())
            return product;
        product = batteryInventory.stream().filter(p -> p.getId().equals(productId)).findFirst();
        if (product.isPresent())
            return product;
        product = brakePadInventory.stream().filter(p -> p.getId().equals(productId)).findFirst();
        return product;
    }

    public List<Product> getFullProductCatalog() {
        List<Product> catalog = new ArrayList<>();
        catalog.addAll(this.tireInventory);
        catalog.addAll(this.lubFilterInventory);
        catalog.addAll(this.batteryInventory);
        catalog.addAll(this.brakePadInventory);
        return catalog;
    }

    public List<Tire> requestTireList() {
        return tireInventory.stream()
                .filter(p -> p instanceof Tire)
                .map(p -> (Tire) p)
                .collect(Collectors.toList());
    }

    public boolean saveTire(Tire tire) {
        boolean saved = saveItemToList(
                tireInventory,
                tire,
                t -> t.getId().equals(tire.getId()),
                tiresPersistence);

        if (saved && viewCallback != null) {
            viewCallback.onDataChanged();
        }
        return saved;
    }

    private void deleteProductImage(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return;
        }
        try {
            Path pathToDelete = Paths.get(System.getProperty("user.dir"), imagePath);
            if (Files.exists(pathToDelete)) {
                Files.delete(pathToDelete);
            } else {
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean requestTireDeletion(String tireId) {
        Optional<Product> productOpt = tireInventory.stream()
                .filter(p -> p.getId().equals(tireId))
                .findFirst();
        if (productOpt.isEmpty()) {
            return false;
        }
        String imagePath = productOpt.get().getImagePath();
        boolean removed = tireInventory.removeIf(p -> p.getId().equals(tireId));
        if (removed) {
            boolean listSaved = saveListToPersistence(tireInventory, tiresPersistence);
            if (listSaved) {
                deleteProductImage(imagePath);
                if (viewCallback != null) {
                    viewCallback.onDataChanged();
                }
            } else {
                return false;
            }
            return true;
        }
        return false;
    }

    public List<Tire> filterTires(String searchText, List<String> brands, List<String> sizes, List<String> types) {
        return tireInventory.stream()
                .filter(p -> p instanceof Tire)
                .map(p -> (Tire) p)
                .filter(t -> (searchText == null || t.getName().toLowerCase().contains(searchText.toLowerCase()) ||
                        t.getBrand().toLowerCase().contains(searchText.toLowerCase())) &&
                        (brands == null || brands.isEmpty() || brands.contains(t.getBrand())) &&
                        (sizes == null || sizes.isEmpty() || sizes.contains(t.getSize())) &&
                        (types == null || types.isEmpty() || types.contains(t.getType())))
                .collect(Collectors.toList());
    }

    public List<Product> requestLubricantList() {
        return new ArrayList<>(lubFilterInventory);
    }

    public boolean saveLubricantOrFilter(Product product) {
        boolean saved = saveItemToList(
                lubFilterInventory,
                product,
                p -> p.getId().equals(product.getId()),
                lubFiltersPersistence);
        if (saved && viewCallback != null) {
            viewCallback.onDataChanged();
        }
        return saved;
    }

    public boolean requestLubricantDeletion(String productId) {
        Optional<Product> productOpt = lubFilterInventory.stream()
                .filter(p -> p.getId().equals(productId))
                .findFirst();
        if (productOpt.isEmpty())
            return false;
        String imagePath = productOpt.get().getImagePath();
        boolean removed = lubFilterInventory.removeIf(p -> p.getId().equals(productId));
        if (removed) {
            if (saveListToPersistence(lubFilterInventory, lubFiltersPersistence)) {
                deleteProductImage(imagePath);
                if (viewCallback != null) {
                    viewCallback.onDataChanged();
                }
            } else {
                return false;
            }
            return true;
        }
        return false;
    }

    public List<Product> filterLubricants(String searchText, List<String> brands) {
        return lubFilterInventory.stream()
                .filter(p -> (searchText == null || p.getName().toLowerCase().contains(searchText.toLowerCase()) ||
                        p.getBrand().toLowerCase().contains(searchText.toLowerCase())) &&
                        (brands == null || brands.isEmpty() || brands.contains(p.getBrand())))
                .collect(Collectors.toList());
    }

    public List<Battery> requestBatteryList() {
        return batteryInventory.stream()
                .filter(p -> p instanceof Battery)
                .map(p -> (Battery) p)
                .collect(Collectors.toList());
    }

    public boolean saveBattery(Battery battery) {
        boolean saved = saveItemToList(
                batteryInventory,
                battery,
                b -> b.getId().equals(battery.getId()),
                batteriesPersistence);
        if (saved && viewCallback != null) {
            viewCallback.onDataChanged();
        }
        return saved;
    }

    public boolean requestBatteryDeletion(String batteryId) {
        Optional<Product> productOpt = batteryInventory.stream()
                .filter(p -> p.getId().equals(batteryId))
                .findFirst();
        if (productOpt.isEmpty())
            return false;
        String imagePath = productOpt.get().getImagePath();
        boolean removed = batteryInventory.removeIf(p -> p.getId().equals(batteryId));
        if (removed) {
            if (saveListToPersistence(batteryInventory, batteriesPersistence)) {
                deleteProductImage(imagePath);
                if (viewCallback != null) {
                    viewCallback.onDataChanged();
                }
            } else {
                return false;
            }
            return true;
        }
        return false;
    }

    public List<Battery> filterBatteries(String searchText, List<String> brands) {
        return batteryInventory.stream()
                .filter(p -> p instanceof Battery)
                .map(p -> (Battery) p)
                .filter(b -> (searchText == null || b.getName().toLowerCase().contains(searchText.toLowerCase()) ||
                        b.getBrand().toLowerCase().contains(searchText.toLowerCase())) &&
                        (brands == null || brands.isEmpty() || brands.contains(b.getBrand())))
                .collect(Collectors.toList());
    }

    public List<BrakePad> requestBrakePadList() {
        return brakePadInventory.stream()
                .filter(p -> p instanceof BrakePad)
                .map(p -> (BrakePad) p)
                .collect(Collectors.toList());
    }

    public boolean saveBrakePad(BrakePad brakePad) {
        boolean saved = saveItemToList(
                brakePadInventory,
                brakePad,
                b -> b.getId().equals(brakePad.getId()),
                brakePadsPersistence);
        if (saved && viewCallback != null) {
            viewCallback.onDataChanged();
        }
        return saved;
    }

    public boolean requestBrakePadDeletion(String brakePadId) {
        Optional<Product> productOpt = brakePadInventory.stream()
                .filter(p -> p.getId().equals(brakePadId))
                .findFirst();
        if (productOpt.isEmpty())
            return false;
        String imagePath = productOpt.get().getImagePath();
        boolean removed = brakePadInventory.removeIf(p -> p.getId().equals(brakePadId));
        if (removed) {
            if (saveListToPersistence(brakePadInventory, brakePadsPersistence)) {
                deleteProductImage(imagePath);
                if (viewCallback != null) {
                    viewCallback.onDataChanged();
                }
            } else {
                return false;
            }
            return true;
        }
        return false;
    }

    public List<BrakePad> filterBrakePads(String searchText, List<String> brands) {
        return brakePadInventory.stream()
                .filter(p -> p instanceof BrakePad)
                .map(p -> (BrakePad) p)
                .filter(b -> (searchText == null || b.getName().toLowerCase().contains(searchText.toLowerCase()) ||
                        b.getBrand().toLowerCase().contains(searchText.toLowerCase())) &&
                        (brands == null || brands.isEmpty() || brands.contains(b.getBrand())))
                .collect(Collectors.toList());
    }

    public List<SellingRegister> requestSalesHistory() {
        return saleHistory.stream()
                .filter(r -> r instanceof SellingRegister)
                .map(r -> (SellingRegister) r)
                .collect(Collectors.toList());
    }

    public List<JSONObject> getSalesHistoryAsJson() {
        List<JSONObject> jsonList = new ArrayList<>();
        List<SellingRegister> sales = requestSalesHistory();
        if (sales != null) {
            for (SellingRegister sale : sales) {
                try {
                    String jsonString = gsonConverter.toJson(sale);
                    JSONObject jsonObject = new JSONObject(jsonString);
                    jsonList.add(jsonObject);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return jsonList;
    }

    public boolean registerNewSale(SellingRegister sale) {
        Optional<Register> oldRegisterOpt = saleHistory.stream()
                .filter(r -> r.getInvoiceNumber().equals(sale.getInvoiceNumber()))
                .findFirst();

        if (oldRegisterOpt.isPresent()) {
            SellingRegister oldSale = (SellingRegister) oldRegisterOpt.get();
            revertStockFromSale(oldSale, false);
        }
        boolean saved = saveItemToList(saleHistory, sale,
                r -> r.getInvoiceNumber().equals(sale.getInvoiceNumber()),
                salesPersistence);

        if (saved) {
            updateStockFromSale(sale, true);
            if (viewCallback != null) {
                viewCallback.onDataChanged();
            }
        } else if (oldRegisterOpt.isPresent()) {
            updateStockFromSale((SellingRegister) oldRegisterOpt.get(), true);
        }
        return saved;
    }

    public SellingRegister requestSaleDetails(String saleId) {
        return saleHistory.stream()
                .filter(r -> r instanceof SellingRegister && r.getInvoiceNumber().equals(saleId))
                .map(r -> (SellingRegister) r)
                .findFirst()
                .orElse(null);
    }

    public boolean requestSaleCancellation(String saleId) {
        SellingRegister saleToCancel = requestSaleDetails(saleId);
        if (saleToCancel == null)
            return false;

        boolean deleted = deleteItemFromList(saleHistory,
                r -> r.getInvoiceNumber().equals(saleId),
                salesPersistence);
        if (deleted) {
            revertStockFromSale(saleToCancel, true);
            if (viewCallback != null) {
                viewCallback.onDataChanged();
            }
        }
        return deleted;
    }

    public String getNextSaleInvoiceNumber() {
        String prefix = "FV-";
        Pattern pattern = Pattern.compile("^" + Pattern.quote(prefix) + "(\\d+)$");
        int maxNum = 0;
        if (saleHistory != null) {
            for (Register reg : saleHistory) {
                Matcher matcher = pattern.matcher(reg.getInvoiceNumber());
                if (matcher.matches()) {
                    try {
                        int currentNum = Integer.parseInt(matcher.group(1));
                        if (currentNum > maxNum) {
                            maxNum = currentNum;
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }
        int nextNum = maxNum + 1;
        return String.format("%s%04d", prefix, nextNum);
    }

    public List<PurchaseRegister> requestPurchasesHistory() {
        return purchaseHistory.stream()
                .filter(r -> r instanceof PurchaseRegister)
                .map(r -> (PurchaseRegister) r)
                .collect(Collectors.toList());
    }

    public List<JSONObject> getPurchasesHistoryAsJson() {
        List<JSONObject> jsonList = new ArrayList<>();
        List<PurchaseRegister> purchases = requestPurchasesHistory();
        if (purchases != null) {
            for (PurchaseRegister purchase : purchases) {
                try {
                    String jsonString = gsonConverter.toJson(purchase);
                    JSONObject jsonObject = new JSONObject(jsonString);
                    jsonList.add(jsonObject);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return jsonList;
    }

    public boolean registerNewPurchase(PurchaseRegister purchase) {
        Optional<Register> oldRegisterOpt = purchaseHistory.stream()
                .filter(r -> r.getInvoiceNumber().equals(purchase.getInvoiceNumber()))
                .findFirst();
        if (oldRegisterOpt.isPresent()) {
            PurchaseRegister oldPurchase = (PurchaseRegister) oldRegisterOpt.get();
            revertStockFromPurchase(oldPurchase, false);
        }
        boolean saved = saveItemToList(purchaseHistory, purchase,
                r -> r.getInvoiceNumber().equals(purchase.getInvoiceNumber()),
                purchasesPersistence);
        if (saved) {
            updateStockFromPurchase(purchase, true);
            if (viewCallback != null) {
                viewCallback.onDataChanged();
            }
        } else if (oldRegisterOpt.isPresent()) {
            updateStockFromPurchase((PurchaseRegister) oldRegisterOpt.get(), true);
        }
        return saved;
    }

    public PurchaseRegister requestPurchaseDetails(String purchaseId) {
        return purchaseHistory.stream()
                .filter(r -> r instanceof PurchaseRegister && r.getInvoiceNumber().equals(purchaseId))
                .map(r -> (PurchaseRegister) r)
                .findFirst()
                .orElse(null);
    }

    public boolean requestPurchaseCancellation(String purchaseId) {
        PurchaseRegister purchaseToCancel = requestPurchaseDetails(purchaseId);
        if (purchaseToCancel == null)
            return false;

        boolean deleted = deleteItemFromList(purchaseHistory,
                r -> r.getInvoiceNumber().equals(purchaseId),
                purchasesPersistence);
        if (deleted) {
            revertStockFromPurchase(purchaseToCancel, true);
            if (viewCallback != null) {
                viewCallback.onDataChanged();
            }
        }
        return deleted;
    }

    public String getNextPurchaseInvoiceNumber() {
        String prefix = "RC-";
        Pattern pattern = Pattern.compile("^" + Pattern.quote(prefix) + "(\\d+)$");
        int maxNum = 0;
        if (purchaseHistory != null) {
            for (Register reg : purchaseHistory) {
                Matcher matcher = pattern.matcher(reg.getInvoiceNumber());
                if (matcher.matches()) {
                    try {
                        int currentNum = Integer.parseInt(matcher.group(1));
                        if (currentNum > maxNum) {
                            maxNum = currentNum;
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }
        }
        int nextNum = maxNum + 1;
        return String.format("%s%04d", prefix, nextNum);
    }

    private void updateStockFromSale(SellingRegister sale, boolean save) {
        if (sale == null || sale.getItems() == null)
            return;
        boolean inventoryChanged = false;
        for (RegisterItem item : sale.getItems()) {
            Optional<Product> productOpt = findProductById(item.getProductId());
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                int oldStock = product.getstock();
                int newStock = oldStock - item.getQuantity();
                product.setstock(newStock);
                inventoryChanged = true;
                checkAndNotifyStockAlert(product, oldStock, newStock);
            }
        }
        if (inventoryChanged && save) {
            saveAllInventories();
        }
    }

    private void revertStockFromSale(SellingRegister canceledSale, boolean save) {
        if (canceledSale == null || canceledSale.getItems() == null)
            return;
        boolean inventoryChanged = false;
        for (RegisterItem item : canceledSale.getItems()) {
            Optional<Product> productOpt = findProductById(item.getProductId());
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                int oldStock = product.getstock();
                int newStock = oldStock + item.getQuantity();
                product.setstock(newStock);
                inventoryChanged = true;
                checkAndNotifyStockAlert(product, oldStock, newStock);
            }
        }
        if (inventoryChanged && save) {
            saveAllInventories();
        }
    }

    private void updateStockFromPurchase(PurchaseRegister purchase, boolean save) {
        if (purchase == null || purchase.getItems() == null)
            return;
        boolean inventoryChanged = false;
        for (RegisterItem item : purchase.getItems()) {
            Optional<Product> productOpt = findProductById(item.getProductId());
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                int oldStock = product.getstock();
                int newStock = oldStock + item.getQuantity();
                product.setstock(newStock);
                inventoryChanged = true;
                checkAndNotifyStockAlert(product, oldStock, newStock);
            }
        }
        if (inventoryChanged && save) {
            saveAllInventories();
        }
    }

    private void revertStockFromPurchase(PurchaseRegister canceledPurchase, boolean save) {
        if (canceledPurchase == null || canceledPurchase.getItems() == null)
            return;
        boolean inventoryChanged = false;
        for (RegisterItem item : canceledPurchase.getItems()) {
            Optional<Product> productOpt = findProductById(item.getProductId());
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                int oldStock = product.getstock();
                int newStock = oldStock - item.getQuantity();
                product.setstock(newStock);
                inventoryChanged = true;
                checkAndNotifyStockAlert(product, oldStock, newStock);
            }
        }
        if (inventoryChanged && save) {
            saveAllInventories();
        }
    }

    private void saveAllInventories() {
        try {
            tiresPersistence.saveList(tireInventory);
            lubFiltersPersistence.saveList(lubFilterInventory);
            batteriesPersistence.saveList(batteryInventory);
            brakePadsPersistence.saveList(brakePadInventory);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkAndNotifyStockAlert(Product product, int oldStock, int newStock) {
        if (viewCallback == null)
            return;
        if (newStock <= criticalThreshold && oldStock > criticalThreshold) {
            viewCallback.onStockAlert(product, "critical");
        } else if (newStock <= warningThreshold && oldStock > warningThreshold) {
            viewCallback.onStockAlert(product, "warning");
        }
    }

    public void setAlertThresholds(int warning, int critical) {
        this.warningThreshold = warning;
        this.criticalThreshold = critical;
        if (viewCallback != null) {
            viewCallback.onDataChanged();
        }
    }

    public List<Product> requestLowStockAlerts() {
        List<Product> allProducts = new ArrayList<>();
        allProducts.addAll(tireInventory);
        allProducts.addAll(lubFilterInventory);
        allProducts.addAll(batteryInventory);
        allProducts.addAll(brakePadInventory);
        return allProducts.stream()
                .filter(p -> p != null && p.getstock() <= this.warningThreshold)
                .collect(Collectors.toList());
    }

    public void generateSalesReportToFile(Date startDate, Date endDate, String outputPath) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        List<SellingRegister> filteredSales = requestSalesHistory().stream()
                .filter(sale -> {
                    try {
                        Date saleDate = sdf.parse(sale.getDate());
                        return !saleDate.before(startDate) && !saleDate.after(endDate);
                    } catch (Exception e) {
                        return false;
                    }
                }).collect(Collectors.toList());
        List<String> headers = List.of("Fecha", "Producto", "Marca", "Cantidad", "Precio Unitario", "Total");
        List<List<String>> rows = filteredSales.stream()
                .flatMap(sale -> sale.getItems().stream()
                        .map(item -> {
                            Product p = findProductById(item.getProductId()).orElse(null);
                            if (p != null) {
                                return List.of(
                                        sale.getDate(),
                                        p.getName(),
                                        p.getBrand(),
                                        String.valueOf(item.getQuantity()),
                                        String.format("$%.2f", item.getUnitPrice()),
                                        String.format("$%.2f", item.getQuantity() * item.getUnitPrice()));
                            } else
                                return null;
                        }))
                .filter(r -> r != null)
                .toList();

        try {
            PDFReportGenerator.generarReporteConTabla(
                    "data/Plantilla Autollantas.pdf",
                    outputPath,
                    String.format("Reporte de Ventas (%tF a %tF)", startDate, endDate),
                    headers,
                    rows);
            System.out.println("Reporte de ventas generado en: " + outputPath);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error al generar el reporte de ventas: " + e.getMessage());
        }
    }

    public void generateInventoryValuationReportToFile(String outputPath) {
        try {
            List<Product> allProducts = getFullProductCatalog();
            List<String> headers = List.of("Producto", "Marca", "Stock", "Precio Unitario", "Valor Total");
            List<List<String>> rows = allProducts.stream()
                    .map(p -> List.of(
                            p.getName(),
                            p.getBrand(),
                            String.valueOf(p.getstock()),
                            String.format("$%.2f", p.getPrice()),
                            String.format("$%.2f", p.getstock() * p.getPrice())))
                    .toList();
            PDFReportGenerator.generarReporteConTabla(
                    "data/Plantilla Autollantas.pdf",
                    outputPath,
                    "Valoración del Inventario",
                    headers,
                    rows);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void generateTopSellingProductsReportToFile(int limit, String outputPath) {
        try {
            List<SellingRegister> allSales = requestSalesHistory();
            if (allSales.isEmpty())
                return;

            var productSalesCount = allSales.stream()
                    .flatMap(sale -> sale.getItems().stream())
                    .collect(Collectors.groupingBy(
                            RegisterItem::getProductId,
                            Collectors.summingInt(RegisterItem::getQuantity)));

            List<Product> topProducts = productSalesCount.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .limit(limit)
                    .map(entry -> findProductById(entry.getKey()).orElse(null))
                    .filter(p -> p != null)
                    .toList();

            List<String> headers = List.of("Rank", "Producto", "Marca", "Vendidas");
            List<List<String>> rows = topProducts.stream().map(p -> {
                int sold = productSalesCount.getOrDefault(p.getId(), 0);
                int rank = topProducts.indexOf(p) + 1;
                return List.of(String.valueOf(rank), p.getName(), p.getBrand(), String.valueOf(sold));
            }).toList();

            PDFReportGenerator.generarReporteConTabla(
                    "data/Plantilla Autollantas.pdf",
                    outputPath,
                    "Top Productos Vendidos",
                    headers,
                    rows);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean authenticateUser(String securityCode) {
        final String ADMIN_CODE = "autoadmin";
        return securityCode.equals(ADMIN_CODE);
    }
}