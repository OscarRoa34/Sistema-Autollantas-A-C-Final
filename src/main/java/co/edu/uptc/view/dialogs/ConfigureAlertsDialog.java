package co.edu.uptc.view.dialogs;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import co.edu.uptc.view.GlobalView;

public class ConfigureAlertsDialog extends JDialog {

    private boolean isSaved = false;
    private JSpinner warningSpinner;
    private JSpinner criticalSpinner;

    public ConfigureAlertsDialog(Frame parent, int currentWarning, int currentCritical) {
        super(parent, "Configurar Límites de Alerta", true);
        setUndecorated(true);
        setResizable(false);
        setBackground(new Color(0, 0, 0, 0));
        initComponents(currentWarning, currentCritical);
        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents(int currentWarning, int currentCritical) {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int arc = 25;
                int width = getWidth();
                int height = getHeight();
                g2.setColor(GlobalView.ASIDE_BACKGROUND);
                g2.fillRoundRect(0, 0, width, height, arc, arc);
                g2.setStroke(new BasicStroke(6f));
                g2.setColor(Color.WHITE);
                g2.drawRoundRect(1, 1, width - 2, height - 2, arc, arc);
                g2.dispose();
            }

            @Override
            public boolean isOpaque() {
                return false;
            }
        };
        mainPanel.setBorder(new EmptyBorder(20, 25, 20, 25));
        mainPanel.setOpaque(false);
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(createLabel("Advertencia (Amarillo) si stock es <= a:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        warningSpinner = createStyledSpinner(currentWarning);
        formPanel.add(warningSpinner, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(createLabel("Crítico (Rojo) si stock es <= a:"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        criticalSpinner = createStyledSpinner(currentCritical);
        formPanel.add(criticalSpinner, gbc);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 10));
        buttonPanel.setOpaque(false);
        JButton btnCancelar = createStyledButton("Cancelar", GlobalView.CANCEL_BUTTON_BACKGROUND);
        JButton btnGuardar = createStyledButton("Guardar", GlobalView.CONFIRM_BUTTON_BACKGROUND);
        btnCancelar.addActionListener(e -> dispose());
        btnGuardar.addActionListener(e -> onSave());
        buttonPanel.add(btnCancelar);
        buttonPanel.add(btnGuardar);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(mainPanel);
    }

    private void onSave() {

        String warningText = ((JSpinner.DefaultEditor) warningSpinner.getEditor()).getTextField().getText().trim();
        String criticalText = ((JSpinner.DefaultEditor) criticalSpinner.getEditor()).getTextField().getText().trim();

        int warningVal;
        int criticalVal;

        try {
            warningVal = Integer.parseInt(warningText);
            criticalVal = Integer.parseInt(criticalText);
        } catch (NumberFormatException e) {

            ConfirmDialog.showErrorDialog(this,
                    "Por favor ingrese solo números válidos.",
                    "Error de Formato");
            return;
        }

        SpinnerNumberModel model = (SpinnerNumberModel) warningSpinner.getModel();
        int min = (Integer) model.getMinimum();
        int max = (Integer) model.getMaximum();

        if (warningVal < min || warningVal > max || criticalVal < min || criticalVal > max) {

            String msg = String.format("Los valores deben estar entre %d y %d.", min, max);
            ConfirmDialog.showErrorDialog(this, msg, "Valor Fuera de Rango");
            return;
        }

        if (criticalVal >= warningVal) {
            ConfirmDialog.showErrorDialog(this,
                    "El límite 'Crítico' (rojo) debe ser menor que el límite de 'Advertencia' (amarillo).",
                    "Error de Validación");
            return;
        }

        warningSpinner.setValue(warningVal);
        criticalSpinner.setValue(criticalVal);

        this.isSaved = true;
        dispose();
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(GlobalView.TABLE_BODY_FONT.deriveFont(Font.BOLD, 16f));
        label.setForeground(Color.WHITE);
        return label;
    }

    private JSpinner createStyledSpinner(int initialValue) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(initialValue, 0, 999, 1));
        spinner.setFont(GlobalView.TEXT_FIELD_FONT.deriveFont(16f));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JFormattedTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.setForeground(GlobalView.TEXT_COLOR);
            textField.setBackground(GlobalView.GENERAL_BACKGROUND_LIGHT);
            textField.setBorder(new CompoundBorder(
                    new LineBorder(GlobalView.BORDER_COLOR, 1, true),
                    new EmptyBorder(5, 8, 5, 8)));
            textField.setFocusLostBehavior(JFormattedTextField.PERSIST);
        }
        return spinner;
    }

    private JButton createStyledButton(String text, Color baseColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 18));
        button.setForeground(Color.WHITE);
        button.setBackground(baseColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(8, 25, 8, 25));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(baseColor.darker());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(baseColor);
            }
        });
        return button;
    }

    public boolean isSaved() {
        return isSaved;
    }

    public int getWarningThreshold() {
        return (Integer) warningSpinner.getValue();
    }

    public int getCriticalThreshold() {
        return (Integer) criticalSpinner.getValue();
    }
}