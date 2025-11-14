package co.edu.uptc.view.panels.SubPanels;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

import co.edu.uptc.presenter.Presenter;
import co.edu.uptc.models.products.Product;

import co.edu.uptc.view.GlobalView;
import co.edu.uptc.view.utils.ViewController;

public class ViewPurchaseDetailsPanel extends JPanel {

    private JLabel lblTotalValue;
    private DefaultTableModel itemsModel;
    private JTable itemsTable;

    private JPanel parentPanel;
    private ViewController controller;
    private Presenter presenter;
    private JSONObject purchaseData;
    private List<Product> productCatalog;
    private final DecimalFormat currencyFormatter;

    public ViewPurchaseDetailsPanel(JPanel parentPanel, ViewController controller,
            Presenter presenter, JSONObject purchaseData) {

        NumberFormat nf = NumberFormat.getInstance(new Locale("es", "CO"));
        nf.setGroupingUsed(true);
        this.currencyFormatter = (DecimalFormat) nf;
        this.currencyFormatter.applyPattern("$ #,##0");

        this.parentPanel = parentPanel;
        this.controller = controller;
        this.presenter = presenter;
        this.purchaseData = purchaseData;
        this.productCatalog = presenter.getFullProductCatalog();

        initComponents();
        loadData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(20, 20));
        setBackground(GlobalView.GENERAL_BACKGROUND);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        add(createTitle("Detalles del Registro de Compra"), BorderLayout.NORTH);

        JPanel masterPanel = createMasterInfoPanel();
        masterPanel.setPreferredSize(new Dimension(450, 0));
        add(masterPanel, BorderLayout.WEST);

        JPanel itemsPanel = createItemsDetailPanel();
        add(itemsPanel, BorderLayout.CENTER);

        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    private JLabel createTitle(String titleText) {
        JLabel title = new JLabel(titleText);
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(Color.BLACK);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setBorder(new EmptyBorder(0, 0, 20, 0));
        return title;
    }

    private JPanel createMasterInfoPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(GlobalView.BORDER_COLOR, 1, true),
                " Datos de la Compra ", TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, GlobalView.TABLE_HEADER_FONT,
                GlobalView.TEXT_COLOR));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        formPanel.add(createLabel("Registro Nº:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.9;
        formPanel.add(createValueLabel(purchaseData.optString("invoiceNumber")), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(createLabel("Proveedor:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        formPanel.add(createValueLabel(purchaseData.optString("supplierName")), gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(createLabel("Fecha:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        formPanel.add(createValueLabel(purchaseData.optString("date")), gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(createLabel("Estado:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 3;
        formPanel.add(createValueLabel(purchaseData.optString("status")), gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weighty = 1.0;
        formPanel.add(new JPanel() {
            {
                setOpaque(false);
            }
        }, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        JLabel totalLabel = createLabel("TOTAL:");
        totalLabel.setFont(GlobalView.TITLE_FONT.deriveFont(20f));
        formPanel.add(totalLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.SOUTHEAST;
        lblTotalValue = new JLabel(formatCurrency(purchaseData.optLong("total")));
        lblTotalValue.setFont(GlobalView.TITLE_FONT.deriveFont(Font.BOLD, 32f));
        lblTotalValue.setForeground(GlobalView.CONFIRM_BUTTON_BACKGROUND.darker());
        lblTotalValue.setHorizontalAlignment(SwingConstants.RIGHT);
        formPanel.add(lblTotalValue, gbc);

        return formPanel;
    }

    private JPanel createItemsDetailPanel() {
        JPanel itemsPanel = new JPanel(new BorderLayout(10, 10));
        itemsPanel.setOpaque(false);
        itemsPanel.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(GlobalView.BORDER_COLOR, 1, true),
                " Ítems Incluidos ", TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, GlobalView.TABLE_HEADER_FONT,
                GlobalView.TEXT_COLOR));

        String[] itemColumns = { "ID", "Nombre", "Cantidad", "Costo", "Subtotal" };
        itemsModel = new DefaultTableModel(itemColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        itemsTable = new JTable(itemsModel);
        setupItemsTableStyle();

        JScrollPane scrollPane = new JScrollPane(itemsTable);
        scrollPane.setBorder(new LineBorder(GlobalView.BORDER_COLOR, 1));
        itemsPanel.add(scrollPane, BorderLayout.CENTER);

        return itemsPanel;
    }

    private void loadData() {
        JSONArray items = purchaseData.optJSONArray("items");
        if (items == null)
            return;

        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            String prodId = item.optString("productId");
            int qty = item.optInt("quantity");
            double price = item.optDouble("unitPrice");

            Product product = findProductInCatalog(prodId);
            String prodName = (product != null) ? product.getName() : "Producto Desconocido";

            itemsModel.addRow(new Object[] {
                    prodId,
                    prodName,
                    qty,
                    formatCurrency((long) price),
                    formatCurrency((long) (qty * price))
            });
        }
    }

    private Product findProductInCatalog(String productId) {
        if (productCatalog == null)
            return null;
        return productCatalog.stream()
                .filter(p -> p.getId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    private void setupItemsTableStyle() {
        itemsTable.setRowHeight(35);
        itemsTable.setFont(GlobalView.TABLE_BODY_FONT);
        itemsTable.setSelectionBackground(GlobalView.TABLE_SELECTION_BACKGROUND);
        itemsTable.setSelectionForeground(GlobalView.TABLE_SELECTION_FOREGROUND);
        itemsTable.setGridColor(GlobalView.BORDER_COLOR);
        itemsTable.getTableHeader().setFont(GlobalView.TABLE_HEADER_FONT);
        itemsTable.getTableHeader().setBackground(GlobalView.TABLE_HEADER_BACKGROUND);
        itemsTable.getTableHeader().setForeground(GlobalView.TABLE_HEADER_FOREGROUND);
        itemsTable.getTableHeader().setReorderingAllowed(false);

        TableColumnModel cm = itemsTable.getColumnModel();
        cm.getColumn(0).setPreferredWidth(100);
        cm.getColumn(1).setPreferredWidth(300);
        cm.getColumn(2).setPreferredWidth(80);
        cm.getColumn(3).setPreferredWidth(120);
        cm.getColumn(4).setPreferredWidth(120);
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lbl.setForeground(Color.BLACK);
        return lbl;
    }

    private JLabel createValueLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        lbl.setForeground(Color.BLACK);
        return lbl;
    }

    private String formatCurrency(long amount) {
        try {
            return currencyFormatter.format(amount);
        } catch (Exception e) {
            return "$ 0";
        }
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 20));
        buttonPanel.setBackground(GlobalView.GENERAL_BACKGROUND);
        JButton saveBtn = createStyledButton("Aceptar", GlobalView.CONFIRM_BUTTON_BACKGROUND,
                GlobalView.CONFIRM_BUTTON_BACKGROUND.darker());

        saveBtn.addActionListener(e -> controller.showPanel(parentPanel));

        buttonPanel.add(saveBtn);
        return buttonPanel;
    }

    private JButton createStyledButton(String text, Color background, Color hoverBackground) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(160, 50));
        button.setFont(new Font("Segoe UI", Font.BOLD, 18));
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverBackground);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(background);
            }
        });
        return button;
    }
}