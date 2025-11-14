package co.edu.uptc.view.panels.SubPanels;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.text.JTextComponent;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import com.toedter.calendar.JDateChooser;

import co.edu.uptc.presenter.Presenter;
import co.edu.uptc.models.products.Product;
import co.edu.uptc.models.registers.SellingRegister;
import co.edu.uptc.models.registers.RegisterItem;

import co.edu.uptc.view.GlobalView;
import co.edu.uptc.view.dialogs.ConfirmDialog;
import co.edu.uptc.view.dialogs.SuccessPopUp;
import co.edu.uptc.view.utils.ViewController;
import co.edu.uptc.view.components.SearchableProductField;

public class EditCreateSalesPanel extends JPanel {

    private JLabel lblFacturaValue;
    private JTextField txtCustomer;
    private JComboBox<String> cmbEstado;
    private JDateChooser dateChooser;
    private JLabel lblTotalValue;
    private SearchableProductField productSearchField;
    private JSpinner spnCantidad;
    private JLabel lblPrecioUnitario;
    private JButton btnAnadirItem;
    private JButton btnQuitarItem;
    private JTable itemsTable;
    private DefaultTableModel itemsModel;
    private Consumer<JSONObject> onSaveCallback;
    private JPanel parentPanel;
    private ViewController controller;
    private Presenter presenter;
    private boolean isCreateMode;
    private String currentInvoiceNumber;
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd-MM-yyyy");
    private final SimpleDateFormat jsonDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private List<Product> productCatalog;
    private final DecimalFormat currencyFormatter;

    private static final int MAX_CUSTOMER_NAME_LENGTH = 40;
    private static final Pattern CUSTOMER_NAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9\\s.'-ñÑáéíóúÁÉÍÓÚüÜ]+$");
    private static final int MIN_SPINNER_QTY = 1;
    private static final int MAX_SPINNER_QTY = 999;

    public EditCreateSalesPanel(String titleText, JSONObject saleData, Consumer<JSONObject> onSaveCallback,
            JPanel parentPanel, ViewController controller, Presenter presenter) {

        NumberFormat nf = NumberFormat.getInstance(new Locale("es", "CO"));
        nf.setGroupingUsed(true);
        this.currencyFormatter = (DecimalFormat) nf;
        this.currencyFormatter.applyPattern("$ #,##0");

        this.onSaveCallback = onSaveCallback;
        this.parentPanel = parentPanel;
        this.controller = controller;
        this.presenter = presenter;
        this.productCatalog = presenter.getFullProductCatalog();

        String invoiceNum = saleData.optString("invoiceNumber");
        this.isCreateMode = invoiceNum.isEmpty();

        if (isCreateMode) {
            this.currentInvoiceNumber = presenter.getNextSaleInvoiceNumber();
        } else {
            this.currentInvoiceNumber = invoiceNum;
        }

        initComponents(titleText, saleData);

        if (!isCreateMode) {
            loadItemsIntoTable(saleData);
        }
        updateTotalFromTable();
    }

