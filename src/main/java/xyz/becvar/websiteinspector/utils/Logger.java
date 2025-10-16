package xyz.becvar.websiteinspector.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedWriter;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import xyz.becvar.websiteinspector.core.Config;

/**
 * This class handles logging to the console and file
 */
public class Logger {

    // ANSI color codes
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_CYAN = "\u001B[36m";

    private static final String CONSOLE_PREFIX = ANSI_YELLOW + "[" + ANSI_GREEN + Config.APP_PREFIX + ANSI_YELLOW + "]" + ANSI_CYAN;
    private static String lastProgressMessage = "";
    private static BufferedWriter fileWriter = null;

    /**
     * Initializes file logging for the given domain
     * 
     * @param domain The domain to log for
     */
    public static void initFileLogging(String domain) {
        try {
            File logsDir = new File("logs");
            if (!logsDir.exists()) {
                if (!logsDir.mkdirs()) {
                    printError("Failed to create logs directory: " + logsDir.getAbsolutePath());
                    fileWriter = null;
                    return;
                }
            }
            String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String fileName = String.format("logs/%s-%s.log", domain.replaceAll("[^a-zA-Z0-9.-]", "_"), dateTime);
            fileWriter = new BufferedWriter(new FileWriter(fileName, StandardCharsets.UTF_8));
        } catch (IOException e) {
            printError("Failed to initialize file logger: " + e.getMessage());
            fileWriter = null;
        }
    }

    /**
     * Closes the file logger
     */
    public static void closeFileLogging() {
        if (fileWriter != null) {
            try {
                fileWriter.close();
            } catch (IOException e) {
                printError("Failed to close file logger: " + e.getMessage());
            }
        }
    }

    /**
     * Logs a message to the file
     * 
     * @param cleanMessage The message to log
     */
    private static synchronized void logToFile(String cleanMessage) {
        if (fileWriter != null) {
            try {
                fileWriter.write(cleanMessage + "\n");
            } catch (IOException e) {
                Logger.printError("Failed to write to log file: " + e.getMessage());
            }
        }
    }

    /**
     * Clears the console line
     */
    private static void clearConsoleLine() {
        System.out.print("\r\u001B[K");
    }

    /**
     * Reprints the progress line
     */
    private static void reprintProgressLine() {
        System.out.print(lastProgressMessage);
    }

    /**
     * Prints a spacer line
     */
    public static void printSpacer() {
        String message = "========================================================================================";
        logToFile(message);
        clearConsoleLine();
        System.out.println(ANSI_CYAN + message + ANSI_RESET);
        reprintProgressLine();
    }

    /**
     * Logs a status message to the console
     * 
     * @param msg The message to log
     */
    public static void logStatus(String msg) {
        clearConsoleLine();
        System.out.println(CONSOLE_PREFIX + ": " + ANSI_CYAN + msg + ANSI_RESET);
        reprintProgressLine();
    }

    /**
     * Prompts the user for input
     * 
     * @param msg The message to prompt with
     */
    public static void prompt(String msg) {
        System.out.print(CONSOLE_PREFIX + ": " + msg + ": " + ANSI_RESET);
    }

    /**
     * Logs a message to the console
     * 
     * @param msg The message to log
     */
    public static void log(String msg) {
        logToFile(msg);
        clearConsoleLine();
        System.out.println(CONSOLE_PREFIX + ": " + ANSI_CYAN + msg + ANSI_RESET);
        reprintProgressLine();
    }

    /**
     * Logs a raw message to the console
     * 
     * @param msg The message to log
     */
    public static void rawLog(String msg) {
        logToFile(msg);
        clearConsoleLine();
        System.out.print(ANSI_GREEN + msg + ANSI_RESET);
        reprintProgressLine();
    }

    /**
     * Prints a key-value pair in a colored format
     * 
     * @param key The key to print
     * @param value The value to print
     */
    public static void printColoredKeyValue(String key, String value) {
        if (value == null || value.trim().isEmpty() || value.equalsIgnoreCase("null")) {
            value = "Not specified";
        }
        logToFile(key + ": " + value);
        clearConsoleLine();
        System.out.println(ANSI_BLUE + key + ANSI_RESET + ": " + ANSI_CYAN + value + ANSI_RESET);
        reprintProgressLine();
    }

    /**
     * Prints a success message in a colored format
     * 
     * @param key The key to print
     * @param value The value to print
     */
    public static void printSuccess(String key, String value) {
        logToFile(key + ": " + value);
        clearConsoleLine();
        System.out.println(ANSI_GREEN + "[+] " + key + ANSI_RESET + ": " + value);
        reprintProgressLine();
    }

    /**
     * Prints a warning message in a colored format
     * 
     * @param key The key to print
     * @param value The value to print
     */
    public static void printWarning(String key, String value) {
        String separator = value.isEmpty() ? "" : ": ";
        logToFile("[!] " + key + separator + value);
        clearConsoleLine();
        System.out.println(ANSI_YELLOW + "[!] " + key + ANSI_RESET + separator + value);
        reprintProgressLine();
    }

    /**
     * Prints an error message in a colored format
     * 
     * @param message The message to print
     */
    public static void printError(String message) {
        logToFile("[ERROR] " + message);
        clearConsoleLine();
        System.out.println(ANSI_RED + "[ERROR] " + message + ANSI_RESET);
        reprintProgressLine();
    }

    /**
     * Prints a progress message
     * 
     * @param msg The message to print
     */ 
    public static void printProgress(String msg) {
        lastProgressMessage = "\r" + CONSOLE_PREFIX + ": " + msg;
        System.out.print(lastProgressMessage);
    }

    /**
     * Clears the progress message  
     */
    public static void clearProgress() {
        clearConsoleLine();
        lastProgressMessage = "";
    }
}
