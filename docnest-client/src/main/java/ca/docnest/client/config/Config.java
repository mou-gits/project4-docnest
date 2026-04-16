package ca.docnest.client.config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class Config {

    public static String HOST = "localhost";
    public static int PORT = 9090;

    static {
        Properties props = new Properties();
        InputStream input = null;

        try {
            // 1. Try working directory override
            try {
                String path = System.getProperty("user.dir") + "/client.env";
                input = new FileInputStream(path);
                System.out.println("[CONFIG] Loaded from working dir: " + path);
            } catch (Exception ignored) {}

            // 2. Fallback to resources
            if (input == null) {
                input = Config.class
                        .getClassLoader()
                        .getResourceAsStream("config/config.ini");

                if (input != null) {
                    System.out.println("[CONFIG] Loaded from resources: config/config.ini");
                }
            }

            // 3. Apply values
            if (input != null) {
                props.load(input);

                HOST = props.getProperty("HOST", HOST);
                PORT = Integer.parseInt(
                        props.getProperty("PORT", String.valueOf(PORT))
                );
            } else {
                System.out.println("[CONFIG] No config found. Using defaults.");
            }

        } catch (Exception e) {
            System.out.println("[CONFIG] Error loading config. Using defaults.");
        } finally {
            try {
                if (input != null) input.close();
            } catch (Exception ignored) {}
        }

        System.out.println("[CONFIG] Final → " + HOST + ":" + PORT);
    }
}