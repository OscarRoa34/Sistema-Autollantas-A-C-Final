package co.edu.uptc.view.dialogs;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import co.edu.uptc.view.GlobalView;
import co.edu.uptc.view.utils.PropertiesService;

public class WarningPopUp extends JWindow {

    private float opacity = 0f;

    public WarningPopUp(Frame parent, String title, String message) {
        super(parent);
        setBackground(new Color(0, 0, 0, 0));

        PropertiesService p = new PropertiesService();

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GlobalView.WARNING_POPUP_BACKGROUND);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }

            @Override
            public boolean isOpaque() {
                return false;
            }
        };

        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBorder(new EmptyBorder(10, 15, 10, 10));
        setContentPane(panel);

        ImageIcon icon = loadIcon(p.getProperties("warning-icon"), 48, 48);
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        panel.add(iconLabel);

        panel.add(Box.createHorizontalStrut(10));

        JLabel lblText = new JLabel(
            "<html><div style='color:white; font-family:Segoe UI; line-height:1.25; width:280px;'>" +
            "<b style='font-size:16px;'>" + title + "</b><br>" +
            "<span style='font-size:13px;'>" + message + "</span></div></html>"
        );
        lblText.setAlignmentY(Component.CENTER_ALIGNMENT);
        panel.add(lblText);

        panel.add(Box.createHorizontalGlue());

        JButton btnClose = new JButton("X");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnClose.setForeground(Color.WHITE);
        btnClose.setFocusPainted(false);
        btnClose.setBorderPainted(false);
        btnClose.setContentAreaFilled(false);
        btnClose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> fadeOutAndClose());
        btnClose.setAlignmentY(Component.CENTER_ALIGNMENT);
        panel.add(btnClose);

        pack();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = screenSize.width - getWidth() - 20;
        int y = 120;
        setLocation(x, y);

        fadeIn();

        Timer timer = new Timer(3000, e -> fadeOutAndClose());
        timer.setRepeats(false);
        timer.start();
    }

    private void fadeIn() {
        setOpacity(0f);
        setVisible(true);
        Timer fadeTimer = new Timer(30, e -> {
            opacity += 0.05f;
            if (opacity >= 1f) {
                opacity = 1f;
                ((Timer) e.getSource()).stop();
            }
            setOpacity(opacity);
        });
        fadeTimer.start();
    }

    private void fadeOutAndClose() {
        Timer fadeTimer = new Timer(30, e -> {
            opacity -= 0.05f;
            if (opacity <= 0f) {
                opacity = 0f;
                ((Timer) e.getSource()).stop();
                dispose();
            }
            setOpacity(opacity);
        });
        fadeTimer.start();
    }

    public static void showWarningPopup(Frame parent, String title, String message) {
        new WarningPopUp(parent, title, message);
    }

    private ImageIcon loadIcon(String path, int width, int height) {
        if (path == null || path.isEmpty()) return null;
        try {
            java.net.URL imgUrl = getClass().getResource(path);
            if (imgUrl == null) {
                System.err.println("Imagen no encontrada en el classpath: " + path);
                return null;
            }
            ImageIcon icon = new ImageIcon(imgUrl);
            Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
