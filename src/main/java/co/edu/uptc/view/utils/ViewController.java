package co.edu.uptc.view.utils;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.function.Supplier;
import co.edu.uptc.presenter.AppCallback;
import co.edu.uptc.models.products.Product;
import co.edu.uptc.view.dialogs.ErrorPopUp;
import co.edu.uptc.view.dialogs.WarningPopUp;
import co.edu.uptc.presenter.Presenter;
import co.edu.uptc.view.panels.*;
import co.edu.uptc.view.panels.SubPanels.*;

public class ViewController implements AppCallback {
    private final JPanel container;
    private Presenter presenter;
    private HashMap<String, JPanel> panelCache;

    public ViewController(JPanel container, Presenter presenter) {
        this.container = container;
        this.presenter = presenter;
        this.panelCache = new HashMap<>();
        this.container.setLayout(new BorderLayout());
    }

    public void showPanel(JPanel newPanel) {
        container.removeAll();
        container.add(newPanel, BorderLayout.CENTER);
        container.revalidate();
        container.repaint();
    }

    public void showCachedPanel(String panelKey) {
        JPanel panelToShow = panelCache.get(panelKey);

        if (panelToShow != null) {
            showPanel(panelToShow);
        } else {

            LoadingPanel loading = new LoadingPanel();
            loading.setLoadingText("Cargando módulo...");
            showPanel(loading);

            Supplier<JPanel> panelSupplier = () -> createNewPanel(panelKey);

            SwingWorker<JPanel, Void> worker = new SwingWorker<>() {
                @Override
                protected JPanel doInBackground() {
                    return panelSupplier.get();
                }

                @Override
                protected void done() {
                    try {
                        JPanel newPanel = get();
                        panelCache.put(panelKey, newPanel);
                        showPanel(newPanel);
                    } catch (Exception e) {
                        e.printStackTrace();
                        showPanel(createNewPanel("WELCOME"));
                        ErrorPopUp.showErrorPopup((Frame) SwingUtilities.getWindowAncestor(container),
                                "Error de Carga", "No se pudo cargar el módulo.");
                    }
                }
            };
            worker.execute();
        }
    }

    private JPanel createNewPanel(String panelKey) {
        switch (panelKey) {

            case "WELCOME":
                return new WelcomePanel();
            case "PRODUCTS":
                return new ProductsPanel(this, presenter);
            case "PURCHASES":
                return new PurchasePanel(this, presenter);
            case "SALES":
                return new SalesPanel(this, presenter);
            case "REPORTS":
                return new ReportsPanel(presenter);

            case "TIRES":
                return new TiresProductPanel(this, presenter);
            case "LUB_FILTERS":
                return new LubFiltersProductPanel(this, presenter);
            case "BATTERIES":
                return new BateryProductPanel(this, presenter);
            case "BRAKES":
                return new BrakepadsProductPanel(this, presenter);

            default:
                return new WelcomePanel();
        }
    }

    @Override
    public void onStockAlert(Product product, String alertLevel) {
        SwingUtilities.invokeLater(() -> {
            Window window = SwingUtilities.getWindowAncestor(container);
            Frame parentFrame = (window instanceof Frame) ? (Frame) window : null;
            String title, message;
            if ("critical".equals(alertLevel)) {
                title = "Alerta de Stock Crítico";
                message = "El producto '" + product.getName() + "' está agotado o en nivel crítico ("
                        + product.getstock() + ").";
                ErrorPopUp.showErrorPopup(parentFrame, title, message);
            } else if ("warning".equals(alertLevel)) {
                title = "Alerta de Stock Bajo";
                message = "El producto '" + product.getName() + "' tiene stock bajo (" + product.getstock() + ").";
                WarningPopUp.showWarningPopup(parentFrame, title, message);
            }
        });
    }

    @Override
    public void onDataChanged() {
        System.out
                .println("ViewController: Recibida notificación de cambio de datos. Refrescando paneles cacheados...");

        for (JPanel panel : panelCache.values()) {

            if (panel instanceof TiresProductPanel) {
                ((TiresProductPanel) panel).refreshData();
            } else if (panel instanceof BrakepadsProductPanel) {
                ((BrakepadsProductPanel) panel).refreshData();
            } else if (panel instanceof BateryProductPanel) {
                ((BateryProductPanel) panel).refreshData();
            } else if (panel instanceof LubFiltersProductPanel) {
                ((LubFiltersProductPanel) panel).refreshData();
            } else if (panel instanceof ReportsPanel) {
                ((ReportsPanel) panel).refreshData();
            }
        }
    }
}