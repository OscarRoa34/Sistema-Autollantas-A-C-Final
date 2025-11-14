package co.edu.uptc.view.panels;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import co.edu.uptc.view.GlobalView;
import co.edu.uptc.view.utils.PropertiesService;

public class LoadingPanel extends JPanel {

    private PropertiesService p;
    private JLabel loadingLabel;
    private JLabel gifLabel;

    public LoadingPanel() {
        this.p = new PropertiesService();
        initComponents();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        setBackground(GlobalView.GENERAL_BACKGROUND);
        setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints mainGbc = new GridBagConstraints();
        mainGbc.anchor = GridBagConstraints.CENTER;
        mainGbc.fill = GridBagConstraints.NONE;
        mainGbc.weightx = 1.0;
        mainGbc.weighty = 1.0;

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(10, 10, 10, 10);

        String gifPath = p.getProperties("loading-gif");
        ImageIcon loadingIcon = null;

        if (gifPath != null && !gifPath.isEmpty()) {
            java.net.URL imgUrl = getClass().getResource(gifPath);
            if (imgUrl != null) {
                ImageIcon originalIcon = new ImageIcon(imgUrl);

                Image scaled = originalIcon.getImage().getScaledInstance(120, 120, Image.SCALE_DEFAULT);
                loadingIcon = new ImageIcon(scaled);
            } else {
                System.err.println("GIF no encontrado en el classpath: " + gifPath);
            }
        }

        gifLabel = new JLabel();
        if (loadingIcon != null) {
            gifLabel.setIcon(loadingIcon);
        } else {
            gifLabel.setText("Cargando...");
            gifLabel.setFont(GlobalView.TITLE_FONT.deriveFont(18f));
            gifLabel.setForeground(GlobalView.TEXT_COLOR);
        }

        gbc.insets = new Insets(0, 0, 15, 0);
        contentPanel.add(gifLabel, gbc);

        loadingLabel = new JLabel("Autenticando...");
        loadingLabel.setFont(GlobalView.TITLE_FONT.deriveFont(22f));
        loadingLabel.setForeground(GlobalView.TEXT_COLOR);
        gbc.insets = new Insets(10, 0, 10, 0);
        contentPanel.add(loadingLabel, gbc);

        add(contentPanel, mainGbc);
    }

    public void setLoadingText(String text) {
        if (loadingLabel != null) {
            loadingLabel.setText(text);
        }
    }

    public void showAuthenticating() {
        setLoadingText("Autenticando...");
    }

    public void showLoadingData() {
        setLoadingText("Cargando datos...");
    }
}
