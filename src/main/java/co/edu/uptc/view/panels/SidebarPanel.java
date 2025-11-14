package co.edu.uptc.view.panels;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import co.edu.uptc.view.utils.PropertiesService;
import co.edu.uptc.view.utils.ViewController;
import co.edu.uptc.presenter.Presenter;
import co.edu.uptc.view.GlobalView;
import co.edu.uptc.view.components.SidebarButton;

public class SidebarPanel extends JPanel {

    private final PropertiesService p;
    private SidebarButton activeButton;
    private final List<SidebarButton> buttons = new ArrayList<>();
    private final ViewController controller;
    private Presenter presenter;

    public SidebarPanel(ViewController controller, Presenter presenter) {
        this.controller = controller;
        this.presenter = presenter;
        this.p = new PropertiesService();

        setLayout(new BorderLayout());
        setBackground(GlobalView.ASIDE_BACKGROUND);
        setPreferredSize(new Dimension(250, 0));

        add(createLogoPanel(), BorderLayout.NORTH);
        add(createButtonsPanel(), BorderLayout.CENTER);
    }

    private JPanel createLogoPanel() {
        JPanel logoPanel = new JPanel(new BorderLayout());
        logoPanel.setBackground(GlobalView.ASIDE_BACKGROUND);
        logoPanel.setPreferredSize(new Dimension(250, 180));

        JLabel logoLabel = new JLabel();
        ImageIcon logoIcon = createIcon(p.getProperties("logo"), 200, 200);
        logoLabel.setIcon(logoIcon);
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        logoLabel.setVerticalAlignment(SwingConstants.TOP);

        logoPanel.add(logoLabel, BorderLayout.CENTER);

        logoLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {

                controller.showCachedPanel("WELCOME");

                setActiveButton(null);
            }
        });

        return logoPanel;
    }

    private JPanel createButtonsPanel() {
        JPanel buttonsPanel = new JPanel(new GridLayout(5, 1, 0, 25));
        buttonsPanel.setBackground(GlobalView.ASIDE_BACKGROUND);
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        SidebarButton btnProductos = createSidebarButton(
                "Gestión de Productos",
                createIcon(p.getProperties("products"), 32, 32),
                () -> controller.showCachedPanel("PRODUCTS"));

        SidebarButton btnCompras = createSidebarButton(
                "Gestión de registros de Compra",
                createIcon(p.getProperties("purchase"), 32, 32),
                () -> controller.showCachedPanel("PURCHASES"));

        SidebarButton btnVentas = createSidebarButton(
                "Gestión de registros de Venta",
                createIcon(p.getProperties("sales"), 32, 32),
                () -> controller.showCachedPanel("SALES"));

        SidebarButton btnReportes = createSidebarButton(
                "Gestión de Reportes y Alertas",
                createIcon(p.getProperties("reports"), 32, 32),
                () -> controller.showCachedPanel("REPORTS"));

        buttonsPanel.add(btnProductos);
        buttonsPanel.add(btnCompras);
        buttonsPanel.add(btnVentas);
        buttonsPanel.add(btnReportes);

        buttons.add(btnProductos);
        buttons.add(btnCompras);
        buttons.add(btnVentas);
        buttons.add(btnReportes);

        return buttonsPanel;
    }

    private SidebarButton createSidebarButton(String text, ImageIcon icon, Runnable onClick) {
        SidebarButton button = new SidebarButton(text, icon);
        button.addActionListener(e -> {
            setActiveButton(button);
            onClick.run();
        });
        return button;
    }

    public void setActiveButton(SidebarButton button) {
        if (activeButton != null) {
            activeButton.setActive(false);
        }
        if (button != null) {
            button.setActive(true);
        }
        activeButton = button;
    }

    public SidebarButton getActiveButton() {
        return activeButton;
    }

    public void setActiveButtonByIndex(int index) {
        if (index >= 0 && index < buttons.size()) {
            setActiveButton(buttons.get(index));
        }
    }

    private ImageIcon createIcon(String path, int width, int height) {
        if (path == null || path.trim().isEmpty()) {
            System.err.println("Error: ruta de imagen vacía");
            return null;
        }
        try {
            java.net.URL imgUrl = getClass().getResource(path);
            if (imgUrl == null) {

                imgUrl = new java.io.File(path).toURI().toURL();
            }
            if (imgUrl == null) {
                System.err.println("Error: No se pudo encontrar la imagen: " + path);
                return null;
            }
            ImageIcon icon = new ImageIcon(imgUrl);
            Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        } catch (Exception e) {
            System.err.println("Excepción al crear icono: " + path);
            e.printStackTrace();
            return null;
        }
    }
}