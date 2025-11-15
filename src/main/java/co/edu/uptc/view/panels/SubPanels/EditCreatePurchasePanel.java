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
import co.edu.uptc.models.registers.PurchaseRegister;
import co.edu.uptc.models.registers.RegisterItem;

import co.edu.uptc.view.GlobalView;
import co.edu.uptc.view.dialogs.ConfirmDialog;
import co.edu.uptc.view.dialogs.SuccessPopUp;
import co.edu.uptc.view.utils.ViewController;
import co.edu.uptc.view.components.SearchableProductField;

public class EditCreatePurchasePanel extends JPanel {

    private JLabel lblFacturaValue;
    private JTextField txtProveedor;
    private JComboBox<String> cmbEstado;
    private JDateChooser dateChooser;
    private JLabel lblTotalValue;
    private SearchableProductField productSearchField;
    private JSpinner spnCantidad;
    private JTextField txtCostoUnitario;
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

    private static final int MAX_SUPPLIER_NAME_LENGTH = 100;
    private static final Pattern SUPPLIER_NAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9\\s.'-ñÑáéíóúÁÉÍÓÚüÜ]+$");
    private static final int MIN_SPINNER_QTY = 1;
    private static final int MAX_SPINNER_QTY = 999;
    private static final long MAX_UNIT_COST = 20_000_000L;

    public EditCreatePurchasePanel(String titleText, JSONObject purchaseData, Consumer<JSONObject> onSaveCallback,
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

        String invoiceNum = purchaseData.optString("invoiceNumber");
        this.isCreateMode = invoiceNum.isEmpty();

        if (isCreateMode) {
            this.currentInvoiceNumber = presenter.getNextPurchaseInvoiceNumber();
        } else {
            this.currentInvoiceNumber = invoiceNum;
        }

        initComponents(titleText, purchaseData);

        if (!isCreateMode) {
            loadItemsIntoTable(purchaseData);
        }
        updateTotalFromTable();
    }

