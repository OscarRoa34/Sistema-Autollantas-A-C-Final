package co.edu.uptc.view.panels;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicScrollBarUI;
import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JTextFieldDateEditor;

import co.edu.uptc.presenter.Presenter;
import co.edu.uptc.models.products.Product;

import co.edu.uptc.view.GlobalView;

import co.edu.uptc.view.dialogs.ConfigureAlertsDialog;
import co.edu.uptc.view.dialogs.SuccessPopUp;
import co.edu.uptc.view.dialogs.ConfirmDialog;

import co.edu.uptc.view.utils.PropertiesService;
import co.edu.uptc.view.utils.ThresholdManager;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.Date;
import java.util.List;

public class ReportsPanel extends JPanel {

    private JList<Product> alertList;
    private DefaultListModel<Product> alertListModel;
    private JComboBox<String> reportTypeComboBox;
    private JPanel parametersPanel;
    private CardLayout cardLayout;
    private JDateChooser startDateChooser, endDateChooser;
    private JSpinner topNSpinner;
    private PropertiesService p;
    private ThresholdManager thresholdManager;
    private Presenter presenter;
    private int warningThreshold = 10;
    private int criticalThreshold = 3;

    public ReportsPanel(Presenter presenter) {
        this.p = new PropertiesService();
        this.presenter = presenter;

        this.thresholdManager = new ThresholdManager();
        this.warningThreshold = thresholdManager.getWarningThreshold();
        this.criticalThreshold = thresholdManager.getCriticalThreshold();

        presenter.setAlertThresholds(this.warningThreshold, this.criticalThreshold);

        initComponents();
        loadAlertsData();
    }

    private void initComponents() {

        setLayout(new BorderLayout(20, 0));
        setBackground(GlobalView.GENERAL_BACKGROUND);
        setBorder(new EmptyBorder(15, 20, 20, 20));
        JLabel title = new JLabel("Gestión de Reportes y Alertas", SwingConstants.CENTER);
        title.setFont(GlobalView.TITLE_FONT);
        title.setForeground(GlobalView.TEXT_COLOR);
        title.setBorder(new EmptyBorder(0, 0, 20, 0));
        add(title, BorderLayout.NORTH);
        JPanel mainContentPanel = new JPanel(new GridLayout(1, 2, 25, 0));
        mainContentPanel.setOpaque(false);
        mainContentPanel.add(createAlertsPanel());
        mainContentPanel.add(createReportsPanel());
        add(mainContentPanel, BorderLayout.CENTER);
    }

