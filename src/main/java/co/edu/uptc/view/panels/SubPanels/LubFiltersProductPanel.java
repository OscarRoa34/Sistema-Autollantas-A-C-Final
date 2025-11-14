package co.edu.uptc.view.panels.SubPanels;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import co.edu.uptc.presenter.Presenter;
import co.edu.uptc.models.products.Filters;
import co.edu.uptc.models.products.Lubricant;
import co.edu.uptc.models.products.Product;

import co.edu.uptc.view.GlobalView;
import co.edu.uptc.view.dialogs.ConfirmDialog;
import co.edu.uptc.view.dialogs.SuccessPopUp;
import co.edu.uptc.view.utils.PropertiesService;
import co.edu.uptc.view.utils.TextPrompt;
import co.edu.uptc.view.utils.ViewController;

public class LubFiltersProductPanel extends JPanel {

    private List<Product> allProducts;
    private List<Product> filteredProducts;
    private Product selectedProduct;

    private JPanel gridPanel;
    private JPanel paginationPanel;
    private JPanel filtersPanel;
    private JTextField searchField;
    private JRadioButton rbMayorPrecio, rbMenorPrecio;
    private PropertiesService p;

    private JTextField brandSearchField;
    private JTextField typeSpecificSearchField;

    private JRadioButton rbTodos, rbLubricantes, rbFiltros;

    private JButton editBtn, deleteBtn, addBtn;
    private int currentPage = 1;
    private final int ITEMS_PER_PAGE = 8;

    private final ViewController controller;
    private final Presenter presenter;

    private static final Icon RADIO_DEFAULT_ICON = createRadioButtonIcon(false, false);
    private static final Icon RADIO_SELECTED_ICON = createRadioButtonIcon(true, false);
    private static final Icon RADIO_HOVER_ICON = createRadioButtonIcon(false, true);
    private static final Icon RADIO_SELECTED_HOVER_ICON = createRadioButtonIcon(true, true);

    public LubFiltersProductPanel(ViewController controller, Presenter presenter) {
        this.controller = controller;
        this.presenter = presenter;
        this.p = new PropertiesService();
        this.allProducts = new ArrayList<>();
        this.filteredProducts = new ArrayList<>();

        setLayout(new BorderLayout());
        setBackground(GlobalView.GENERAL_BACKGROUND);

        loadDataFromPresenter();

        filtersPanel = createFilterPanel();
        filtersPanel.setBackground(GlobalView.GENERAL_BACKGROUND);
        add(filtersPanel, BorderLayout.EAST);

        gridPanel = new JPanel(new GridLayout(2, 4, 10, 10));
        gridPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        gridPanel.setBackground(GlobalView.GENERAL_BACKGROUND);
        add(gridPanel, BorderLayout.CENTER);

        paginationPanel = new JPanel();
        paginationPanel.setBackground(GlobalView.GENERAL_BACKGROUND);
        paginationPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(paginationPanel, BorderLayout.SOUTH);

        applyFilters();
        updateGrid();
        updatePagination();
    }

    private void loadDataFromPresenter() {

        this.allProducts = presenter.requestLubricantList();
        if (this.allProducts == null) {
            this.allProducts = new ArrayList<>();
            System.err.println("LubFiltersProductPanel: El presenter devolvió una lista nula.");
        }
        this.filteredProducts = new ArrayList<>(allProducts);

        if (filtersPanel != null) {
            remove(filtersPanel);
            filtersPanel = createFilterPanel();
            add(filtersPanel, BorderLayout.EAST);
        }
    }

