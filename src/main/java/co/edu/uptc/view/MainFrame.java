package co.edu.uptc.view;

import javax.swing.*;
import co.edu.uptc.presenter.Presenter;
import co.edu.uptc.view.panels.*;
import co.edu.uptc.view.utils.PropertiesService;
import co.edu.uptc.view.utils.ViewController;
import java.awt.*;

public class MainFrame extends JFrame {

    private SidebarPanel sidebar;
    private JPanel mainContainer;
    private ViewController controller;
    private JPanel mainLayout;
    private Presenter presenter;
    private PropertiesService p;
    private CardLayout cardLayout;
    private JPanel mainCardPanel;
    private LogInPanel loginPanel;
    private LoadingPanel loadingPanel;

    public MainFrame() {
        p = new PropertiesService();
        presenter = new Presenter();

        initPanels();

        setTitle("Autollantas A&C");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setUndecorated(true);
        setExtendedState(MAXIMIZED_BOTH);

        Image icono = loadImage(p.getProperties("logo"));
        if (icono != null) {
            setIconImage(icono);
        }

        add(mainCardPanel, BorderLayout.CENTER);

        setVisible(true);
        startInitialLoad();
    }

    private void initPanels() {
        cardLayout = new CardLayout();
        mainCardPanel = new JPanel(cardLayout);

        loginPanel = new LogInPanel(this, presenter);
        mainCardPanel.add(loginPanel, "LOGIN");

        loadingPanel = new LoadingPanel();
        mainCardPanel.add(loadingPanel, "LOADING");

        mainContainer = new JPanel(new BorderLayout());
        controller = new ViewController(mainContainer, presenter);
        presenter.setViewCallback(controller);

        sidebar = new SidebarPanel(controller, presenter);
        HeaderPanel header = new HeaderPanel();

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(header, BorderLayout.NORTH);
        rightPanel.add(mainContainer, BorderLayout.CENTER);

        mainLayout = new JPanel(new BorderLayout());
        mainLayout.add(sidebar, BorderLayout.WEST);
        mainLayout.add(rightPanel, BorderLayout.CENTER);

        mainCardPanel.add(mainLayout, "MAIN_APP");
    }

    private void startInitialLoad() {

        showLoadingScreen("Cargando datos iniciales...");

        SwingWorker<Void, Void> dataLoader = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                presenter.loadInitialData();
                return null;
            }

            @Override
            protected void done() {
                showLoginScreen();
            }
        };

        dataLoader.execute();
    }

    public void showMainApplication() {
        SwingUtilities.invokeLater(() -> {
            controller.showPanel(new WelcomePanel());
            cardLayout.show(mainCardPanel, "MAIN_APP");
            revalidate();
            repaint();
        });
    }

    public void showLoginScreen() {
        cardLayout.show(mainCardPanel, "LOGIN");
    }

    public void showLoadingScreen(String message) {
        loadingPanel.setLoadingText(message);
        cardLayout.show(mainCardPanel, "LOADING");
    }

    private Image loadImage(String path) {
        if (path == null || path.trim().isEmpty())
            return null;
        try {
            java.net.URL imgUrl = getClass().getResource(path);
            if (imgUrl == null) {
                System.err.println("No se encontró la imagen en el classpath: " + path);
                return null;
            }
            return new ImageIcon(imgUrl).getImage();
        } catch (Exception e) {
            System.err.println("Excepción al cargar la imagen: " + path);
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainFrame::new);
    }
}