    private JPanel createAlertsPanel() {
        JPanel alertsContainer = new JPanel(new BorderLayout(0, 10));
        alertsContainer.setOpaque(false);
        alertsContainer.setBorder(new TitledBorder(
                new LineBorder(GlobalView.BORDER_COLOR, 1, true),
                " Alertas de Inventario ", TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, GlobalView.TABLE_HEADER_FONT,
                GlobalView.TEXT_COLOR));

        alertListModel = new DefaultListModel<>();
        alertList = new JList<>(alertListModel);

        alertList.setCellRenderer(new AlertListCellRenderer());
        alertList.setBackground(GlobalView.GENERAL_BACKGROUND_LIGHT);
        alertList.setSelectionBackground(GlobalView.TABLE_SELECTION_BACKGROUND);
        alertList.setFixedCellHeight(70);

        JScrollPane scrollPane = new JScrollPane(alertList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        verticalScrollBar.setUI(new CustomScrollBarUI());
        verticalScrollBar.setPreferredSize(new Dimension(10, 0));
        verticalScrollBar.setBackground(GlobalView.GENERAL_BACKGROUND_LIGHT);
        scrollPane.getViewport().setBackground(GlobalView.GENERAL_BACKGROUND_LIGHT);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        alertsContainer.add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JButton configureButton = new JButton("Configurar Alertas");
        configureButton.setFont(new Font(GlobalView.BUTTON_FONT.getFamily(), Font.BOLD, 15));
        configureButton.setIcon(createIcon(p.getProperties("settings"), 20, 20));
        configureButton.setIconTextGap(10);
        Color defaultBg = GlobalView.ASIDE_BACKGROUND;
        Color hoverBg = defaultBg.darker();
        configureButton.setBackground(defaultBg);
        configureButton.setForeground(Color.WHITE);
        configureButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        configureButton.setFocusPainted(false);
        configureButton.setBorderPainted(false);
        configureButton.setContentAreaFilled(true);
        configureButton.setOpaque(true);
        configureButton.setMargin(new Insets(10, 40, 10, 40));
        configureButton.setMaximumSize(new Dimension(260, 50));
        configureButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        configureButton.addMouseListener(new ButtonHoverEffect(configureButton, defaultBg, hoverBg));
        configureButton.addActionListener(e -> openConfigureAlertsDialog());
        bottomPanel.add(Box.createHorizontalGlue());
        bottomPanel.add(configureButton);
        bottomPanel.add(Box.createHorizontalGlue());
        alertsContainer.add(bottomPanel, BorderLayout.SOUTH);

        return alertsContainer;
    }

    private void openConfigureAlertsDialog() {
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        ConfigureAlertsDialog dialog = new ConfigureAlertsDialog(
                parentFrame, this.warningThreshold, this.criticalThreshold);
        dialog.setVisible(true);

        if (dialog.isSaved()) {
            this.warningThreshold = dialog.getWarningThreshold();
            this.criticalThreshold = dialog.getCriticalThreshold();

            thresholdManager.saveThresholds(this.warningThreshold, this.criticalThreshold);

            presenter.setAlertThresholds(this.warningThreshold, this.criticalThreshold);
            loadAlertsData();
            SuccessPopUp.showSuccessPopup(parentFrame, "Éxito:", "Límites de alerta actualizados.");
        }
    }

    private JPanel createReportsPanel() {

        JPanel reportsContainer = new JPanel();
        reportsContainer.setLayout(new BoxLayout(reportsContainer, BoxLayout.Y_AXIS));

        reportsContainer.setOpaque(false);
        reportsContainer.setBorder(new TitledBorder(
                new LineBorder(GlobalView.BORDER_COLOR, 1, true),
                " Generación de Reportes ", TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, GlobalView.TABLE_HEADER_FONT,
                GlobalView.TEXT_COLOR));

        reportsContainer.setBorder(new CompoundBorder(reportsContainer.getBorder(),
                new EmptyBorder(15, 15, 15, 15)));

        JPanel configPanel = new JPanel();
        configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));
        configPanel.setOpaque(false);

        JLabel reportTypeLabel = new JLabel("Seleccione el tipo de reporte:");
        reportTypeLabel.setFont(GlobalView.TABLE_BODY_FONT.deriveFont(Font.BOLD));
        reportTypeLabel.setForeground(GlobalView.TEXT_COLOR);

        reportTypeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String[] reportTypes = { "Reporte de Ventas", "Valoración de Inventario", "Productos Más Vendidos" };
        reportTypeComboBox = new JComboBox<>(reportTypes);
        reportTypeComboBox.setFont(GlobalView.TEXT_FIELD_FONT);
        reportTypeComboBox.setBackground(Color.WHITE);
        reportTypeComboBox.setForeground(GlobalView.TEXT_COLOR);
        reportTypeComboBox.setBorder(new LineBorder(GlobalView.BORDER_COLOR, 1));

        reportTypeComboBox.setAlignmentX(Component.CENTER_ALIGNMENT);

        Dimension comboBoxPreferredSize = reportTypeComboBox.getPreferredSize();
        reportTypeComboBox.setPreferredSize(new Dimension(350, comboBoxPreferredSize.height + 10));

        reportTypeComboBox.setMaximumSize(new Dimension(350, comboBoxPreferredSize.height + 10));

        configPanel.add(reportTypeLabel);
        configPanel.add(Box.createVerticalStrut(10));
        configPanel.add(reportTypeComboBox);
        configPanel.add(Box.createVerticalStrut(20));

        parametersPanel = createReportParametersPanel();

        parametersPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        configPanel.add(parametersPanel);

        reportTypeComboBox.addActionListener(e -> updateParametersPanel());
        updateParametersPanel();

        reportsContainer.add(configPanel);

        reportsContainer.add(Box.createVerticalGlue());

        JButton generateButton = new JButton("Generar Reporte");
        generateButton.setFont(new Font(GlobalView.BUTTON_FONT.getFamily(), Font.BOLD, 16));
        generateButton.setIcon(createIcon(p.getProperties("report"), 22, 22));
        generateButton.setIconTextGap(15);
        generateButton.setBackground(GlobalView.CONFIRM_BUTTON_BACKGROUND);
        generateButton.setForeground(Color.WHITE);
        generateButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        generateButton.setFocusPainted(false);
        generateButton.setBorderPainted(false);

        generateButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        generateButton.setMinimumSize(new Dimension(250, 50));
        generateButton.setPreferredSize(new Dimension(300, 50));
        generateButton.setMaximumSize(new Dimension(350, 50));

