package co.edu.uptc.view.panels.SubPanels;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import co.edu.uptc.presenter.Presenter;
import co.edu.uptc.models.products.*;
import co.edu.uptc.view.GlobalView;
import co.edu.uptc.view.dialogs.ConfirmDialog;
import co.edu.uptc.view.dialogs.SuccessPopUp;
import co.edu.uptc.view.utils.ViewController;

public class EditCreateProductPanel extends JPanel {

    private JTextField nombreField, marcaField, precioField, stockField, imagenField;
    private JLabel imagenPreview;
    private JPanel specificFieldsPanel;
    private Map<String, JComponent> specificFieldsMap;
    private Consumer<Product> onSaveCallback;
    private JPanel parentPanel;
    private ViewController controller;
    private Presenter presenter;
    private Product product;
    private boolean isCreateMode;

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_BRAND_LENGTH = 50;
    private static final int MAX_SPEC_LENGTH = 50;
    private static final Pattern GENERAL_TEXT_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9\\s.'-ñÑáéíóúÁÉÍÓÚüÜ/]+$");
    private static final long MIN_PRICE = 0L;
    private static final long MAX_PRICE = 100_000_000L;
    private static final int MIN_STOCK = 0;
    private static final int MAX_STOCK = 999;

    public EditCreateProductPanel(String titleText, Product product, Consumer<Product> onSaveCallback,
            JPanel parentPanel, ViewController controller, Presenter presenter) {
        this.onSaveCallback = onSaveCallback;
        this.parentPanel = parentPanel;
        this.controller = controller;
        this.presenter = presenter;
        this.product = product;
        this.specificFieldsMap = new HashMap<>();

        this.isCreateMode = (product.getId() == null || product.getId().trim().isEmpty());

        if (isCreateMode) {
            String newId = "prod-" + UUID.randomUUID().toString().substring(0, 8);
            product.setId(newId);
        }

        initComponents(titleText);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    Window win = SwingUtilities.getWindowAncestor(EditCreateProductPanel.this);
                    if (win instanceof JDialog) {
                        JDialog dialog = (JDialog) win;
                        if (parentPanel != null && parentPanel.isShowing()) {
                            Point locationOnScreen = parentPanel.getLocationOnScreen();
                            Dimension size = parentPanel.getSize();
                            dialog.setBounds(locationOnScreen.x, locationOnScreen.y, size.width, size.height);
                        } else {
                            dialog.pack();
                            dialog.setLocationRelativeTo(null);
                        }
                    }
                    EditCreateProductPanel.this.removeComponentListener(this);
                });
            }
        });
    }

    private void initComponents(String titleText) {
        setLayout(new BorderLayout(20, 20));
        setBackground(GlobalView.GENERAL_BACKGROUND);
        setBorder(new EmptyBorder(15, 25, 15, 25));

        JLabel title = new JLabel(titleText, SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(Color.BLACK);
        title.setBorder(new EmptyBorder(0, 0, 10, 0));
        add(title, BorderLayout.NORTH);

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setOpaque(false);
        mainSplitPane.setBackground(GlobalView.GENERAL_BACKGROUND);
        mainSplitPane.setBorder(null);
        mainSplitPane.setResizeWeight(0.6);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setOpaque(false);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(GlobalView.GENERAL_BACKGROUND);
        formPanel.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(GlobalView.BORDER_COLOR, 1, true),
                " Datos del Producto ", TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, GlobalView.TABLE_HEADER_FONT,
                GlobalView.TEXT_COLOR));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.1;
        formPanel.add(createLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.9;
        nombreField = createStyledField(product.getName() != null ? product.getName() : "");
        addStandardFocusListener(nombreField);
        formPanel.add(nombreField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(createLabel("Marca:"), gbc);
        gbc.gridx = 1;
        marcaField = createStyledField(product.getBrand() != null ? product.getBrand() : "");
        addStandardFocusListener(marcaField);
        formPanel.add(marcaField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(createLabel("Precio:"), gbc);
        gbc.gridx = 1;
        precioField = createStyledField(String.valueOf((long) product.getPrice()));
        addStandardFocusListener(precioField);
        formPanel.add(precioField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(createLabel("Existencias:"), gbc);
        gbc.gridx = 1;
        stockField = createStyledField(String.valueOf(product.getstock()));
        addStandardFocusListener(stockField);
        formPanel.add(stockField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(createLabel("Ruta de imagen:"), gbc);
        gbc.gridx = 1;
        formPanel.add(createImageSelectionPanel(product.getImagePath() != null ? product.getImagePath() : ""), gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        specificFieldsPanel = new JPanel(new GridBagLayout());
        specificFieldsPanel.setOpaque(false);
        specificFieldsPanel.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(GlobalView.BORDER_COLOR, 1, true),
                " Propiedades Específicas ", TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, GlobalView.TABLE_HEADER_FONT,
                GlobalView.TEXT_COLOR));
        createSpecificFields(product);
        formPanel.add(specificFieldsPanel, gbc);

        leftPanel.add(formPanel, BorderLayout.CENTER);
        mainSplitPane.setLeftComponent(leftPanel);

        JPanel imagePreviewPanel = new JPanel(new GridBagLayout());
        imagePreviewPanel.setOpaque(false);
        imagePreviewPanel.setBorder(new TitledBorder(
                new LineBorder(Color.LIGHT_GRAY, 1, true),
                " Previsualización ", TitledBorder.CENTER, TitledBorder.TOP,
                new Font("Segoe UI", Font.PLAIN, 14), Color.DARK_GRAY));
        imagenPreview = new JLabel("Sin imagen", SwingConstants.CENTER);
        imagenPreview.setPreferredSize(new Dimension(300, 300));
        updateImagePreview(product.getImagePath());
        GridBagConstraints imgGbc = new GridBagConstraints();
        imgGbc.anchor = GridBagConstraints.CENTER;
        imgGbc.weightx = 1.0;
        imgGbc.weighty = 1.0;
        imagePreviewPanel.add(imagenPreview, imgGbc);
        mainSplitPane.setRightComponent(imagePreviewPanel);

        mainSplitPane.setDividerLocation(0.6);
        mainSplitPane.setDividerSize(10);

        add(mainSplitPane, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    private void createSpecificFields(Product p) {
        specificFieldsMap.clear();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.weightx = 0.1;
        gbc.gridy = 0;
        int gridY = 0;
        if (p instanceof Tire) {
            Tire tire = (Tire) p;
            JTextField sizeField = createStyledField(tire.getSize() != null ? tire.getSize() : "");
            JTextField typeField = createStyledField(tire.getType() != null ? tire.getType() : "");
            addStandardFocusListener(sizeField);
            addStandardFocusListener(typeField);
            gbc.gridy = gridY;
            gbc.gridx = 0;
            specificFieldsPanel.add(createLabel("Tamaño:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 0.9;
            specificFieldsMap.put("size", sizeField);
            specificFieldsPanel.add(sizeField, gbc);
            gbc.gridy = ++gridY;
            gbc.gridx = 0;
            specificFieldsPanel.add(createLabel("Tipo:"), gbc);
            gbc.gridx = 1;
            specificFieldsMap.put("type", typeField);
            specificFieldsPanel.add(typeField, gbc);
        } else if (p instanceof Battery) {
            Battery battery = (Battery) p;
            JTextField voltageField = createStyledField(battery.getVoltage() != null ? battery.getVoltage() : "");
            JTextField capacityField = createStyledField(battery.getCapacity() != null ? battery.getCapacity() : "");
            addStandardFocusListener(voltageField);
            addStandardFocusListener(capacityField);
            gbc.gridy = gridY;
            gbc.gridx = 0;
            specificFieldsPanel.add(createLabel("Voltaje:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 0.9;
            specificFieldsMap.put("voltage", voltageField);
            specificFieldsPanel.add(voltageField, gbc);
            gbc.gridy = ++gridY;
            gbc.gridx = 0;
            specificFieldsPanel.add(createLabel("Capacidad:"), gbc);
            gbc.gridx = 1;
            specificFieldsMap.put("capacity", capacityField);
            specificFieldsPanel.add(capacityField, gbc);
        } else if (p instanceof BrakePad) {
            BrakePad brakePad = (BrakePad) p;
            JTextField materialField = createStyledField(
                    brakePad.getMaterialType() != null ? brakePad.getMaterialType() : "");
            JTextField compatField = createStyledField(
                    brakePad.getVehicleCompatibility() != null ? brakePad.getVehicleCompatibility() : "");
            addStandardFocusListener(materialField);
            addStandardFocusListener(compatField);
            gbc.gridy = gridY;
            gbc.gridx = 0;
            specificFieldsPanel.add(createLabel("Material:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 0.9;
            specificFieldsMap.put("materialType", materialField);
            specificFieldsPanel.add(materialField, gbc);
            gbc.gridy = ++gridY;
            gbc.gridx = 0;
            specificFieldsPanel.add(createLabel("Compatibilidad:"), gbc);
            gbc.gridx = 1;
            specificFieldsMap.put("vehicleCompatibility", compatField);
            specificFieldsPanel.add(compatField, gbc);
        } else if (p instanceof Filters) {
            Filters filter = (Filters) p;
            JTextField vehicleTypeField = createStyledField(
                    filter.getVehicleType() != null ? filter.getVehicleType() : "");
            JTextField filterTypeField = createStyledField(
                    filter.getFilterType() != null ? filter.getFilterType() : "");
            addStandardFocusListener(vehicleTypeField);
            addStandardFocusListener(filterTypeField);
            gbc.gridy = gridY;
            gbc.gridx = 0;
            specificFieldsPanel.add(createLabel("Tipo Vehículo:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 0.9;
            specificFieldsMap.put("vehicleType", vehicleTypeField);
            specificFieldsPanel.add(vehicleTypeField, gbc);
            gbc.gridy = ++gridY;
            gbc.gridx = 0;
            specificFieldsPanel.add(createLabel("Tipo Filtro:"), gbc);
            gbc.gridx = 1;
            specificFieldsMap.put("filterType", filterTypeField);
            specificFieldsPanel.add(filterTypeField, gbc);
        } else if (p instanceof Lubricant) {
            Lubricant lubricant = (Lubricant) p;
            JTextField typeField = createStyledField(lubricant.getType() != null ? lubricant.getType() : "");
            addStandardFocusListener(typeField);
            gbc.gridy = gridY;
            gbc.gridx = 0;
            specificFieldsPanel.add(createLabel("Tipo Lubricante:"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 0.9;
            specificFieldsMap.put("type", typeField);
            specificFieldsPanel.add(typeField, gbc);
        }
        gbc.gridy = ++gridY;
        gbc.weighty = 1.0;
        specificFieldsPanel.add(new JPanel() {
            {
                setOpaque(false);
            }
        }, gbc);
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

    private JPanel createImageSelectionPanel(String initialPath) {
        imagenField = createStyledField(initialPath);
        addStandardFocusListener(imagenField);
        JButton browseBtn = new JButton("Seleccionar...");
        browseBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        browseBtn.setFocusPainted(false);
        browseBtn.setBackground(GlobalView.ASIDE_BACKGROUND);
        browseBtn.setForeground(Color.WHITE);
        browseBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        browseBtn.addActionListener(e -> chooseImage());
        JPanel imgPanel = new JPanel(new BorderLayout(5, 0));
        imgPanel.setBackground(GlobalView.GENERAL_BACKGROUND);
        imgPanel.add(imagenField, BorderLayout.CENTER);
        imgPanel.add(browseBtn, BorderLayout.EAST);
        return imgPanel;
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

    private void chooseImage() {

        LookAndFeel originalLaf = UIManager.getLookAndFeel();
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            JFileChooser chooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
            chooser.setDialogTitle("Seleccionar una imagen");
            chooser.setAcceptAllFileFilterUsed(false);
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Imágenes (JPG, PNG, GIF)", "jpg", "jpeg",
                    "png", "gif");
            chooser.addChoosableFileFilter(filter);
            SwingUtilities.updateComponentTreeUI(chooser);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                try {
                    Path destinationDir = Paths.get(System.getProperty("user.dir"), "data", "images");
                    Files.createDirectories(destinationDir);
                    String originalName = selectedFile.getName();
                    String extension = "";
                    int i = originalName.lastIndexOf('.');
                    if (i > 0) {
                        extension = originalName.substring(i);
                    }
                    String newFileName = product.getId() + extension;
                    Path destinationPath = destinationDir.resolve(newFileName);
                    Files.copy(selectedFile.toPath(), destinationPath, StandardCopyOption.REPLACE_EXISTING);
                    String relativePath = "data/images/" + newFileName;
                    imagenField.setText(relativePath);
                    updateImagePreview(relativePath);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    ConfirmDialog.showErrorDialog(SwingUtilities.getWindowAncestor(this),
                            "Error al copiar la imagen: " + ex.getMessage(), "Error de Archivo");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ConfirmDialog.showErrorDialog(SwingUtilities.getWindowAncestor(this),
                    "No se pudo abrir el selector de archivos.", "Error de UI");
        } finally {
            try {
                UIManager.setLookAndFeel(originalLaf);
            } catch (UnsupportedLookAndFeelException ex) {
            }
        }
    }

    private void updateImagePreview(String path) {

        if (path == null || path.trim().isEmpty()) {
            imagenPreview.setIcon(null);
            imagenPreview.setText("Sin imagen");
            return;
        }
        try {
            ImageIcon icon = new ImageIcon(path);
            if (icon.getImage() == null || icon.getIconWidth() == -1) {
                imagenPreview.setIcon(null);
                imagenPreview.setText("Vista previa no disponible");
                return;
            }
            Image scaled = icon.getImage().getScaledInstance(300, 300, Image.SCALE_SMOOTH);
            imagenPreview.setIcon(new ImageIcon(scaled));
            imagenPreview.setText(null);
        } catch (Exception e) {
            imagenPreview.setIcon(null);
            imagenPreview.setText("Vista previa no disponible");
        }
    }

    /**
     * Valida un campo de texto (String) según las reglas.
     * Añade errores al StringBuilder si falla.
     */
    private boolean validateStringField(JTextField field, String fieldName, int maxLength, Pattern pattern,
            StringBuilder errors, boolean isRequired) {
        String text = field.getText().trim();
        if (text.isEmpty()) {
            if (isRequired) {
                errors.append("- El campo '").append(fieldName).append("' es obligatorio.\n");
                return false;
            }
            return true;
        }

        if (text.length() > maxLength) {
            errors.append("- El '").append(fieldName).append("' no debe exceder los ").append(maxLength)
                    .append(" caracteres.\n");
            return false;
        }

        if (pattern != null && !pattern.matcher(text).matches()) {
            errors.append("- El '").append(fieldName).append("' contiene caracteres no válidos.\n");
            return false;
        }
        return true;
    }

    /**
     * Valida un campo numérico (long) según las reglas.
     * Añade errores al StringBuilder si falla.
     */
    private Long parseAndValidateLong(JTextField field, String fieldName, long min, long max,
            StringBuilder errors, boolean isRequired) {
        String text = field.getText().trim();
        if (text.isEmpty()) {
            if (isRequired) {
                errors.append("- El campo '").append(fieldName).append("' es obligatorio.\n");
                return null;
            }
            return 0L;
        }

        try {
            long value = Long.parseLong(text);

            if (value < min) {
                errors.append("- El '").append(fieldName).append("' no puede ser negativo.\n");
                return null;
            }

            if (value > max) {
                errors.append("- El '").append(fieldName).append("' es demasiado grande (máx: ").append(max)
                        .append(").\n");
                return null;
            }

            return value;

        } catch (NumberFormatException ex) {
            errors.append("- El '").append(fieldName)
                    .append("' debe ser un número entero válido (sin puntos, comas o letras).\n");
            return null;
        }
    }

    /**
     * Valida un campo numérico (int) según las reglas.
     * Añade errores al StringBuilder si falla.
     */
    private Integer parseAndValidateInt(JTextField field, String fieldName, int min, int max,
            StringBuilder errors, boolean isRequired) {

        Long longValue = parseAndValidateLong(field, fieldName, min, max, errors, isRequired);

        if (longValue == null) {
            return null;
        }

        if (longValue > Integer.MAX_VALUE) {
            errors.append("- El '").append(fieldName).append("' es demasiado grande (máx: ").append(max).append(").\n");
            return null;
        }

        return longValue.intValue();
    }

    /**
     * MÉTODO DE VALIDACIÓN PRINCIPAL (MODIFICADO)
     * Ahora usa los helpers de blindaje.
     */
    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        validateStringField(nombreField, "Nombre", MAX_NAME_LENGTH, GENERAL_TEXT_PATTERN, errors, true);
        validateStringField(marcaField, "Marca", MAX_BRAND_LENGTH, GENERAL_TEXT_PATTERN, errors, true);

        Long precioValidado = parseAndValidateLong(precioField, "Precio", MIN_PRICE, MAX_PRICE, errors, true);
        Integer stockValidado = parseAndValidateInt(stockField, "Existencias", MIN_STOCK, MAX_STOCK, errors, true);

        for (Map.Entry<String, JComponent> entry : specificFieldsMap.entrySet()) {
            String key = entry.getKey();
            JTextField field = (JTextField) entry.getValue();

            String fieldName = key.substring(0, 1).toUpperCase() + key.substring(1);
            validateStringField(field, fieldName, MAX_SPEC_LENGTH, GENERAL_TEXT_PATTERN, errors, true);
        }

        if (errors.length() > 0) {
            ConfirmDialog.showErrorDialog(SwingUtilities.getWindowAncestor(this),
                    "Por favor corrija los siguientes errores:\n" + errors.toString(),
                    "Errores de validación");
            return false;
        }

        precioField.setText(String.valueOf(precioValidado));
        stockField.setText(String.valueOf(stockValidado));
        nombreField.setText(nombreField.getText().trim());
        marcaField.setText(marcaField.getText().trim());

        return true;
    }

    private void saveChanges() {
        if (!validateInput()) {
            return;
        }
        try {

            String id = this.product.getId();
            String name = nombreField.getText().trim();
            String brand = marcaField.getText().trim();
            double price = Double.parseDouble(precioField.getText().trim());
            int stock = Integer.parseInt(stockField.getText().trim());
            String imagePath = imagenField.getText().trim();

            Product updatedProduct = null;

            if (product instanceof Tire) {
                String size = ((JTextField) specificFieldsMap.get("size")).getText().trim();
                String type = ((JTextField) specificFieldsMap.get("type")).getText().trim();
                updatedProduct = new Tire(imagePath, id, name, price, size, type, brand);
            } else if (product instanceof Battery) {
                String voltage = ((JTextField) specificFieldsMap.get("voltage")).getText().trim();
                String capacity = ((JTextField) specificFieldsMap.get("capacity")).getText().trim();
                updatedProduct = new Battery(imagePath, id, name, price, voltage, capacity, brand);
            } else if (product instanceof BrakePad) {
                String materialType = ((JTextField) specificFieldsMap.get("materialType")).getText().trim();
                String vehicleCompatibility = ((JTextField) specificFieldsMap.get("vehicleCompatibility")).getText()
                        .trim();
                updatedProduct = new BrakePad(imagePath, id, name, brand, price, materialType, vehicleCompatibility);
            } else if (product instanceof Filters) {
                String vehicleType = ((JTextField) specificFieldsMap.get("vehicleType")).getText().trim();
                String filterType = ((JTextField) specificFieldsMap.get("filterType")).getText().trim();
                updatedProduct = new Filters(imagePath, id, name, price, vehicleType, filterType, brand);
            } else if (product instanceof Lubricant) {
                String type = ((JTextField) specificFieldsMap.get("type")).getText().trim();
                updatedProduct = new Lubricant(imagePath, id, name, price, type, brand);
            }

            if (updatedProduct == null) {
                ConfirmDialog.showErrorDialog(SwingUtilities.getWindowAncestor(this),
                        "Tipo de producto desconocido. No se puede guardar.", "Error");
                return;
            }

            updatedProduct.setstock(stock);

            String message = isCreateMode ? "¿Desea crear este nuevo producto?"
                    : "¿Desea guardar los cambios realizados?";
            boolean confirmed = ConfirmDialog.showConfirmDialog(SwingUtilities.getWindowAncestor(this), message,
                    "Confirmación");

            if (confirmed) {
              onSaveCallback.accept(updatedProduct);
            closePanel();

            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            Frame frameParent = (parentWindow instanceof Frame) ? (Frame) parentWindow : null;

            String successMessage = isCreateMode 
                    ? "El producto fue creado exitosamente." 
                    : "El producto se actualizó exitosamente.";

                    SuccessPopUp.showSuccessPopup(frameParent,
                    "Éxito:", successMessage);
        }

        } catch (Exception ex) {
            ex.printStackTrace();
            ConfirmDialog.showErrorDialog(SwingUtilities.getWindowAncestor(this),
                    "Ocurrió un error al guardar los datos:\n" + ex.getMessage(), "Error inesperado");
        }
    }

    private void closePanel() {
        Window win = SwingUtilities.getWindowAncestor(this);
        if (win instanceof JDialog) {
            ((JDialog) win).dispose();
        } else {
            controller.showPanel(parentPanel);
        }
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
                if (c instanceof JTextComponent) {
                    return ((JTextComponent) c).isEditable();
                }
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
}