package co.edu.uptc.view.panels;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import org.json.*;

import co.edu.uptc.presenter.Presenter;
import co.edu.uptc.view.utils.ViewController;
import co.edu.uptc.view.GlobalView;
import co.edu.uptc.view.dialogs.ConfirmDialog;
import co.edu.uptc.view.dialogs.SuccessPopUp;

import co.edu.uptc.view.panels.SubPanels.EditCreateSalesPanel;
import co.edu.uptc.view.panels.SubPanels.ViewSalesDetailsPanel;

import co.edu.uptc.view.utils.PropertiesService;

public class SalesPanel extends JPanel {

    private PropertiesService p;
    private JTable table;
    private DefaultTableModel model;
    private List<JSONObject> allSales;
    private List<JSONObject> filteredSales;
    private int currentPage = 1;
    private final int rowsPerPage = 8;
    private JLabel pageLabel;
    private JButton prevBtn, nextBtn;
    private JTextField searchField;
    private boolean sortAscending = true;

    private JButton btnViewDetails;

    private ViewController controller;
    private Presenter presenter;

    public SalesPanel(ViewController controller, Presenter presenter) {
        this.p = new PropertiesService();
        this.controller = controller;
        this.presenter = presenter;
        this.allSales = new ArrayList<>();
        this.filteredSales = new ArrayList<>();
        initComponents();
        loadDataFromServer();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(GlobalView.GENERAL_BACKGROUND);

        JLabel title = new JLabel("Gestión de Registros de Venta", SwingConstants.CENTER);
        title.setFont(GlobalView.TITLE_FONT);
        title.setForeground(GlobalView.TEXT_COLOR);
        title.setBorder(new EmptyBorder(15, 0, 15, 0));
        add(title, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(GlobalView.GENERAL_BACKGROUND_LIGHT);
        centerPanel.setBorder(new EmptyBorder(10, 30, 41, 30));
        add(centerPanel, BorderLayout.CENTER);

        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.setBackground(GlobalView.GENERAL_BACKGROUND_LIGHT);
        searchPanel.setBorder(new EmptyBorder(0, 0, 15, 0));

        searchField = new JTextField(" Buscar por nombre del cliente");

        searchField.setFont(GlobalView.TEXT_FIELD_FONT);
        searchField.setForeground(GlobalView.PLACEHOLDER_COLOR);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GlobalView.BORDER_COLOR, 1, true),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        searchField.setPreferredSize(new Dimension(0, 45));
        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (searchField.getText().trim().equals("Buscar por nombre del cliente")) {
                    searchField.setText("");
                    searchField.setForeground(GlobalView.TEXT_COLOR);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (searchField.getText().trim().isEmpty()) {
                    searchField.setText(" Buscar por nombre del cliente");
                    searchField.setForeground(GlobalView.PLACEHOLDER_COLOR);
                }
            }
        });
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                performSearch();
            }

            public void removeUpdate(DocumentEvent e) {
                performSearch();
            }

            public void changedUpdate(DocumentEvent e) {
                performSearch();
            }
        });

        searchPanel.add(searchField, BorderLayout.CENTER);
        centerPanel.add(searchPanel, BorderLayout.NORTH);

        String[] columns = { "Registro Nº", "Cliente", "Fecha", "Total", "Estado", "Acciones" };
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 5;
            }
        };

        table = new JTable(model);
        setupTableStyle();

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                btnViewDetails.setEnabled(table.getSelectedRow() != -1);
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        setupScrollPane(scrollPane);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        centerPanel.add(createBottomPanel(), BorderLayout.SOUTH);
    }

    private void setupTableStyle() {

        table.setRowHeight(48);
        table.setFont(GlobalView.TABLE_BODY_FONT);
        table.setSelectionBackground(GlobalView.TABLE_SELECTION_BACKGROUND);
        table.setSelectionForeground(GlobalView.TABLE_SELECTION_FOREGROUND);
        table.setGridColor(GlobalView.BORDER_COLOR);
        table.setShowVerticalLines(false);
        JTableHeader header = table.getTableHeader();
        header.setFont(GlobalView.TABLE_HEADER_FONT);
        header.setBackground(GlobalView.TABLE_HEADER_BACKGROUND);
        header.setForeground(GlobalView.TABLE_HEADER_FOREGROUND);
        header.setResizingAllowed(false);
        header.setReorderingAllowed(false);
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount() - 1; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(5).setCellRenderer(new ActionRenderer());
        columnModel.getColumn(5).setCellEditor(new ActionEditor(this));
        columnModel.getColumn(5).setMaxWidth(120);
        columnModel.getColumn(5).setMinWidth(120);
    }

    private void setupScrollPane(JScrollPane scrollPane) {

        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        int headerHeight = table.getTableHeader().getPreferredSize().height;
        int tableHeight = rowsPerPage * table.getRowHeight();
        scrollPane.setPreferredSize(new Dimension(0, headerHeight + tableHeight));
        scrollPane.setBorder(new LineBorder(GlobalView.BORDER_COLOR, 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(GlobalView.GENERAL_BACKGROUND_LIGHT);
        bottomPanel.setBorder(new EmptyBorder(15, 30, 15, 30));

        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        paginationPanel.setBackground(GlobalView.GENERAL_BACKGROUND_LIGHT);

        prevBtn = new JButton("<<");
        nextBtn = new JButton(">>");
        pageLabel = new JLabel();
        pageLabel.setFont(GlobalView.TEXT_FIELD_FONT);
        for (JButton b : new JButton[] { prevBtn, nextBtn }) {
            b.setFont(GlobalView.BUTTON_FONT);
            b.setFocusable(false);
            b.setBackground(GlobalView.BUTTON_BACKGROUND_COLOR);
            b.setForeground(GlobalView.BUTTON_FOREGROUND_COLOR);
            b.setBorder(new LineBorder(GlobalView.BORDER_COLOR));
            b.setPreferredSize(new Dimension(50, 38));
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            b.addMouseListener(
                    new ButtonHoverEffect(b, GlobalView.BUTTON_BACKGROUND_COLOR, GlobalView.BUTTON_HOVER_COLOR));
        }
        prevBtn.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                refreshTable();
            }
        });
        nextBtn.addActionListener(e -> {
            int maxPage = (filteredSales != null && !filteredSales.isEmpty())
                    ? (int) Math.ceil((double) filteredSales.size() / rowsPerPage)
                    : 1;
            if (currentPage < maxPage) {
                currentPage++;
                refreshTable();
            }
        });
        paginationPanel.add(new JLabel("Página") {
            {
                setFont(GlobalView.TEXT_FIELD_FONT);
            }
        });
        paginationPanel.add(prevBtn);
        paginationPanel.add(pageLabel);
        paginationPanel.add(nextBtn);

        JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionButtonPanel.setOpaque(false);

        ImageIcon viewIcon = createIcon(p.getProperties("search"), 22, 22);
        btnViewDetails = new JButton("Ver Detalles", viewIcon);
        btnViewDetails.setFont(new Font(GlobalView.BUTTON_FONT.getFamily(), Font.BOLD, 16));
        btnViewDetails.setIconTextGap(15);
        btnViewDetails.setBackground(GlobalView.ASIDE_BACKGROUND);
        btnViewDetails.setForeground(Color.WHITE);
        btnViewDetails.setFocusPainted(false);
        btnViewDetails.setBorder(new EmptyBorder(10, 25, 10, 25));
        btnViewDetails.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnViewDetails.addMouseListener(new ButtonHoverEffect(btnViewDetails, GlobalView.ASIDE_BACKGROUND,
                GlobalView.ASIDE_BACKGROUND.darker()));
        if (viewIcon == null)
            btnViewDetails.setIcon(null);
        btnViewDetails.setEnabled(false);

        btnViewDetails.addActionListener(e -> viewDetailsAction());

        ImageIcon addIcon = createIcon(p.getProperties("add"), 22, 22);
        JButton newSaleBtn = new JButton("Nuevo Registro", addIcon);

        newSaleBtn.setFont(new Font(GlobalView.BUTTON_FONT.getFamily(), Font.BOLD, 16));
        newSaleBtn.setIconTextGap(15);
        newSaleBtn.setBackground(GlobalView.CONFIRM_BUTTON_BACKGROUND);
        newSaleBtn.setForeground(Color.WHITE);
        newSaleBtn.setFocusPainted(false);
        newSaleBtn.setBorder(new EmptyBorder(10, 25, 10, 25));
        newSaleBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        newSaleBtn.addMouseListener(new ButtonHoverEffect(newSaleBtn, GlobalView.CONFIRM_BUTTON_BACKGROUND,
                GlobalView.CONFIRM_BUTTON_BACKGROUND.darker()));
        if (addIcon == null)
            newSaleBtn.setIcon(null);

        newSaleBtn.addActionListener(e -> newSaleAction());

        actionButtonPanel.add(btnViewDetails);
        actionButtonPanel.add(newSaleBtn);

        bottomPanel.add(paginationPanel, BorderLayout.WEST);
        bottomPanel.add(actionButtonPanel, BorderLayout.EAST);

        JButton sortByDateBtn = new JButton("Ordenar por Fecha");
        sortByDateBtn.setFont(new Font(GlobalView.BUTTON_FONT.getFamily(), Font.BOLD, 16));
        sortByDateBtn.setBackground(GlobalView.ASIDE_BACKGROUND);
        sortByDateBtn.setForeground(Color.WHITE);
        sortByDateBtn.setFocusPainted(false);
        sortByDateBtn.setBorder(new EmptyBorder(10, 25, 10, 25));
        sortByDateBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sortByDateBtn.addMouseListener(new ButtonHoverEffect(sortByDateBtn, GlobalView.ASIDE_BACKGROUND,
                GlobalView.ASIDE_BACKGROUND.darker()));

        sortByDateBtn.addActionListener(e -> sortTableByDate());

        actionButtonPanel.add(sortByDateBtn);

        return bottomPanel;
    }

    private void viewDetailsAction() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1)
            return;

        int modelRow = table.convertRowIndexToModel(selectedRow);
        int dataIndex = (currentPage - 1) * rowsPerPage + modelRow;
        JSONObject saleData = filteredSales.get(dataIndex);

        ViewSalesDetailsPanel detailsPanel = new ViewSalesDetailsPanel(
                this, controller, presenter, saleData);
        controller.showPanel(detailsPanel);
    }

    private void newSaleAction() {
        JSONObject newSale = new JSONObject();
        Consumer<JSONObject> onSaveCallback = (savedData) -> {
            allSales.add(0, savedData);
            refreshData();
        };
        EditCreateSalesPanel createPanel = new EditCreateSalesPanel(
                "Nuevo Registro de Venta", newSale, onSaveCallback, this, controller, presenter);
        controller.showPanel(createPanel);
    }

    private void loadDataFromServer() {
        this.allSales = presenter.getSalesHistoryAsJson();

        if (this.allSales == null) {
            System.err.println("ERROR: El Presenter devolvió null al cargar el historial de ventas.");
            this.allSales = new ArrayList<>();

            ConfirmDialog.showErrorDialog(SwingUtilities.getWindowAncestor(this),
                    "No se pudieron cargar los datos de ventas.",
                    "Error de Carga");

        } else {
            System.out.println("SalesPanel: " + this.allSales.size() + " registros cargados del Presenter.");
        }
        refreshData();
    }

    public void refreshData() {

        if (allSales != null) {
            allSales.sort((a, b) -> {
                String idA = a.optString("invoiceNumber", "0");
                String idB = b.optString("invoiceNumber", "0");
                try {

                    int intA = Integer.parseInt(idA);
                    int intB = Integer.parseInt(idB);
                    return Integer.compare(intB, intA);
                } catch (NumberFormatException e) {

                    return idB.compareTo(idA);
                }
            });
        }
        performSearch();
    }

    private void performSearch() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.equals("buscar por nombre del cliente")) {
            filterData("");
        } else {
            filterData(query);
        }
    }

    private void filterData(String query) {
        if (query.isEmpty()) {
            filteredSales = new ArrayList<>(allSales != null ? allSales : List.of());
        } else {
            filteredSales = new ArrayList<>();
            if (allSales != null) {
                for (JSONObject sale : allSales) {
                    if (sale.optString("customerName").toLowerCase().contains(query)) {
                        filteredSales.add(sale);
                    }
                }
            }
        }
        currentPage = 1;
        refreshTable();
    }

    private void sortTableByDate() {
        if (filteredSales == null || filteredSales.isEmpty())
            return;

        filteredSales.sort((a, b) -> {
            String dateA = a.optString("date", "");
            String dateB = b.optString("date", "");

            try {
                SimpleDateFormat sdf = dateA.contains("T") ? new SimpleDateFormat("dd-MM-yyyy'T'HH:mm:ss")
                        : new SimpleDateFormat("dd-MM-yyyy");
                Date dA = sdf.parse(dateA);
                Date dB = sdf.parse(dateB);

                return sortAscending ? dA.compareTo(dB) : dB.compareTo(dA);
            } catch (Exception e) {
                return 0;
            }
        });

        sortAscending = !sortAscending;
        currentPage = 1;
        refreshTable();
    }

    private String formatCurrency(String numberStr) {
        try {
            String clean = unformatCurrency(numberStr);
            long amount = Long.parseLong(clean);
            DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(new Locale("es", "CO"));
            formatter.applyPattern("$ #,##0");
            return formatter.format(amount);
        } catch (Exception e) {
            try {
                double dAmount = Double.parseDouble(numberStr);
                DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(new Locale("es", "CO"));
                formatter.applyPattern("$ #,##0");
                return formatter.format((long) dAmount);
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

    private void refreshTable() {
        model.setRowCount(0);
        if (filteredSales == null || filteredSales.isEmpty()) {
            pageLabel.setText(" 0 / 0 ");
            prevBtn.setEnabled(false);
            nextBtn.setEnabled(false);
            btnViewDetails.setEnabled(false);
            return;
        }
        int start = (currentPage - 1) * rowsPerPage;
        int end = Math.min(start + rowsPerPage, filteredSales.size());
        for (int i = start; i < end; i++) {
            JSONObject sale = filteredSales.get(i);
            model.addRow(new Object[] {
                    sale.optString("invoiceNumber", "N/A"),
                    sale.optString("customerName", "N/A"),
                    formatDateColombian(sale.optString("date", "N/A")),
                    formatCurrency(String.valueOf(sale.opt("total"))),
                    sale.optString("status", "N/A"),
                    ""
            });
        }
        int maxPage = Math.max(1, (int) Math.ceil((double) filteredSales.size() / rowsPerPage));
        pageLabel.setText(" " + currentPage + " / " + maxPage + " ");
        prevBtn.setEnabled(currentPage > 1);
        nextBtn.setEnabled(currentPage < maxPage);
        if (table.getSelectedRow() == -1) {
            btnViewDetails.setEnabled(false);
        }
    }

    private ImageIcon createIcon(String path, int width, int height) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        try {
            java.net.URL imgUrl = getClass().getResource(path);
            if (imgUrl == null) {
                System.err.println("Error: No se pudo encontrar la imagen en el classpath: " + path);
                return null;
            }
            ImageIcon icon = new ImageIcon(imgUrl);
            Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        } catch (Exception e) {
            System.err.println("Excepción al crear icono desde el classpath: " + path);
            e.printStackTrace();
            return null;
        }
    }

    class ActionRenderer extends DefaultTableCellRenderer {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));

        public ActionRenderer() {
            setOpaque(true);
            panel.setOpaque(false);

            JButton editBtn = new JButton(createIcon(p.getProperties("edit"), 18, 18));
            JButton deleteBtn = new JButton(createIcon(p.getProperties("delete"), 18, 18));
            if (editBtn.getIcon() == null)
                editBtn.setText("E");
            if (deleteBtn.getIcon() == null)
                deleteBtn.setText("X");

            for (JButton b : new JButton[] { editBtn, deleteBtn }) {
                b.setFocusPainted(false);
                b.setBorderPainted(false);
                b.setContentAreaFilled(false);
                b.setPreferredSize(new Dimension(35, 35));
                b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            panel.add(editBtn);
            panel.add(deleteBtn);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            JPanel wrapper = new JPanel(new GridBagLayout());
            wrapper.setBackground(getBackground());
            wrapper.add(panel);
            return wrapper;
        }
    }

    class ActionEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        private JSONObject currentSaleData;
        private String currentSaleId;
        private final SalesPanel salesPanel;

        public ActionEditor(SalesPanel salesPanel) {
            this.salesPanel = salesPanel;
            panel.setOpaque(true);

            JButton editBtn = new JButton(createIcon(p.getProperties("edit"), 18, 18));
            JButton deleteBtn = new JButton(createIcon(p.getProperties("delete"), 18, 18));
            if (editBtn.getIcon() == null)
                editBtn.setText("E");
            if (deleteBtn.getIcon() == null)
                deleteBtn.setText("X");

            for (JButton b : new JButton[] { editBtn, deleteBtn }) {
                b.setFocusPainted(false);
                b.setBorderPainted(false);
                b.setContentAreaFilled(false);
                b.setPreferredSize(new Dimension(35, 35));
                b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            editBtn.addActionListener(e -> {
                fireEditingStopped();
                if (currentSaleData == null || currentSaleId == null)
                    return;
                Consumer<JSONObject> onSaveCallback = (savedData) -> {
                    boolean updated = false;
                    for (int i = 0; i < allSales.size(); i++) {
                        if (allSales.get(i).optString("invoiceNumber").equals(currentSaleId)) {
                            allSales.set(i, savedData);
                            updated = true;
                            break;
                        }
                    }
                    if (!updated)
                        System.err.println("Edit Callback Warning (Sales): Couldn't find original item to update.");
                    refreshData();
                };
                EditCreateSalesPanel editPanel = new EditCreateSalesPanel(
                        "Editar Registro de Venta", currentSaleData, onSaveCallback,
                        salesPanel, controller, presenter);
                controller.showPanel(editPanel);
            });

            deleteBtn.addActionListener(e -> {
                fireEditingStopped();
                if (currentSaleData == null || currentSaleId == null)
                    return;
                Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(salesPanel);
                boolean confirm = ConfirmDialog.showConfirmDialog(
                        parentFrame, "¿Desea eliminar el registro \"" + currentSaleId + "\"?", "Confirmar Eliminación");
                if (confirm) {
                    boolean deleted = presenter.requestSaleCancellation(currentSaleId);
                    if (deleted) {
                        boolean removed = allSales.removeIf(
                                sale -> sale.optString("invoiceNumber").equals(currentSaleId));
                        if (removed) {
                            refreshData();
                            SuccessPopUp.showSuccessPopup(parentFrame, "Éxito:",
                                    "El registro se eliminó correctamente.");
                        } else {
                            System.err.println(
                                    "Delete Warning (Sales): Item deleted by presenter but not found in local list!");
                            refreshData();
                        }
                    } else {

                        ConfirmDialog.showErrorDialog(parentFrame,
                                "No se pudo eliminar el registro. Intente de nuevo.",
                                "Error de Eliminación");

                    }
                }
            });

            panel.add(editBtn);
            panel.add(deleteBtn);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
                int column) {
            panel.setBackground(table.getSelectionBackground());
            int modelRow = table.convertRowIndexToModel(row);
            int listIndex = (currentPage - 1) * rowsPerPage + modelRow;
            if (filteredSales != null && listIndex >= 0 && listIndex < filteredSales.size()) {
                currentSaleData = filteredSales.get(listIndex);
                currentSaleId = currentSaleData.optString("invoiceNumber");
            } else {
                currentSaleData = null;
                currentSaleId = null;
            }
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return null;
        }
    }

    class ButtonHoverEffect extends MouseAdapter {
        private final JButton button;
        private final Color defaultBackground;
        private final Color hoverBackground;

        public ButtonHoverEffect(JButton button, Color defaultBackground, Color hoverBackground) {
            this.button = button;
            this.defaultBackground = defaultBackground;
            this.hoverBackground = hoverBackground;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            button.setBackground(hoverBackground);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            button.setBackground(defaultBackground);
        }
    }

    private String formatDateColombian(String isoDate) {
        if (isoDate == null || isoDate.isEmpty())
            return "N/A";
        try {
            Date date;
            if (isoDate.contains("/")) {

                SimpleDateFormat sdfIn = new SimpleDateFormat("dd/MM/yyyy");
                date = sdfIn.parse(isoDate);
            } else if (isoDate.contains("T")) {
                SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                date = sdfIn.parse(isoDate);
            } else {
                SimpleDateFormat sdfIn = new SimpleDateFormat("yyyy-MM-dd");
                date = sdfIn.parse(isoDate);
            }
            SimpleDateFormat sdfOut = new SimpleDateFormat("dd/MM/yyyy");
            return sdfOut.format(date);
        } catch (Exception e) {
            System.err.println("Error formateando fecha: " + isoDate);
            return isoDate;
        }
    }

}