    private void initComponents(String titleText, JSONObject purchaseData) {
        setLayout(new BorderLayout(20, 20));
        setBackground(GlobalView.GENERAL_BACKGROUND);
        setBorder(new EmptyBorder(30, 40, 30, 40));
        add(createTitle(titleText), BorderLayout.NORTH);
        JPanel masterPanel = createMasterFormPanel(purchaseData);
        masterPanel.setPreferredSize(new Dimension(450, 0));
        add(masterPanel, BorderLayout.WEST);
        JPanel itemsPanel = createItemsDetailPanel();
        add(itemsPanel, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    private JLabel createTitle(String titleText) {
        JLabel title = new JLabel(titleText);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(Color.BLACK);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setBorder(new EmptyBorder(0, 0, 20, 0));
        return title;
    }

    private JPanel createMasterFormPanel(JSONObject purchaseData) {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(GlobalView.BORDER_COLOR, 1, true),
                " Datos de la Compra ",
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
        formPanel.add(createLabel("Proveedor:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        txtProveedor = createStyledField(purchaseData.optString("supplierName", ""));
        addStandardFocusListener(txtProveedor);
        formPanel.add(txtProveedor, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(createLabel("Fecha:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        dateChooser = createStyledDateChooser(purchaseData.optString("date"));
        addStandardFocusListener((JComponent) dateChooser.getDateEditor().getUiComponent());
        formPanel.add(dateChooser, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(createLabel("Estado:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 3;
        String[] estados = { "Recibida", "En Tránsito", "Pagada", "Cancelada" };
        cmbEstado = createStyledComboBox(estados);
        cmbEstado.setSelectedItem(purchaseData.optString("status", "Recibida"));
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

        int standardHeight = txtProveedor.getPreferredSize().height;
        dateChooser.setPreferredSize(new Dimension(dateChooser.getPreferredSize().width, standardHeight));
        cmbEstado.setPreferredSize(new Dimension(cmbEstado.getPreferredSize().width, standardHeight));
        return formPanel;
    }

    private JPanel createItemsDetailPanel() {
        JPanel itemsPanel = new JPanel(new BorderLayout(10, 10));
        itemsPanel.setOpaque(false);
        itemsPanel.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(GlobalView.BORDER_COLOR, 1, true),
                " Ítems de la Compra ", TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, GlobalView.TABLE_HEADER_FONT,
                GlobalView.TEXT_COLOR));
        itemsPanel.add(createAddItemsSubPanel(), BorderLayout.NORTH);

        String[] itemColumns = { "ID Producto", "Nombre", "Cantidad", "Costo Unitario" };
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
        addPanel.add(createLabel("Costo Unitario:"), gbc);
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 0.3;
        txtCostoUnitario = createStyledField("0");
        addStandardFocusListener(txtCostoUnitario);
        txtCostoUnitario.setPreferredSize(new Dimension(txtCostoUnitario.getPreferredSize().width, standardHeight));

        txtCostoUnitario.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                String input = txtCostoUnitario.getText().trim();
                try {
                    long value = parseCurrencyStrictOrThrow(input);

                    if (value >= 0 && value <= MAX_UNIT_COST) {
                        txtCostoUnitario.setText(formatCurrency(value));
                    }

                } catch (IllegalArgumentException ex) {

                }
            }
        });

        addPanel.add(txtCostoUnitario, gbc);

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

    private void loadItemsIntoTable(JSONObject purchaseData) {
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
        int quantity;
        try {
            quantity = Integer.parseInt(qtyText);
        } catch (NumberFormatException e) {
            showSimpleError("La cantidad debe ser un número válido.");
            spnCantidad.requestFocus();
            return;
        }

        if (quantity < MIN_SPINNER_QTY || quantity > MAX_SPINNER_QTY) {
            String msg = String.format("La cantidad debe estar entre %d y %d.", MIN_SPINNER_QTY, MAX_SPINNER_QTY);
            showSimpleError(msg);
            spnCantidad.requestFocus();
            return;
        }

        String rawCostInput = txtCostoUnitario.getText().trim();
        long unitCostLong;
        try {

            unitCostLong = parseCurrencyStrictOrThrow(rawCostInput);
        } catch (IllegalArgumentException ex) {
            ConfirmDialog.showErrorDialog(SwingUtilities.getWindowAncestor(this), ex.getMessage(),
                    "Error de Validación");
            txtCostoUnitario.requestFocus();
            return;
        }

        if (unitCostLong < 0) {
            ConfirmDialog.showErrorDialog(SwingUtilities.getWindowAncestor(this),
                    "El costo unitario no puede ser negativo.", "Error de Validación");
            txtCostoUnitario.requestFocus();
            return;
        }

        if (unitCostLong > MAX_UNIT_COST) {
            String msg = String.format("El costo unitario es demasiado grande (máx: %s).",
                    formatCurrency(MAX_UNIT_COST));
            ConfirmDialog.showErrorDialog(SwingUtilities.getWindowAncestor(this),
                    msg, "Error de Validación");
            txtCostoUnitario.requestFocus();
            return;
        }

        double unitCost = (double) unitCostLong;

        spnCantidad.setValue(quantity);
        txtCostoUnitario.setText(formatCurrency(unitCostLong));

        itemsModel.addRow(new Object[] {
                selectedProduct.getId(),
                selectedProduct.getName(),
                quantity,
                unitCost
        });

        updateTotalFromTable();

        spnCantidad.setValue(1);
        txtCostoUnitario.setText(formatCurrency(0L));
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
        double total = 0.0;

        for (int i = 0; i < itemsModel.getRowCount(); i++) {
            try {
                int qty = (Integer) itemsModel.getValueAt(i, 2);
                Object costObj = itemsModel.getValueAt(i, 3);
                double cost;
                if (costObj instanceof Number) {
                    cost = ((Number) costObj).doubleValue();
                } else {
                    cost = Double.parseDouble(costObj.toString());
                }

                total += qty * cost;
            } catch (Exception e) {
                System.err.println("Error al calcular total en fila " + i + ": " + e.getMessage());
            }
        }
        lblTotalValue.setText(formatCurrency((long) total));
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
            @Override
            public void focusGained(FocusEvent e) {
                boolean editable = false;
                if (field instanceof JTextComponent) {
                    editable = ((JTextComponent) field).isEditable();
                } else if (field instanceof JFormattedTextField) {
                    editable = ((JFormattedTextField) field).isEditable();
                } else if (field.getParent() instanceof JSpinner) {
                    editable = field.isEnabled();
                }

                if (editable)
                    field.setBorder(focusBorder);
            }

            @Override
            public void focusLost(FocusEvent e) {
                boolean editable = false;
                if (field instanceof JTextComponent) {
                    editable = ((JTextComponent) field).isEditable();
                } else if (field instanceof JFormattedTextField) {
                    editable = ((JFormattedTextField) field).isEditable();
                } else if (field.getParent() instanceof JSpinner) {
                    editable = field.isEnabled();
                }

                if (editable)
                    field.setBorder(defaultBorder);
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

        String clean = currencyStr.replaceAll("\\D", "");
        if (clean.isEmpty())
            return "0";
        return clean;
    }

    private long parseCurrencyStrictOrThrow(String input) throws IllegalArgumentException {
        if (input == null)
            throw new IllegalArgumentException("El costo unitario no puede estar vacío. Ingrese un número válido.");
        String trimmed = input.trim();
        if (trimmed.isEmpty())
            throw new IllegalArgumentException("El costo unitario no puede estar vacío. Ingrese un número válido.");

        if (Pattern.compile("[A-Za-z]").matcher(trimmed).find()) {
            throw new IllegalArgumentException("El costo unitario debe contener solo números. Corríjalo.");
        }

        if (trimmed.contains("-")) {
            throw new IllegalArgumentException("El costo unitario no puede ser negativo. Corríjalo.");
        }

        String digits = trimmed.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            throw new IllegalArgumentException("El costo unitario debe contener dígitos. Corríjalo.");
        }

        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {

            throw new IllegalArgumentException("El costo unitario no es un número válido o es demasiado grande.");
        }
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

        String supplierName = txtProveedor.getText().trim();
        if (supplierName.isEmpty()) {
            errors.append("- El campo 'Proveedor' es obligatorio.\n");
        } else if (supplierName.length() > MAX_SUPPLIER_NAME_LENGTH) {
            errors.append("- El 'Proveedor' no debe exceder los " + MAX_SUPPLIER_NAME_LENGTH + " caracteres.\n");
        } else if (!SUPPLIER_NAME_PATTERN.matcher(supplierName).matches()) {
            errors.append("- El 'Proveedor' contiene caracteres no válidos.\n");
        }

        if (dateChooser.getDate() == null) {
            errors.append("- El campo 'Fecha' es obligatorio.\n");
        }
        if (itemsModel.getRowCount() == 0) {
            errors.append("- Debe añadir al menos un producto a la compra.\n");
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
            String supplierName = txtProveedor.getText().trim();
            String dateStr = jsonDateFormat.format(dateChooser.getDate());
            String statusStr = (String) cmbEstado.getSelectedItem();

            List<RegisterItem> itemsList = new ArrayList<>();
            JSONArray itemsJsonArray = new JSONArray();
            for (int i = 0; i < itemsModel.getRowCount(); i++) {
                String prodId = (String) itemsModel.getValueAt(i, 0);
                int qty = (Integer) itemsModel.getValueAt(i, 2);
                Number costNum = (Number) itemsModel.getValueAt(i, 3);
                double cost = costNum.doubleValue();
                itemsList.add(new RegisterItem(prodId, qty, cost));
                JSONObject itemJson = new JSONObject();
                itemJson.put("productId", prodId);
                itemJson.put("quantity", qty);
                itemJson.put("unitPrice", cost);
                itemsJsonArray.put(itemJson);
            }

            String message = isCreateMode ? "¿Desea crear este nuevo registro?"
                    : "¿Desea guardar los cambios realizados?";
            boolean confirmed = ConfirmDialog.showConfirmDialog(SwingUtilities.getWindowAncestor(this), message,
                    "Confirmación");

            if (confirmed) {
                PurchaseRegister purchaseDTO = new PurchaseRegister(
                        invoiceNum, dateStr, statusStr, supplierName, itemsList);

                boolean success = presenter.registerNewPurchase(purchaseDTO);

                if (success) {
                    JSONObject dataForCallback = new JSONObject();
                    dataForCallback.put("invoiceNumber", purchaseDTO.getInvoiceNumber());
                    dataForCallback.put("supplierName", purchaseDTO.getSupplierName());
                    dataForCallback.put("date", purchaseDTO.getDate());
                    dataForCallback.put("total", purchaseDTO.getTotal());
                    dataForCallback.put("status", purchaseDTO.getStatus());
                    dataForCallback.put("items", itemsJsonArray);

                    Window parentWindow = SwingUtilities.getWindowAncestor(this);
                    Frame frameForPopUp = (parentWindow instanceof Frame) ? (Frame) parentWindow
                            : (Frame) SwingUtilities.getWindowAncestor(parentPanel);

                    onSaveCallback.accept(dataForCallback);
                    closePanel();

                    String successMessage = isCreateMode
                            ? "El registro de compra fue creado exitosamente."
                            : "El registro de compra se actualizó exitosamente.";

                    SuccessPopUp.showSuccessPopup(frameForPopUp,
                            "Éxito:", successMessage);
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