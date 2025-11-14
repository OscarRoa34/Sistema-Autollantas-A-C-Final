package co.edu.uptc.view.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesService {

    private final Properties properties = new Properties();

    public PropertiesService() {
        load();
    }

    private void load() {
        try (InputStream is = getClass().getResourceAsStream("/config.properties")) {
            if (is == null) {
                System.err.println("No se encontró config.properties en el classpath.");
                return;
            }
            properties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getProperties(String keyName) {
        return properties.getProperty(keyName);
    }
}