        generateButton.addActionListener(e -> generateReport());
        generateButton.addMouseListener(new ButtonHoverEffect(generateButton,
                GlobalView.CONFIRM_BUTTON_BACKGROUND,
                GlobalView.CONFIRM_BUTTON_BACKGROUND.darker()));

        reportsContainer.add(Box.createVerticalStrut(10));
        reportsContainer.add(generateButton);

        return reportsContainer;
    }

    private JPanel createReportParametersPanel() {
        JPanel panel = new JPanel(new CardLayout());
        panel.setOpaque(false);
        this.cardLayout = (CardLayout) panel.getLayout();

        Font inputFont = GlobalView.TEXT_FIELD_FONT;
        Color inputBg = Color.WHITE;
        Color inputFg = GlobalView.TEXT_COLOR;
        Border inputBorder = new LineBorder(GlobalView.BORDER_COLOR, 1);
        Border inputInnerPadding = new EmptyBorder(5, 5, 5, 5);

        JPanel dateRangePanel = new JPanel(new GridBagLayout());
        dateRangePanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.insets = new Insets(8, 5, 8, 5);

        startDateChooser = new JDateChooser();
        startDateChooser.setDate(new Date());
        startDateChooser.setFont(inputFont);
        startDateChooser.setBackground(inputBg);
        startDateChooser.setForeground(inputFg);
        startDateChooser.setBorder(inputBorder);
        JTextFieldDateEditor startEditor = (JTextFieldDateEditor) startDateChooser.getDateEditor();
        startEditor.setBackground(inputBg);
        startEditor.setForeground(inputFg);
        startEditor.setBorder(inputInnerPadding);
        startEditor.setEditable(false);
        Dimension dateChooserPreferredSize = startDateChooser.getPreferredSize();

        startDateChooser
                .setPreferredSize(new Dimension(dateChooserPreferredSize.width, dateChooserPreferredSize.height + 10));
        startDateChooser.setMaximumSize(new Dimension(Integer.MAX_VALUE, dateChooserPreferredSize.height + 10));

        endDateChooser = new JDateChooser();
        endDateChooser.setDate(new Date());
        endDateChooser.setFont(inputFont);
        endDateChooser.setBackground(inputBg);
        endDateChooser.setForeground(inputFg);
        endDateChooser.setBorder(inputBorder);
        JTextFieldDateEditor endEditor = (JTextFieldDateEditor) endDateChooser.getDateEditor();
        endEditor.setBackground(inputBg);
        endEditor.setForeground(inputFg);
        endEditor.setBorder(inputInnerPadding);
        endEditor.setEditable(false);

        endDateChooser
                .setPreferredSize(new Dimension(dateChooserPreferredSize.width, dateChooserPreferredSize.height + 10));
        endDateChooser.setMaximumSize(new Dimension(Integer.MAX_VALUE, dateChooserPreferredSize.height + 10));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        dateRangePanel.add(new JLabel("Fecha de Inicio:") {
            {
                setFont(GlobalView.TABLE_BODY_FONT);
                setForeground(GlobalView.TEXT_COLOR);
            }
        }, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        dateRangePanel.add(startDateChooser, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        dateRangePanel.add(new JLabel("Fecha de Fin:") {
            {
                setFont(GlobalView.TABLE_BODY_FONT);
                setForeground(GlobalView.TEXT_COLOR);
            }
        }, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        dateRangePanel.add(endDateChooser, gbc);

        JPanel topNPanel = new JPanel(new GridBagLayout());
        topNPanel.setOpaque(false);
        GridBagConstraints gbcTopN = new GridBagConstraints();

        gbcTopN.insets = new Insets(8, 5, 8, 5);

        topNSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));
        topNSpinner.setFont(inputFont);
        topNSpinner.setBorder(inputBorder);
        JSpinner.DefaultEditor spinnerEditor = (JSpinner.DefaultEditor) topNSpinner.getEditor();
        spinnerEditor.getTextField().setBackground(inputBg);
        spinnerEditor.getTextField().setForeground(inputFg);
        spinnerEditor.getTextField().setBorder(inputInnerPadding);
        spinnerEditor.getTextField().setEditable(true);
        Dimension spinnerPreferredSize = topNSpinner.getPreferredSize();

        topNSpinner.setPreferredSize(new Dimension(spinnerPreferredSize.width + 20, spinnerPreferredSize.height + 10));
        topNSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, spinnerPreferredSize.height + 10));

        gbcTopN.gridx = 0;
        gbcTopN.gridy = 0;
        gbcTopN.anchor = GridBagConstraints.WEST;
        gbcTopN.weightx = 0;
        topNPanel.add(new JLabel("Mostrar Top:") {
            {
                setFont(GlobalView.TABLE_BODY_FONT);
                setForeground(GlobalView.TEXT_COLOR);
            }
        }, gbcTopN);
        gbcTopN.gridx = 1;
        gbcTopN.weightx = 1;
        gbcTopN.fill = GridBagConstraints.HORIZONTAL;
        topNPanel.add(topNSpinner, gbcTopN);

        JSeparator separator = new JSeparator();
        separator.setForeground(GlobalView.BORDER_COLOR.brighter());
        separator.setBackground(GlobalView.GENERAL_BACKGROUND.darker());
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.insets = new Insets(15, 0, 15, 0);
        dateRangePanel.add(separator, gbc);

        JPanel emptyPanel = new JPanel();
        emptyPanel.setOpaque(false);

        panel.add(dateRangePanel, "DATES");
        panel.add(emptyPanel, "EMPTY");
        panel.add(topNPanel, "SPINNER");
        return panel;
    }

    private void updateParametersPanel() {
        int selectedIndex = reportTypeComboBox.getSelectedIndex();
        switch (selectedIndex) {
            case 0:
                cardLayout.show(parametersPanel, "DATES");
                break;
            case 1:
                cardLayout.show(parametersPanel, "EMPTY");
                break;
            case 2:
                cardLayout.show(parametersPanel, "SPINNER");
                break;
        }
    }

    private void loadAlertsData() {
        alertListModel.clear();

        List<Product> lowStockProducts = presenter.requestLowStockAlerts();

        if (lowStockProducts != null) {
            for (Product product : lowStockProducts) {
                alertListModel.addElement(product);
            }
        }
    }

    /**
     * MÉTODO MODIFICADO:
     * Ahora usa ConfirmDialog.showErrorDialog en lugar de JOptionPane.
     */
    private void generateReport() {
        int selectedIndex = reportTypeComboBox.getSelectedIndex();
        JFileChooser fileChooser = new JFileChooser();

        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            SwingUtilities.updateComponentTreeUI(fileChooser);
        } catch (Exception ignored) {
        }

        String suggestedFileName = "Reporte.pdf";
        switch (selectedIndex) {
            case 0:
                Date startDate = startDateChooser.getDate();
                Date endDate = endDateChooser.getDate();

                if (startDate == null || endDate == null) {

                    ConfirmDialog.showErrorDialog(SwingUtilities.getWindowAncestor(this),
                            "Debe seleccionar una fecha de inicio y fin.", "Error de Fechas");
                    return;
                }

                if (endDate.before(startDate)) {

                    ConfirmDialog.showErrorDialog(SwingUtilities.getWindowAncestor(this),
                            "La fecha de fin no puede ser anterior a la fecha de inicio.", "Error de Fechas");
                    return;
                }

                suggestedFileName = String.format("Reporte_Ventas_%tF_a_%tF.pdf",
                        startDate, endDate);
                break;
            case 1:
                suggestedFileName = "Reporte_Valoracion_Inventario.pdf";
                break;
            case 2:
                suggestedFileName = String.format("Reporte_Top_%d_Vendidos.pdf", topNSpinner.getValue());
                break;
        }

        fileChooser.setDialogTitle("Guardar Reporte como...");
        fileChooser.setSelectedFile(new File(suggestedFileName));
        FileNameExtensionFilter pdfFilter = new FileNameExtensionFilter("Documento PDF (*.pdf)", "pdf");
        fileChooser.addChoosableFileFilter(pdfFilter);
        fileChooser.setFileFilter(pdfFilter);

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION)
            return;

        File fileToSave = fileChooser.getSelectedFile();
        if (!fileToSave.getName().toLowerCase().endsWith(".pdf")) {
            fileToSave = new File(fileToSave.getAbsolutePath() + ".pdf");
        }

        try {
            switch (selectedIndex) {
                case 0:
                    presenter.generateSalesReportToFile(startDateChooser.getDate(), endDateChooser.getDate(),
                            fileToSave.getAbsolutePath());
                    break;
                case 1:
                    presenter.generateInventoryValuationReportToFile(fileToSave.getAbsolutePath());
                    break;
                case 2:
                    presenter.generateTopSellingProductsReportToFile((int) topNSpinner.getValue(),
                            fileToSave.getAbsolutePath());
                    break;
            }

            Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
            SuccessPopUp.showSuccessPopup(parentFrame, "Reporte Generado",
                    "El reporte se ha guardado exitosamente en PDF.");

        } catch (Exception ex) {
            ex.printStackTrace();

            ConfirmDialog.showErrorDialog(SwingUtilities.getWindowAncestor(this),
                    "Error al generar el PDF: " + ex.getMessage(), "Error");
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

    private class AlertListCellRenderer extends JPanel implements ListCellRenderer<Product> {

        private JPanel iconPanel;
        private JLabel iconLabel;
        private JTextArea messageArea;
        private Color lowStockColor = new Color(255, 193, 7);
        private Color criticalStockColor = new Color(220, 53, 69);

        public AlertListCellRenderer() {

            setLayout(new BorderLayout(0, 0));
            setOpaque(true);
            iconPanel = new JPanel(new GridBagLayout());
            iconPanel.setOpaque(true);
            iconPanel.setPreferredSize(new Dimension(60, 0));
            iconPanel.setBorder(new EmptyBorder(0, 10, 0, 10));
            iconLabel = new JLabel();
            iconPanel.add(iconLabel);
            messageArea = new JTextArea();
            messageArea.setWrapStyleWord(true);
            messageArea.setLineWrap(true);
            messageArea.setOpaque(false);
            messageArea.setEditable(false);
            messageArea.setFont(GlobalView.TABLE_BODY_FONT.deriveFont(14f));
            messageArea.setBorder(new EmptyBorder(10, 15, 10, 10));
            add(iconPanel, BorderLayout.WEST);
            add(messageArea, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Product> list, Product value, int index,
                boolean isSelected, boolean cellHasFocus) {

            int stock = value.getstock();
            String name = value.getName() + " (" + value.getBrand() + ")";

            String message;
            if (stock <= 0) {
                message = String.format("%s AGOTADO (%d unidades).", name, stock);
            } else {
                message = String.format("%s solo tiene %d unidades restantes.", name, stock);
            }
            messageArea.setText(message);

            Color alertColor;
            ImageIcon icon;

            if (stock <= ReportsPanel.this.criticalThreshold) {
                icon = createIcon(p.getProperties("error-icon"), 32, 32);
                alertColor = criticalStockColor;
            } else {
                icon = createIcon(p.getProperties("warning-icon"), 32, 32);
                alertColor = lowStockColor;
            }

            if (icon == null) {
                iconLabel.setIcon(null);
                iconLabel.setText("!");
                iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
                iconLabel.setForeground(Color.WHITE);
            } else {
                iconLabel.setIcon(icon);
                iconLabel.setText(null);
            }

            iconPanel.setBackground(alertColor);

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                messageArea.setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
                messageArea.setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                messageArea.setBackground(list.getBackground());
                setForeground(list.getForeground());
                messageArea.setForeground(list.getForeground());
            }
            return this;
        }
    }

    class ButtonHoverEffect extends java.awt.event.MouseAdapter {

        private final JButton button;
        private final Color defaultBackground;
        private final Color hoverBackground;

        public ButtonHoverEffect(JButton button, Color defaultBackground, Color hoverBackground) {
            this.button = button;
            this.defaultBackground = defaultBackground;
            this.hoverBackground = hoverBackground;
        }

        @Override
        public void mouseEntered(java.awt.event.MouseEvent e) {
            button.setBackground(hoverBackground);
        }

        @Override
        public void mouseExited(java.awt.event.MouseEvent e) {
            button.setBackground(defaultBackground);
        }
    }

    private static class CustomScrollBarUI extends BasicScrollBarUI {

        private final Color THUMB_COLOR = GlobalView.ASIDE_BACKGROUND;
        private final int ARC_RADIUS = 8;
        private final Dimension ZERO_DIMENSION = new Dimension(0, 0);

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(ZERO_DIMENSION);
            button.setMinimumSize(ZERO_DIMENSION);
            button.setMaximumSize(ZERO_DIMENSION);
            return button;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            g.setColor(GlobalView.GENERAL_BACKGROUND_LIGHT);
            g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(THUMB_COLOR);
            int x = thumbBounds.x + 2;
            int y = thumbBounds.y + 2;
            int width = thumbBounds.width - 4;
            int height = thumbBounds.height - 4;
            g2.fill(new RoundRectangle2D.Float(x, y, width, height, ARC_RADIUS, ARC_RADIUS));
            g2.dispose();
        }
    }

    public void refreshData() {
        loadAlertsData();
    }
}