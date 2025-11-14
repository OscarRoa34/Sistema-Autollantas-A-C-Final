package co.edu.uptc.view.panels;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

import co.edu.uptc.presenter.Presenter;
import co.edu.uptc.view.dialogs.ConfirmDialog;
import co.edu.uptc.view.GlobalView;
import co.edu.uptc.view.MainFrame;
import co.edu.uptc.view.utils.PropertiesService;

public class LogInPanel extends JPanel {

    private PropertiesService p;
    private JPasswordField securityCodeField;
    private JButton loginButton;
    private JButton closeButton;
    private final String codePlaceholder = "Código de Seguridad";
    private MainFrame mainFrame;
    private Presenter presenter;

    public LogInPanel(MainFrame mainFrame, Presenter presenter) {
        this.mainFrame = mainFrame;
        this.presenter = presenter;
        this.p = new PropertiesService();
        initComponents();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        setBackground(GlobalView.GENERAL_BACKGROUND);
        GridBagConstraints mainGbc = new GridBagConstraints();
        mainGbc.gridx = 0;
        mainGbc.gridy = 0;
        mainGbc.gridheight = GridBagConstraints.REMAINDER;
        mainGbc.weightx = 1.8;
        mainGbc.weighty = 1.0;
        mainGbc.fill = GridBagConstraints.BOTH;
        JPanel imagePanel = new BlurredBackgroundPanel(p.getProperties("login"));
        add(imagePanel, mainGbc);
        closeButton = createCloseButton();
        mainGbc.gridx = 1;
        mainGbc.gridy = 0;
        mainGbc.gridheight = 1;
        mainGbc.weightx = 0.4;
        mainGbc.weighty = 0.0;
        mainGbc.fill = GridBagConstraints.NONE;
        mainGbc.anchor = GridBagConstraints.NORTHEAST;
        mainGbc.insets = new Insets(10, 10, 0, 10);
        add(closeButton, mainGbc);
        JPanel cardPanel = createCardPanel();
        mainGbc.gridx = 1;
        mainGbc.gridy = 1;
        mainGbc.weighty = 1.0;
        mainGbc.anchor = GridBagConstraints.CENTER;
        mainGbc.fill = GridBagConstraints.NONE;
        mainGbc.insets = new Insets(0, 20, 0, 40);
        add(cardPanel, mainGbc);
    }

    private JPanel createCardPanel() {
        JPanel cardPanel = new JPanel(new GridBagLayout());
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Color.LIGHT_GRAY, 1, true),
                new EmptyBorder(40, 50, 40, 50)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0;
        gbc.insets = new Insets(10, 0, 10, 0);

        ImageIcon logoIcon = createIcon(p.getProperties("logo"), 150, 150);
        if (logoIcon != null) {
            JLabel logoLabel = new JLabel(logoIcon);
            gbc.insets = new Insets(0, 0, 20, 0);
            cardPanel.add(logoLabel, gbc);
            gbc.insets = new Insets(10, 0, 10, 0);
        }
        JLabel titleLabel = new JLabel("Acceso Restringido", SwingConstants.CENTER);
        titleLabel.setFont(GlobalView.TITLE_FONT.deriveFont(Font.BOLD, 28f));
        titleLabel.setForeground(GlobalView.TEXT_COLOR);
        cardPanel.add(titleLabel, gbc);

        securityCodeField = createStyledPasswordField(codePlaceholder);
        securityCodeField.addActionListener(e -> handleLogin());
        cardPanel.add(securityCodeField, gbc);

        loginButton = new JButton("Ingresar");
        styleLoginButton(loginButton);
        gbc.insets = new Insets(20, 0, 0, 0);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        cardPanel.add(loginButton, gbc);

        loginButton.addActionListener(e -> handleLogin());