    private JPanel createFilterPanel() {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(240, 0));
        panel.setBackground(GlobalView.GENERAL_BACKGROUND);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new CompoundBorder(
                new MatteBorder(0, 1, 0, 0, Color.DARK_GRAY),
                new EmptyBorder(15, 15, 15, 15)));

        JLabel lblTitle = new JLabel("FILTROS");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 30));
        lblTitle.setForeground(Color.BLACK);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblBusqueda = new JLabel("Búsqueda");
        lblBusqueda.setFont(new Font("Segoe UI", Font.BOLD, 25));
        lblBusqueda.setForeground(Color.BLACK);
        lblBusqueda.setAlignmentX(Component.LEFT_ALIGNMENT);

        searchField = createStyledSearchField("Nombre del producto");
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                applyFilters();
            }
        });

        JLabel lblMarca = new JLabel("Marca");
        lblMarca.setFont(new Font("Segoe UI", Font.BOLD, 25));
        lblMarca.setForeground(Color.BLACK);
        lblMarca.setAlignmentX(Component.LEFT_ALIGNMENT);

        brandSearchField = createStyledSearchField("Buscar por marca...");
        brandSearchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                applyFilters();
            }
        });

        JLabel lblTipoProducto = new JLabel("Tipo Producto");
        lblTipoProducto.setFont(new Font("Segoe UI", Font.BOLD, 25));
        lblTipoProducto.setForeground(Color.BLACK);
        lblTipoProducto.setAlignmentX(Component.LEFT_ALIGNMENT);

        rbTodos = createStyledRadio("Todos");
        rbLubricantes = createStyledRadio("Lubricantes");
        rbFiltros = createStyledRadio("Filtros");
        rbTodos.setSelected(true);

        ButtonGroup tipoGroup = new ButtonGroup();
        tipoGroup.add(rbTodos);
        tipoGroup.add(rbLubricantes);
        tipoGroup.add(rbFiltros);

        ActionListener tipoListener = e -> applyFilters();
        rbTodos.addActionListener(tipoListener);
        rbLubricantes.addActionListener(tipoListener);
        rbFiltros.addActionListener(tipoListener);

        JLabel lblTipoEspecifico = new JLabel("Especificación");
        lblTipoEspecifico.setFont(new Font("Segoe UI", Font.BOLD, 25));
        lblTipoEspecifico.setForeground(Color.BLACK);
        lblTipoEspecifico.setAlignmentX(Component.LEFT_ALIGNMENT);

        typeSpecificSearchField = createStyledSearchField("Tipo");
        typeSpecificSearchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                applyFilters();
            }
        });

        JLabel lblPrecio = new JLabel("Precio");
        lblPrecio.setFont(new Font("Segoe UI", Font.BOLD, 25));
        lblPrecio.setForeground(Color.BLACK);
        lblPrecio.setAlignmentX(Component.LEFT_ALIGNMENT);

        rbMayorPrecio = createStyledRadio("Mayor precio");
        rbMenorPrecio = createStyledRadio("Menor precio");
        ButtonGroup precioGroup = new ButtonGroup();
        precioGroup.add(rbMayorPrecio);
        precioGroup.add(rbMenorPrecio);
        ActionListener precioListener = e -> applyFilters();
        rbMayorPrecio.addActionListener(precioListener);
        rbMenorPrecio.addActionListener(precioListener);

        panel.add(lblTitle);
        panel.add(Box.createVerticalStrut(15));
        panel.add(lblBusqueda);
        panel.add(searchField);
        panel.add(Box.createVerticalStrut(15));

        panel.add(lblMarca);
        panel.add(brandSearchField);
        panel.add(Box.createVerticalStrut(15));

        panel.add(lblTipoProducto);
        panel.add(rbTodos);
        panel.add(rbLubricantes);
        panel.add(rbFiltros);
        panel.add(Box.createVerticalStrut(15));

        panel.add(lblTipoEspecifico);
        panel.add(typeSpecificSearchField);
        panel.add(Box.createVerticalStrut(15));

        panel.add(lblPrecio);
        panel.add(rbMayorPrecio);
        panel.add(rbMenorPrecio);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JTextField createStyledSearchField(String prompt) {
        JTextField field = new JTextField();
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        new TextPrompt(prompt, field);
        field.setBorder(new CompoundBorder(
                new LineBorder(Color.GRAY, 1, true),
                new EmptyBorder(5, 8, 5, 8)));
        return field;
    }

    private JRadioButton createStyledRadio(String text) {
        JRadioButton rb = new JRadioButton(text);
        rb.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        rb.setForeground(Color.BLACK);
        rb.setBackground(GlobalView.GENERAL_BACKGROUND);
        rb.setFocusPainted(false);
        rb.setAlignmentX(Component.LEFT_ALIGNMENT);
        rb.setCursor(new Cursor(Cursor.HAND_CURSOR));
        rb.setBorder(new EmptyBorder(4, 0, 4, 0));
        rb.setIconTextGap(10);
        rb.setIcon(RADIO_DEFAULT_ICON);
        rb.setSelectedIcon(RADIO_SELECTED_ICON);
        rb.setRolloverIcon(RADIO_HOVER_ICON);
        rb.setRolloverSelectedIcon(RADIO_SELECTED_HOVER_ICON);
        return rb;
    }

    private static Icon createRadioButtonIcon(boolean isSelected, boolean isHover) {
        int size = 18;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(isHover ? Color.BLACK : Color.GRAY);
        g2.setStroke(new BasicStroke(1.8f));
        g2.drawOval(1, 1, size - 3, size - 3);
        if (isSelected) {
            g2.setColor(isHover ? GlobalView.ASIDE_BACKGROUND.darker() : GlobalView.ASIDE_BACKGROUND);
            g2.fillOval(5, 5, size - 10, size - 10);
        }
        g2.dispose();
        return new ImageIcon(image);
    }

    private void applyFilters() {
        String search = searchField.getText().trim().toLowerCase();

        String brandSearch = brandSearchField.getText().trim().toLowerCase();
        String typeSearch = typeSpecificSearchField.getText().trim().toLowerCase();

        filteredProducts = allProducts.stream()
                .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(search))

                .filter(p -> brandSearch.isEmpty()
                        || (p.getBrand() != null && p.getBrand().toLowerCase().contains(brandSearch)))

                .filter(p -> {
                    if (rbLubricantes.isSelected())
                        return p instanceof Lubricant;
                    if (rbFiltros.isSelected())
                        return p instanceof Filters;
                    return true;
                })

                .filter(p -> {
                    if (typeSearch.isEmpty())
                        return true;
                    if (p instanceof Lubricant) {
                        String lubType = ((Lubricant) p).getType();
                        return (lubType != null && lubType.toLowerCase().contains(typeSearch));
                    } else if (p instanceof Filters) {
                        String filterType = ((Filters) p).getFilterType();
                        return (filterType != null && filterType.toLowerCase().contains(typeSearch));
                    }
                    return false;
                })
                .collect(Collectors.toList());

        if (rbMayorPrecio.isSelected()) {
            filteredProducts.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
        } else if (rbMenorPrecio.isSelected()) {
            filteredProducts.sort(Comparator.comparingDouble(Product::getPrice));
        }

        int totalPages = (int) Math.ceil((double) filteredProducts.size() / ITEMS_PER_PAGE);
        if (totalPages == 0)
            totalPages = 1;
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }

        updateGrid();
        updatePagination();
    }

    private void updateGrid() {
        gridPanel.removeAll();
        int start = (currentPage - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, filteredProducts.size());

        for (int i = start; i < end; i++) {
            gridPanel.add(createCard(filteredProducts.get(i)));
        }

        for (int i = end; i < start + ITEMS_PER_PAGE; i++) {
            JPanel emptyPanel = new JPanel();
            emptyPanel.setBackground(GlobalView.GENERAL_BACKGROUND);
            gridPanel.add(emptyPanel);
        }

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private JPanel createCard(Product product) {
        JPanel card = new JPanel(new BorderLayout(0, 5));
        card.setBackground(GlobalView.CARDS_BACKGROUND);
        card.setBorder(new CompoundBorder(
                new LineBorder(Color.LIGHT_GRAY, 1, true),
                new EmptyBorder(10, 10, 10, 10)));

        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);

        try {
            ImageIcon icon = createIcon(product.getImagePath(), 100, 100);
            if (icon == null) {
                imageLabel.setText("Sin imagen");
                imageLabel.setFont(GlobalView.TABLE_BODY_FONT);
            } else {
                imageLabel.setIcon(icon);
            }
        } catch (Exception e) {
            imageLabel.setText("Sin imagen");
        }

        JPanel infoPanel = new JPanel(new GridLayout(5, 1));
        infoPanel.setBackground(GlobalView.CARDS_BACKGROUND);

        String nameAsHtml = "<html><div style='width: 130px; text-align: center;'>" +
                product.getName() +
                "</div></html>";

        infoPanel.add(new JLabel(nameAsHtml) {
            {
                setFont(new Font("Segoe UI", Font.BOLD, 12));

            }
        });
        infoPanel.add(new JLabel("Marca: " + product.getBrand(), SwingConstants.CENTER) {
            {
                setFont(new Font("Segoe UI", Font.PLAIN, 12));
            }
        });

        String specText = "";
        if (product instanceof Lubricant) {
            specText = "Tipo: " + ((Lubricant) product).getType();
        } else if (product instanceof Filters) {
            specText = "Tipo: " + ((Filters) product).getFilterType();
        }
        infoPanel.add(new JLabel(specText, SwingConstants.CENTER) {
            {
                setFont(new Font("Segoe UI", Font.PLAIN, 12));
            }
        });

        String precioFormateado = String.format("%,.0f", product.getPrice());
        infoPanel.add(new JLabel("Precio: $" + precioFormateado, SwingConstants.CENTER) {
            {
                setFont(new Font("Segoe UI", Font.PLAIN, 12));
            }
        });
        infoPanel.add(new JLabel("Existencias: " + product.getstock() + " unidades", SwingConstants.CENTER) {
            {
                setFont(new Font("Segoe UI", Font.ITALIC, 11));
                setForeground(Color.GRAY);
            }
        });

        card.add(imageLabel, BorderLayout.NORTH);
        card.add(infoPanel, BorderLayout.CENTER);

        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectedProduct = product;
                editBtn.setEnabled(true);
                deleteBtn.setEnabled(true);
                updateGrid();
            }
        });

        if (product.equals(selectedProduct)) {
            card.setBorder(new CompoundBorder(
                    new LineBorder(Color.RED, 3, true),
                    new EmptyBorder(8, 8, 8, 8)));
        }

        return card;
    }

    private int showProductTypeSelector() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Seleccionar Tipo",
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(GlobalView.GENERAL_BACKGROUND_LIGHT);
        dialog.setSize(400, 230);
        dialog.setLocationRelativeTo(this);
        dialog.setShape(new java.awt.geom.RoundRectangle2D.Double(0, 0, 400, 230, 25, 25));

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(GlobalView.GENERAL_BACKGROUND_LIGHT);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JLabel title = new JLabel("Seleccione el tipo de producto");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(GlobalView.DARK_PRIMARY_TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(title);
        mainPanel.add(Box.createVerticalStrut(25));

        JButton btnLubricante = new JButton("Lubricante");
        JButton btnFiltro = new JButton("Filtro");

        for (JButton btn : new JButton[] { btnLubricante, btnFiltro }) {
            btn.setFocusPainted(false);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 18));
            btn.setBackground(GlobalView.ASIDE_BACKGROUND);
            btn.setForeground(Color.WHITE);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(new LineBorder(GlobalView.DARK_PRIMARY_TEXT, 1, true));
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
            btn.setPreferredSize(new Dimension(150, 45));
            btn.setMaximumSize(new Dimension(150, 45));
            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    btn.setBackground(GlobalView.ASIDE_BUTTONS_HOVER_COLOR);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    btn.setBackground(GlobalView.ASIDE_BACKGROUND);
                }
            });
            mainPanel.add(btn);
            mainPanel.add(Box.createVerticalStrut(15));
        }

        final int[] result = { -1 };
        btnLubricante.addActionListener(e -> {
            result[0] = 0;
            dialog.dispose();
        });
        btnFiltro.addActionListener(e -> {
            result[0] = 1;
            dialog.dispose();
        });

        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.setVisible(true);
        return result[0];
    }

    private void updatePagination() {
        paginationPanel.removeAll();
        int total = (int) Math.ceil((double) filteredProducts.size() / ITEMS_PER_PAGE);
        if (total == 0)
            total = 1;
        final int totalPages = total;

        JPanel container = new JPanel(new BorderLayout(120, 0));
        container.setBackground(GlobalView.GENERAL_BACKGROUND);
        container.setBorder(new EmptyBorder(10, 0, 10, 60));

        JPanel pagesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 5));
        pagesPanel.setBackground(GlobalView.GENERAL_BACKGROUND);

        JButton firstBtn = createStyledButton("<<");
        JButton prevBtn = createStyledButton("<");
        JButton nextBtn = createStyledButton(">");
        JButton lastBtn = createStyledButton(">>");
        for (JButton btn : new JButton[] { firstBtn, prevBtn, nextBtn, lastBtn }) {
            btn.setPreferredSize(new Dimension(40, 40));
            btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        }
        firstBtn.addActionListener(e -> {
            currentPage = 1;
            updateGrid();
            updatePagination();
        });
        prevBtn.addActionListener(e -> {
            if (currentPage > 1)
                currentPage--;
            updateGrid();
            updatePagination();
        });
        nextBtn.addActionListener(e -> {
            if (currentPage < totalPages)
                currentPage++;
            updateGrid();
            updatePagination();
        });
        lastBtn.addActionListener(e -> {
            currentPage = totalPages;
            updateGrid();
            updatePagination();
        });
        JLabel pagLabel = new JLabel("Página ");
        pagLabel.setFont(new Font("Segoe UI", Font.BOLD, 25));
        pagesPanel.add(pagLabel);
        pagesPanel.add(firstBtn);
        pagesPanel.add(prevBtn);
        int maxVisible = 4;
        int startPage = Math.max(1, currentPage - 1);
        int endPage = Math.min(totalPages, startPage + maxVisible - 1);
        if (endPage - startPage < maxVisible - 1)
            startPage = Math.max(1, endPage - maxVisible + 1);
        if (startPage > 1)
            pagesPanel.add(new JLabel("..."));
        for (int i = startPage; i <= endPage; i++) {
            JButton pageBtn = createStyledButton(String.valueOf(i));
            final int page = i;
            pageBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));
            pageBtn.setPreferredSize(new Dimension(45, 45));
            pageBtn.setBackground(
                    i == currentPage ? GlobalView.ASIDE_BACKGROUND : GlobalView.ASIDE_BUTTONS_ACTIVE_BACKGROUND);
            pageBtn.addActionListener(e -> {
                currentPage = page;
                updateGrid();
                updatePagination();
            });
            pagesPanel.add(pageBtn);
        }
        if (endPage < totalPages)
            pagesPanel.add(new JLabel("..."));
        pagesPanel.add(nextBtn);
        pagesPanel.add(lastBtn);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 5));
        actionPanel.setBackground(GlobalView.GENERAL_BACKGROUND);

        editBtn = new JButton(createIcon(p.getProperties("edit2"), 24, 24));
        deleteBtn = new JButton(createIcon(p.getProperties("delete2"), 24, 24));
        addBtn = new JButton(createIcon(p.getProperties("add"), 24, 24));
        if (editBtn.getIcon() == null)
            editBtn.setText("Editar");
        if (deleteBtn.getIcon() == null)
            deleteBtn.setText("Eliminar");
        if (addBtn.getIcon() == null)
            addBtn.setText("Añadir");

        for (JButton btn : new JButton[] { editBtn, deleteBtn, addBtn }) {
            btn.setBackground(GlobalView.ASIDE_BUTTONS_ACTIVE_BACKGROUND);
            btn.setFocusPainted(false);
            btn.setBorder(new LineBorder(Color.GRAY, 1, true));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setPreferredSize(new Dimension(45, 45));
        }
        editBtn.setEnabled(selectedProduct != null);
        deleteBtn.setEnabled(selectedProduct != null);

        editBtn.addActionListener(e -> {
            if (selectedProduct == null)
                return;
            String title = (selectedProduct instanceof Lubricant) ? "Editar Lubricante" : "Editar Filtro";

            EditCreateProductPanel editPanel = new EditCreateProductPanel(
                    title,
                    selectedProduct,
                    (updatedProduct) -> {
                        boolean success = presenter.saveLubricantOrFilter(updatedProduct);
                        if (success) {
                            loadDataFromPresenter();
                            applyFilters();
                            SuccessPopUp.showSuccessPopup((Frame) SwingUtilities.getWindowAncestor(this), "Éxito:",
                                    "Producto actualizado.");
                        } else {
                            ConfirmDialog.showErrorDialog(SwingUtilities.getWindowAncestor(this),
                                    "Error al guardar el producto.", "Error");
                        }
                    },
                    this, controller, presenter);
            showDialog(editPanel, title);
        });

        deleteBtn.addActionListener(e -> {
            if (selectedProduct == null)
                return;
            Frame frame = (Frame) SwingUtilities.getWindowAncestor(this);
            boolean confirmed = ConfirmDialog.showConfirmDialog(frame, "¿Deseas eliminar este producto?",
                    "Confirmar eliminación");

            if (confirmed) {

                boolean success = presenter.requestLubricantDeletion(selectedProduct.getId());
                if (success) {
                    loadDataFromPresenter();
                    applyFilters();
                    selectedProduct = null;
                    editBtn.setEnabled(false);
                    deleteBtn.setEnabled(false);
                    SuccessPopUp.showSuccessPopup(frame, "Éxito:", "El producto se eliminó exitosamente.");
                } else {
                    ConfirmDialog.showErrorDialog(frame, "No se pudo eliminar el producto.", "Error");
                }
            }
        });

        addBtn.addActionListener(e -> {

            int choice = showProductTypeSelector();
            Product newProduct;
            String title;

            if (choice == 0) {
                newProduct = new Lubricant(null, null, "", 0, "", "");
                title = "Crear Lubricante";
            } else if (choice == 1) {
                newProduct = new Filters(null, null, "", 0, "", "", "");
                title = "Crear Filtro";
            } else {
                return;
            }

            EditCreateProductPanel addPanel = new EditCreateProductPanel(
                    title,
                    newProduct,
                    (addedProduct) -> {
                        boolean success = presenter.saveLubricantOrFilter(addedProduct);
                        if (success) {
                            loadDataFromPresenter();
                            applyFilters();
                            SuccessPopUp.showSuccessPopup((Frame) SwingUtilities.getWindowAncestor(this), "Éxito:",
                                    "El producto se añadió exitosamente.");
                        } else {
                            ConfirmDialog.showErrorDialog(SwingUtilities.getWindowAncestor(this),
                                    "Error al crear el producto.", "Error");
                        }
                    },
                    this, controller, presenter);
            showDialog(addPanel, title);
        });

        actionPanel.add(editBtn);
        actionPanel.add(deleteBtn);
        actionPanel.add(addBtn);

        container.add(pagesPanel, BorderLayout.WEST);
        container.add(actionPanel, BorderLayout.EAST);
        paginationPanel.add(container);
        paginationPanel.revalidate();
        paginationPanel.repaint();
    }

    private void showDialog(JPanel panel, String title) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), title,
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setContentPane(panel);
        Point location = this.getLocationOnScreen();
        Dimension size = this.getSize();
        dialog.setBounds(location.x, location.y, size.width, size.height);
        dialog.setVisible(true);
    }

    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(GlobalView.ASIDE_BUTTONS_ACTIVE_BACKGROUND);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(Color.DARK_GRAY, 1, true));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(6, 12, 6, 12));
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(GlobalView.ASIDE_BACKGROUND);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!(btn.getText().equals(String.valueOf(currentPage))))
                    btn.setBackground(GlobalView.ASIDE_BUTTONS_ACTIVE_BACKGROUND);
            }
        });
        return btn;
    }

    private ImageIcon createIcon(String path, int width, int height) {
        if (path == null || path.trim().isEmpty())
            return null;

        Image img = null;

        try {

            java.net.URL resourceUrl = getClass().getResource(path);
            if (resourceUrl != null) {
                img = new ImageIcon(resourceUrl).getImage();
            } else {

                java.io.File file = new java.io.File(path);
                if (file.exists()) {
                    img = new ImageIcon(path).getImage();
                }
            }

            if (img == null) {
                System.err.println("No se pudo cargar la imagen: " + path);
                return null;
            }

            Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);

        } catch (Exception e) {
            System.err.println("Excepción al cargar imagen: " + path);
            e.printStackTrace();
            return null;
        }
    }

    public void refreshData() {
        loadDataFromPresenter();
        applyFilters();
    }
}