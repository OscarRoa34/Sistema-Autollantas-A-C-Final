package co.edu.uptc.view.panels;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import java.io.File;
import java.io.IOException;

import co.edu.uptc.view.GlobalView;
import co.edu.uptc.view.utils.PropertiesService;
import co.edu.uptc.view.utils.RoundedButton;

public class HeaderPanel extends JPanel {

    private RoundedButton btnCerrar;
    private RoundedButton btnAyuda;
    private JLabel lblFechaHora;
    private PropertiesService p;
    
    private final SimpleDateFormat formato = new SimpleDateFormat("'Hoy es' EEEE d 'de' MMMM 'de' yyyy, h:mma",
            new Locale("es", "ES"));

    public HeaderPanel() {
        p = new PropertiesService();
        setLayout(new BorderLayout());
        setBackground(GlobalView.HEADER_BACKGROUND);
        setPreferredSize(new Dimension(0, 100));

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(23, 23, 23, 23));

        lblFechaHora = new JLabel(formatFecha());
        lblFechaHora.setForeground(GlobalView.HEADER_TEXT_COLOR);
        lblFechaHora.setFont(GlobalView.BODY_FONT.deriveFont(Font.BOLD, 20f));
        content.add(lblFechaHora, BorderLayout.WEST);

        Timer timer = new Timer(60000, e -> lblFechaHora.setText(formatFecha()));
        timer.setInitialDelay(0);
        timer.start();

        ImageIcon iconSalir = createIcon(p.getProperties("logout"), 32, 32);
        ImageIcon iconAyuda = createIcon(p.getProperties("information"), 32, 32);

        btnCerrar = new RoundedButton(
                "Cerrar",
                iconSalir,
                15,
                GlobalView.CLOSE_BUTTON_BACKGROUND,
                GlobalView.CLOSE_BUTTON_HOVER_COLOR,
                null);
        btnCerrar.setForeground(GlobalView.LIGHT_TEXT_COLOR);
        btnCerrar.setFont(GlobalView.BUTTON_FONT);
        btnCerrar.addActionListener(e -> {
            boolean confirm = co.edu.uptc.view.dialogs.ConfirmDialog.showConfirmDialog(
                    null,
                    "¿Está seguro de querer cerrar la aplicación?",
                    "Confirmar salida");

            if (confirm) {
                System.exit(0);
            }
        });

        btnAyuda = new RoundedButton(
                "¿Cómo usar?",
                iconAyuda,
                15,
                GlobalView.INFO_COLOR,
                GlobalView.INFO_COLOR.darker(),
                null);
        btnAyuda.setForeground(GlobalView.LIGHT_TEXT_COLOR);
        btnAyuda.setFont(GlobalView.BUTTON_FONT);

        btnAyuda.addActionListener(e -> {
            abrirManualUsuario();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        buttonPanel.setOpaque(false);

        buttonPanel.add(btnAyuda);
        buttonPanel.add(btnCerrar);

        content.add(buttonPanel, BorderLayout.EAST);
        add(content, BorderLayout.CENTER);
    }

    private void abrirManualUsuario() {
        try {

            String workingDir = System.getProperty("user.dir");

            String pdfPath = workingDir + File.separator + "data" + File.separator + "Manual de usuario.pdf";

            File pdfFile = new File(pdfPath);

            if (pdfFile.exists() && !pdfFile.isDirectory()) {

                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(pdfFile);
                } else {

                    System.err.println("Error: Desktop API no es compatible.");
                    JOptionPane.showMessageDialog(this,
                            "No se puede abrir el archivo automáticamente en este sistema.",
                            "Error de Compatibilidad",
                            JOptionPane.WARNING_MESSAGE);
                }
            } else {

                System.err.println("Error: No se pudo encontrar el manual en: " + pdfFile.getAbsolutePath());
                JOptionPane.showMessageDialog(this,
                        "El archivo del manual no se encuentra en la ruta esperada:\n" + pdfFile.getAbsolutePath(),
                        "Error de Archivo",
                        JOptionPane.ERROR_MESSAGE);
            }

        } catch (IOException ex) {

            System.err.println("Error de E/S al intentar abrir el manual.");
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Ocurrió un error de Archivo al intentar abrir el manual: " + ex.getMessage(),
                    "Error de E/S",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {

            System.err.println("Error inesperado al abrir el manual.");
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Ocurrió un error inesperado: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String formatFecha() {
        String texto = formato.format(new Date()).toLowerCase();
        if (texto.length() > 0) {
            return "H" + texto.substring(1);
        }
        return texto;
    }

    private ImageIcon createIcon(String path, int width, int height) {
        if (path == null || path.trim().isEmpty()) {
            System.err.println("Error: ruta de imagen vacía");
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
}