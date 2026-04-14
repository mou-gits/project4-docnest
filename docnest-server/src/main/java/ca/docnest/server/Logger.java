package ca.docnest.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    private static PrintWriter writer;
    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // Initialize logger (call once at server startup)
    public static void init() {
        try {
            Path logDir = Paths.get("logs");
            Files.createDirectories(logDir);

            String timestamp = LocalDateTime.now().format(FILE_FORMAT);
            Path logFile = logDir.resolve("server_" + timestamp + ".log");

            writer = new PrintWriter(Files.newBufferedWriter(
                    logFile,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            ), true);

            info("Logger initialized. Writing to: " + logFile.toAbsolutePath());

        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize logger", e);
        }
    }

    private static String format(String level, String msg) {
        String ts = LocalDateTime.now().format(TS_FORMAT);
        return "[" + level + "] " + ts + " - " + msg;
    }

    public static void info(String msg) {
        log("INFO", msg);
    }

    public static void error(String msg) {
        log("ERROR", msg);
    }

    private static void log(String level, String msg) {
        String line = format(level, msg);

        // Console
        System.out.println(line);

        // File
        if (writer != null) {
            writer.println(line);
        }
    }
}