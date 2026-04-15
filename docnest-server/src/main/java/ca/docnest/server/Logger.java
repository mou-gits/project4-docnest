package ca.docnest.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @class Logger
 * @brief Provides simple logging functionality for the DocNest server.
 *
 * @details
 * The {@code Logger} class is a lightweight utility used to record server
 * activity and errors to both the console and a log file.
 *
 * It supports:
 * <ul>
 *   <li>Initialization of a timestamped log file at server startup</li>
 *   <li>Writing informational messages</li>
 *   <li>Writing error messages</li>
 *   <li>Formatting log entries with timestamps and severity levels</li>
 * </ul>
 *
 * Log files are stored in a {@code logs} directory, and each server run
 * creates a separate file named using the current date and time.
 *
 * This class is designed for static use only and does not need to be
 * instantiated.
 */
public class Logger {

    /**
     * @brief Writer used to append log messages to the current log file.
     *
     * @details
     * This field is initialized in {@link #init()} and remains available for
     * subsequent logging operations throughout the server runtime.
     */
    private static PrintWriter writer;

    /**
     * @brief Timestamp formatter used for individual log messages.
     *
     * @details
     * Produces timestamps in the format {@code yyyy-MM-dd HH:mm:ss}.
     */
    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * @brief Timestamp formatter used for log file names.
     *
     * @details
     * Produces timestamps in the format {@code yyyyMMdd_HHmmss}.
     */
    private static final DateTimeFormatter FILE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * @brief Initializes the logger and creates a new log file.
     *
     * @details
     * This method should be called once during server startup.
     *
     * It performs the following actions:
     * <ol>
     *   <li>Creates the {@code logs} directory if it does not already exist</li>
     *   <li>Generates a timestamped log file name</li>
     *   <li>Opens a buffered writer in append mode</li>
     *   <li>Wraps the writer in a {@link PrintWriter} with auto-flush enabled</li>
     *   <li>Writes an initialization message to the log</li>
     * </ol>
     *
     * If initialization fails, the method throws a runtime exception because
     * logging is considered an essential server service.
     *
     * @throws RuntimeException If the log directory or file cannot be created.
     */
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

    /**
     * @brief Formats a log entry with severity level and timestamp.
     *
     * @details
     * Builds a single formatted log line in the form:
     * {@code [LEVEL] yyyy-MM-dd HH:mm:ss - message}
     *
     * @param level The severity level, such as {@code INFO} or {@code ERROR}.
     * @param msg The log message text.
     *
     * @return A fully formatted log entry string.
     */
    private static String format(String level, String msg) {
        String ts = LocalDateTime.now().format(TS_FORMAT);
        return "[" + level + "] " + ts + " - " + msg;
    }

    /**
     * @brief Logs an informational message.
     *
     * @details
     * This is a convenience wrapper around {@link #log(String, String)} using
     * the severity level {@code INFO}.
     *
     * @param msg The informational message to log.
     */
    public static void info(String msg) {
        log("INFO", msg);
    }

    /**
     * @brief Logs an error message.
     *
     * @details
     * This is a convenience wrapper around {@link #log(String, String)} using
     * the severity level {@code ERROR}.
     *
     * @param msg The error message to log.
     */
    public static void error(String msg) {
        log("ERROR", msg);
    }

    /**
     * @brief Writes a formatted log entry to the console and log file.
     *
     * @details
     * This method formats the message, prints it to standard output, and also
     * writes it to the active log file if the logger has been initialized.
     *
     * If {@link #init()} has not yet been called, the message will still appear
     * on the console but will not be written to a file.
     *
     * @param level The severity level of the log entry.
     * @param msg The message content to record.
     */
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