        return cardPanel;
    }

    private void handleLogin() {
        String securityCode = new String(securityCodeField.getPassword());
        if (securityCode.isEmpty() || securityCode.equals(codePlaceholder)) {
            ConfirmDialog.showErrorDialog(SwingUtilities.getWindowAncestor(this),
                    "Por favor ingrese el código de seguridad.", "Campo Vacío");
            return;
        }
        mainFrame.showLoadingScreen("Autenticando...");
        SwingWorker<Boolean, Void> authWorker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                long start = System.currentTimeMillis();
                boolean authenticated = presenter.authenticateUser(securityCode);
                long elapsed = System.currentTimeMillis() - start;
                long minTime = 500;
                if (elapsed < minTime) {
                    Thread.sleep(minTime - elapsed);
                }
                return authenticated;
            }

            @Override
            protected void done() {
                try {
                    boolean isAuthenticated = get();
                    if (isAuthenticated) {
                        mainFrame.showMainApplication();
                    } else {
                        mainFrame.showLoginScreen();
                        ConfirmDialog.showErrorDialog(SwingUtilities.getWindowAncestor(LogInPanel.this),
                                "Código de seguridad incorrecto.", "Error de Autenticación");

                        securityCodeField.setText("");
                        securityCodeField.setEchoChar((char) 0);
                        securityCodeField.setForeground(GlobalView.PLACEHOLDER_COLOR);
                        securityCodeField.setText(codePlaceholder);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    mainFrame.showLoginScreen();
                    ConfirmDialog.showErrorDialog(SwingUtilities.getWindowAncestor(LogInPanel.this),
                            "Ocurrió un error inesperado.", "Error");
                }
            }
        };
        authWorker.execute();
    }

    private JPasswordField createStyledPasswordField(String placeholder) {
        JPasswordField passField = new JPasswordField();
        passField.setFont(GlobalView.TEXT_FIELD_FONT);
        passField.setForeground(GlobalView.PLACEHOLDER_COLOR);
        passField.setEchoChar((char) 0);
        passField.setText(placeholder);
        passField.setPreferredSize(new Dimension(280, 45));
        passField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(GlobalView.BORDER_COLOR, 1, true),
                new EmptyBorder(5, 15, 5, 15)));
        passField.setHorizontalAlignment(JPasswordField.CENTER);
        passField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (String.valueOf(passField.getPassword()).equals(placeholder)) {
                    passField.setText("");
                    passField.setEchoChar('*');
                    passField.setForeground(GlobalView.TEXT_COLOR);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (String.valueOf(passField.getPassword()).isEmpty()) {
                    passField.setEchoChar((char) 0);
                    passField.setForeground(GlobalView.PLACEHOLDER_COLOR);
                    passField.setText(placeholder);
                }
            }
        });
        return passField;
    }

    private void styleLoginButton(JButton button) {
        button.setFont(GlobalView.BUTTON_FONT.deriveFont(Font.BOLD, 18f));
        button.setBackground(GlobalView.CONFIRM_BUTTON_BACKGROUND);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(12, 50, 12, 50));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new ButtonHoverEffect(button,
                GlobalView.CONFIRM_BUTTON_BACKGROUND,
                GlobalView.CONFIRM_BUTTON_BACKGROUND.darker()));
    }

    private JButton createCloseButton() {
        JButton btn = new JButton("X");
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        btn.setForeground(Color.GRAY);
        btn.setBackground(GlobalView.GENERAL_BACKGROUND);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(5, 10, 5, 10));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(Color.RED);
                btn.setBackground(GlobalView.GENERAL_BACKGROUND.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setForeground(Color.GRAY);
                btn.setBackground(GlobalView.GENERAL_BACKGROUND);
            }
        });
        btn.addActionListener(e -> System.exit(0));
        return btn;
    }

    private ImageIcon createIcon(String path, int width, int height) {
        if (path == null || path.isEmpty()) {
            System.err.println("Error: Icon path is null or empty.");
            return null;
        }
        try {
            java.net.URL imgUrl = getClass().getResource(path);
            if (imgUrl == null) {
                System.err.println("Error: Imagen no encontrada en el classpath: " + path);
                return null;
            }
            ImageIcon icon = new ImageIcon(imgUrl);
            Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        } catch (Exception e) {
            System.err.println("Exception creando icon desde path: " + path);
            e.printStackTrace();
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
            this.button.setBackground(defaultBackground);
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

    private class BlurredBackgroundPanel extends JPanel {

        private final Image backgroundImage;
        private BufferedImage cachedBlurredImage;

        public BlurredBackgroundPanel(String imagePath) {
            this.backgroundImage = loadImage(imagePath);

            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    invalidateBlurredImageCache();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawBlurredBackground(g2);

            g2.dispose();
        }

        private void drawBlurredBackground(Graphics2D g2) {
            if (cachedBlurredImage == null ||
                    cachedBlurredImage.getWidth() != getWidth() ||
                    cachedBlurredImage.getHeight() != getHeight()) {
                cachedBlurredImage = generateBlurredImage(getWidth(), getHeight());
            }
            if (cachedBlurredImage != null) {
                g2.drawImage(cachedBlurredImage, 0, 0, this);
            }
        }

        private void invalidateBlurredImageCache() {
            cachedBlurredImage = null;
            repaint();
        }

        private BufferedImage generateBlurredImage(int width, int height) {
            if (width <= 0 || height <= 0 || backgroundImage == null) {
                return null;
            }
            BufferedImage sourceImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = sourceImage.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(backgroundImage, 0, 0, width, height, this);
            g2.dispose();
            float[] matrix = new float[9];
            for (int i = 0; i < 9; i++) {
                matrix[i] = 1.0f / 9.0f;
            }
            Kernel kernel = new Kernel(3, 3, matrix);
            ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
            BufferedImage blurred = sourceImage;
            int iterations = 7;
            for (int i = 0; i < iterations; i++) {
                BufferedImage temp = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                op.filter(blurred, temp);
                blurred = temp;
            }
            return blurred;
        }

        private Image loadImage(String path) {
            if (path == null || path.trim().isEmpty()) {
                System.err.println("Error: ruta de imagen vacía");
                return null;
            }
            try {
                java.net.URL imgUrl = getClass().getResource(path);
                if (imgUrl == null) {
                    System.err.println("Error: No se encontró la imagen en el classpath: " + path);
                    return null;
                }
                return new ImageIcon(imgUrl).getImage();
            } catch (Exception e) {
                System.err.println("Excepción al cargar la imagen: " + path);
                e.printStackTrace();
                return null;
            }
        }
    }
}