    private void initComponents(String titleText, JSONObject saleData) {
        setLayout(new BorderLayout(20, 20));
        setBackground(GlobalView.GENERAL_BACKGROUND);
        setBorder(new EmptyBorder(30, 40, 30, 40));
        add(createTitle(titleText), BorderLayout.NORTH);
        JPanel masterPanel = createMasterFormPanel(saleData);
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

    private JPanel createMasterFormPanel(JSONObject saleData) {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(GlobalView.BORDER_COLOR, 1, true),
                " Datos de la Venta ",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                GlobalView.TABLE_HEADER_FONT,
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
        lblFacturaValue = new JLabel(this.currentInvoiceNumber);
        lblFacturaValue.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblFacturaValue.setForeground(Color.DARK_GRAY);
        formPanel.add(lblFacturaValue, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(createLabel("Cliente:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        txtCustomer = createStyledField(saleData.optString("customerName", ""));
        addStandardFocusListener(txtCustomer);
        formPanel.add(txtCustomer, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(createLabel("Fecha:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        dateChooser = createStyledDateChooser(saleData.optString("date"));
        addStandardFocusListener((JComponent) dateChooser.getDateEditor().getUiComponent());
        formPanel.add(dateChooser, gbc);
        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(createLabel("Estado:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 3;
        String[] estados = { "Pagada", "Pendiente", "Anulada" };
        cmbEstado = createStyledComboBox(estados);
        cmbEstado.setSelectedItem(saleData.optString("status", "Pagada"));
        formPanel.add(cmbEstado, gbc);
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
        lblTotalValue = new JLabel(formatCurrency(0L));
        lblTotalValue.setFont(GlobalView.TITLE_FONT.deriveFont(Font.BOLD, 32f));
        lblTotalValue.setForeground(GlobalView.CONFIRM_BUTTON_BACKGROUND.darker());
        lblTotalValue.setHorizontalAlignment(SwingConstants.RIGHT);
        formPanel.add(lblTotalValue, gbc);
        int standardHeight = txtCustomer.getPreferredSize().height;
        dateChooser.setPreferredSize(new Dimension(dateChooser.getPreferredSize().width, standardHeight));
        cmbEstado.setPreferredSize(new Dimension(cmbEstado.getPreferredSize().width, standardHeight));
        return formPanel;
    }

    private JPanel createItemsDetailPanel() {
        JPanel itemsPanel = new JPanel(new BorderLayout(10, 10));
        itemsPanel.setOpaque(false);
        itemsPanel.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(GlobalView.BORDER_COLOR, 1, true),
                " Ítems de la Venta ", TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, GlobalView.TABLE_HEADER_FONT,
                GlobalView.TEXT_COLOR));
        itemsPanel.add(createAddItemsSubPanel(), BorderLayout.NORTH);
        String[] itemColumns = { "ID Producto", "Nombre", "Cantidad", "Precio Unitario" };
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
        btnQuitarItem = new JButton("Quitar Ítem Seleccionado");
        btnQuitarItem.setFont(GlobalView.BUTTON_FONT.deriveFont(14f));
        btnQuitarItem.setBackground(GlobalView.CANCEL_BUTTON_BACKGROUND);
        btnQuitarItem.setForeground(Color.WHITE);
        btnQuitarItem.addActionListener(e -> quitarItemSeleccionado());
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.setOpaque(false);
        southPanel.add(btnQuitarItem);
        itemsPanel.add(southPanel, BorderLayout.SOUTH);
        return itemsPanel;
    }

    private JPanel createAddItemsSubPanel() {
        JPanel addPanel = new JPanel(new GridBagLayout());
        addPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        addPanel.add(createLabel("Buscar Producto:"), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.6;
        productSearchField = new SearchableProductField(productCatalog);
        addPanel.add(productSearchField, gbc);
        int standardHeight = productSearchField.getPreferredSize().height;
        productSearchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                Product p = productSearchField.getSelectedProduct();
                if (p != null) {
                    lblPrecioUnitario.setText(formatCurrency((long) p.getPrice()));
                } else {
                    lblPrecioUnitario.setText(formatCurrency(0L));
                }
            }
        });

        gbc.gridx = 1;
        gbc.gridy = 0;
        addPanel.add(createLabel("Cantidad:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.1;
        spnCantidad = new JSpinner(new SpinnerNumberModel(MIN_SPINNER_QTY, MIN_SPINNER_QTY, MAX_SPINNER_QTY, 1));

        int preferredWidth = spnCantidad.getPreferredSize().width;

        Dimension fixedHeightSize = new Dimension(preferredWidth, standardHeight);

        spnCantidad.setPreferredSize(fixedHeightSize);
        spnCantidad.setMinimumSize(fixedHeightSize);
        spnCantidad.setMaximumSize(fixedHeightSize);

        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        addPanel.add(spnCantidad, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JFormattedTextField spinnerTextField = ((JSpinner.DefaultEditor) spnCantidad.getEditor()).getTextField();
        spinnerTextField.setFocusLostBehavior(JFormattedTextField.PERSIST);

        gbc.gridx = 2;
        gbc.gridy = 0;
        addPanel.add(createLabel("Precio Unitario:"), gbc);

        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        lblPrecioUnitario = new JLabel(formatCurrency(0L));
        lblPrecioUnitario.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblPrecioUnitario.setForeground(Color.BLACK);
        lblPrecioUnitario.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 200, 200), 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        lblPrecioUnitario.setBackground(new Color(230, 230, 230));
        lblPrecioUnitario.setOpaque(true);
        lblPrecioUnitario.setPreferredSize(new Dimension(lblPrecioUnitario.getPreferredSize().width, standardHeight));
        addPanel.add(lblPrecioUnitario, gbc);

        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        btnAnadirItem = new JButton("Añadir");
        btnAnadirItem.setFont(GlobalView.BUTTON_FONT.deriveFont(Font.BOLD, 14f));
        btnAnadirItem.setBackground(GlobalView.ASIDE_BACKGROUND);
        btnAnadirItem.setForeground(Color.WHITE);
        btnAnadirItem.addActionListener(e -> anadirItemATabla());
        btnAnadirItem.setPreferredSize(new Dimension(btnAnadirItem.getPreferredSize().width, standardHeight));
        addPanel.add(btnAnadirItem, gbc);

        return addPanel;
    }

    private void loadItemsIntoTable(JSONObject saleData) {
        JSONArray items = saleData.optJSONArray("items");
        if (items == null)
            return;
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            String prodId = item.optString("productId");
            int qty = item.optInt("quantity");
            double price = item.optDouble("unitPrice");
            Product product = findProductInCatalog(prodId);
            String prodName = (product != null) ? product.getName() : "Producto Desconocido";
            itemsModel.addRow(new Object[] { prodId, prodName, qty, price });
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

    private void anadirItemATabla() {
        Product selectedProduct = productSearchField.getSelectedProduct();
        if (selectedProduct == null) {
            showSimpleError("Debe seleccionar un producto válido de la lista.");
            return;
        }

        String qtyText = ((JSpinner.DefaultEditor) spnCantidad.getEditor()).getTextField().getText().trim();
        int quantityToAdd;
        try {
            quantityToAdd = Integer.parseInt(qtyText);
        } catch (NumberFormatException e) {
            showSimpleError("La cantidad debe ser un número válido.");
            return;
        }
        if (quantityToAdd < MIN_SPINNER_QTY || quantityToAdd > MAX_SPINNER_QTY) {
            String msg = String.format("La cantidad debe estar entre %d y %d.", MIN_SPINNER_QTY, MAX_SPINNER_QTY);
            showSimpleError(msg);
            return;
        }

        int totalStock = selectedProduct.getstock();
        String selectedProductId = selectedProduct.getId();

        int quantityInCart = 0;
        for (int i = 0; i < itemsModel.getRowCount(); i++) {
            String productIdInRow = (String) itemsModel.getValueAt(i, 0);
            if (productIdInRow.equals(selectedProductId)) {
                quantityInCart += (Integer) itemsModel.getValueAt(i, 2);
            }
        }

        int availableStock = totalStock - quantityInCart;

        if (quantityToAdd > availableStock) {
            String errorMsg;
            if (quantityInCart > 0) {

                errorMsg = String.format(
                        "No se puede añadir %d. Stock total: %d.\n" +
                                "Ya tiene %d en el carrito. Solo quedan %d disponibles.",
                        quantityToAdd, totalStock, quantityInCart, availableStock);
            } else {

                errorMsg = String.format(
                        "No se puede añadir %d. Stock disponible: %d.",
                        quantityToAdd, availableStock);
            }
            showSimpleError(errorMsg);
            return;
        }

        spnCantidad.setValue(quantityToAdd);
        double unitPrice = selectedProduct.getPrice();

        itemsModel.addRow(new Object[] {
                selectedProduct.getId(),
                selectedProduct.getName(),
                quantityToAdd,
                unitPrice
        });

        updateTotalFromTable();

        spnCantidad.setValue(1);
        lblPrecioUnitario.setText(formatCurrency(0L));
        productSearchField.clearSelection();
        productSearchField.requestFocusInWindow();
    }

    private void quitarItemSeleccionado() {
        int[] selectedRows = itemsTable.getSelectedRows();
        if (selectedRows.length == 0) {
            showSimpleError("Debe seleccionar uno o más ítems de la tabla para quitar.");
            return;
        }
        for (int i = selectedRows.length - 1; i >= 0; i--) {
            int modelRow = itemsTable.convertRowIndexToModel(selectedRows[i]);
            itemsModel.removeRow(modelRow);
        }
        updateTotalFromTable();
    }

    private void updateTotalFromTable() {
        long total = 0;
        for (int i = 0; i < itemsModel.getRowCount(); i++) {
            try {
                int qty = (Integer) itemsModel.getValueAt(i, 2);
                double price = (Double) itemsModel.getValueAt(i, 3);
                total += (long) (qty * price);
            } catch (Exception e) {
                System.err.println("Error al calcular total en fila " + i + ": " + e.getMessage());
            }
        }
        lblTotalValue.setText(formatCurrency(total));
    }

    private void showSimpleError(String message) {
        ConfirmDialog.showErrorDialog(SwingUtilities.getWindowAncestor(this),
                message, "Error de Validación");
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
        cm.getColumn(1).setPreferredWidth(350);
        cm.getColumn(2).setPreferredWidth(80);
        cm.getColumn(3).setPreferredWidth(120);
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lbl.setForeground(Color.BLACK);
        return lbl;
    }

    private JTextField createStyledField(String text) {
        JTextField field = new JTextField(text);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        field.setBackground(new Color(245, 245, 245));
        field.setBorder(new CompoundBorder(
                new LineBorder(new Color(180, 180, 180), 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        return field;
    }

    private JDateChooser createStyledDateChooser(String dateStr) {
        Date initialDate = new Date();
        try {
            if (dateStr != null && !dateStr.isEmpty()) {
                initialDate = jsonDateFormat.parse(dateStr);
            }
        } catch (java.text.ParseException ex) {
            ex.printStackTrace();
        }

        JDateChooser chooser = new JDateChooser();
        chooser.setDateFormatString("dd-MM-yyyy");
        chooser.setDate(initialDate);
        chooser.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        JFormattedTextField editor = (JFormattedTextField) chooser.getDateEditor().getUiComponent();
        editor.setBackground(new Color(245, 245, 245));
        editor.setBorder(new CompoundBorder(
                new LineBorder(new Color(180, 180, 180), 1, true),
                new EmptyBorder(8, 12, 8, 12)));

        return chooser;
    }

    private void addStandardFocusListener(JComponent field) {
        final Border defaultBorder = field.getBorder();
        final Border focusBorder;
        Insets insideInsets;
        if (field.getBorder() instanceof CompoundBorder) {
            insideInsets = ((CompoundBorder) defaultBorder).getInsideBorder().getBorderInsets(field);
        } else {
            insideInsets = field.getInsets();
        }
        focusBorder = new CompoundBorder(new LineBorder(GlobalView.ASIDE_BACKGROUND, 2, true),
                new EmptyBorder(insideInsets.top - 1, insideInsets.left - 1, insideInsets.bottom - 1,
                        insideInsets.right - 1));
        field.addFocusListener(new FocusAdapter() {
            private boolean isEditable(Component c) {
                if (c instanceof JTextComponent)
                    return ((JTextComponent) c).isEditable();
                if (c instanceof JFormattedTextField)
                    return ((JFormattedTextField) c).isEditable();
                if (c.getParent() instanceof JSpinner)
                    return c.isEnabled();
                return c.isEnabled();
            }

            @Override
            public void focusGained(FocusEvent e) {
                if (isEditable(field))
                    field.setBorder(focusBorder);
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (isEditable(field))
                    field.setBorder(defaultBorder);
            }
        });
    }

    private void addCurrencyFocusListener(JTextField field) {
        final Border defaultBorder = field.getBorder();
        final Border focusBorder = new CompoundBorder(
                new LineBorder(GlobalView.ASIDE_BACKGROUND, 2, true),
                new EmptyBorder(7, 11, 7, 11));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.isEditable()) {
                    field.setBorder(focusBorder);
                    field.setText(unformatCurrency(field.getText()));
                    field.selectAll();
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (field.isEditable()) {
                    field.setBorder(defaultBorder);
                    try {
                        Long.parseLong(unformatCurrency(field.getText()));
                        field.setText(formatCurrency(field.getText()));
                    } catch (NumberFormatException ex) {
                        field.setText(formatCurrency(0L));
                    }
                }
            }
        });
    }

    private String formatCurrency(long amount) {
        try {
            return currencyFormatter.format(amount);
        } catch (Exception e) {
            return "$ 0";
        }
    }

    private String formatCurrency(String numberStr) {
        try {
            String clean = unformatCurrency(numberStr);
            long amount = Long.parseLong(clean);
            return currencyFormatter.format(amount);
        } catch (Exception e) {
            try {
                double dAmount = Double.parseDouble(numberStr);
                return currencyFormatter.format((long) dAmount);
            } catch (Exception e2) {
                if (numberStr != null && numberStr.startsWith("$"))
                    return numberStr;
                return "$ 0";
            }
        }
    }

    private String unformatCurrency(String currencyStr) {
        if (currencyStr == null || currencyStr.isEmpty())
            return "0";
        String clean = currencyStr;
        int dotIndex = clean.indexOf('.');
        if (dotIndex != -1) {
            clean = clean.substring(0, dotIndex);
        }
        clean = clean.replaceAll("\\D", "");
        if (clean.isEmpty())
            return "0";
        return clean;
    }

    private JComboBox<String> createStyledComboBox(String[] items) {
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        combo.setBackground(new Color(245, 245, 245));
        combo.setBorder(new CompoundBorder(
                new LineBorder(new Color(180, 180, 180), 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        return combo;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 20));
        buttonPanel.setBackground(GlobalView.GENERAL_BACKGROUND);
        JButton cancelBtn = createStyledButton("Cancelar", GlobalView.CANCEL_BUTTON_BACKGROUND,
                GlobalView.CANCEL_BUTTON_BACKGROUND.darker());
        JButton saveBtn = createStyledButton("Aceptar", GlobalView.CONFIRM_BUTTON_BACKGROUND,
                GlobalView.CONFIRM_BUTTON_BACKGROUND.darker());
        cancelBtn.addActionListener(e -> closePanel());
        saveBtn.addActionListener(e -> saveChanges());
        buttonPanel.add(cancelBtn);
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

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();
        if (currentInvoiceNumber.isEmpty()) {
            errors.append("- Error: El 'Registro Nº' no se generó.\n");
        }

        String customerName = txtCustomer.getText().trim();
        if (customerName.isEmpty()) {
            errors.append("- El campo 'Cliente' es obligatorio.\n");
        } else if (customerName.length() > MAX_CUSTOMER_NAME_LENGTH) {
            errors.append("- El 'Cliente' no debe exceder los " + MAX_CUSTOMER_NAME_LENGTH + " caracteres.\n");
        } else if (!CUSTOMER_NAME_PATTERN.matcher(customerName).matches()) {
            errors.append("- El 'Cliente' contiene caracteres no válidos.\n");
        }

        if (dateChooser.getDate() == null) {
            errors.append("- El campo 'Fecha' es obligatorio.\n");
        }
        if (itemsModel.getRowCount() == 0) {
            errors.append("- Debe añadir al menos un producto a la venta.\n");
        }
        if (errors.length() > 0) {
            ConfirmDialog.showErrorDialog(SwingUtilities.getWindowAncestor(this),
                    "Por favor corrija los siguientes errores:\n" + errors.toString(),
                    "Errores de validación");
            return false;
        }
        return true;
    }

    private void saveChanges() {
        if (!validateInput()) {
            return;
        }
        try {
            String invoiceNum = this.currentInvoiceNumber;
            String customerName = txtCustomer.getText().trim();
            String dateStr = jsonDateFormat.format(dateChooser.getDate());
            String statusStr = (String) cmbEstado.getSelectedItem();
            List<RegisterItem> itemsList = new ArrayList<>();
            for (int i = 0; i < itemsModel.getRowCount(); i++) {
                String prodId = (String) itemsModel.getValueAt(i, 0);
                int qty = (Integer) itemsModel.getValueAt(i, 2);
                double price = (Double) itemsModel.getValueAt(i, 3);
                itemsList.add(new RegisterItem(prodId, qty, price));
            }
            String message = isCreateMode ? "¿Desea crear este nuevo registro?"
                    : "¿Desea guardar los cambios realizados?";
            boolean confirmed = ConfirmDialog.showConfirmDialog(SwingUtilities.getWindowAncestor(this), message,
                    "Confirmación");

            if (confirmed) {
                SellingRegister saleDTO = new SellingRegister(
                        invoiceNum, dateStr, statusStr, customerName, itemsList);
                boolean success = presenter.registerNewSale(saleDTO);
                if (success) {
                    JSONObject dataForCallback = new JSONObject();
                    dataForCallback.put("invoiceNumber", saleDTO.getInvoiceNumber());
                    dataForCallback.put("customerName", saleDTO.getCustomerName());
                    dataForCallback.put("date", saleDTO.getDate());
                    dataForCallback.put("total", saleDTO.getTotal());
                    dataForCallback.put("status", saleDTO.getStatus());
                    JSONArray itemsJsonArray = new JSONArray();
                    for (RegisterItem item : itemsList) {
                        JSONObject itemJson = new JSONObject();
                        itemJson.put("productId", item.getProductId());
                        itemJson.put("quantity", item.getQuantity());
                        itemJson.put("unitPrice", item.getUnitPrice());
                        itemsJsonArray.put(itemJson);
                    }
                    dataForCallback.put("items", itemsJsonArray);
                    onSaveCallback.accept(dataForCallback);
                    closePanel();
                    SuccessPopUp.showSuccessPopup((Frame) SwingUtilities.getWindowAncestor(this),
                            "Éxito:", "El registro se actualizó exitosamente.");
                } else {
                    showSimpleError("No se pudo guardar el registro. Revise la consola.");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            ConfirmDialog.showErrorDialog(SwingUtilities.getWindowAncestor(this),
                    "Ocurrió un error inesperado al guardar:\n" + ex.getMessage(), "Error inesperado");
        }
    }

    private void closePanel() {
        controller.showPanel(parentPanel);
    }

    private class ProductListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Product) {
                Product product = (Product) value;
                setText(product.getName() + " (" + product.getBrand() + ")");
            }
            return this;
        }
    }
}