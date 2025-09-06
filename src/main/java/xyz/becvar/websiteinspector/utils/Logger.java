package xyz.becvar.websiteinspector.utils;

import xyz.becvar.websiteinspector.Main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    // ANSI color codes
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_CYAN = "\u001B[36m";

    private static final String CONSOLE_PREFIX = ANSI_YELLOW + "[" + ANSI_GREEN + Main.APP_PREFIX + ANSI_YELLOW + "]" + ANSI_CYAN;
    private static String lastProgressMessage = "";
    private static BufferedWriter fileWriter = null;

    public static void initFileLogging(String domain) {
        try {
            File logsDir = new File("logs");
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }
            String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String fileName = String.format("logs/%s-%s.log", domain.replaceAll("[^a-zA-Z0-9.-]", "_"), dateTime);
            fileWriter = new BufferedWriter(new FileWriter(fileName));
        } catch (IOException e) {
            printError("Failed to initialize file logger: " + e.getMessage());
            fileWriter = null;
        }
    }

    public static void closeFileLogging() {
        if (fileWriter != null) {
            try {
                fileWriter.close();
            } catch (IOException e) {
                printError("Failed to close file logger: " + e.getMessage());
            }
        }
    }

    private static void logToFile(String cleanMessage) {
        if (fileWriter != null) {
            try {
                fileWriter.write(cleanMessage + "\n");
            } catch (IOException e) {
                // Can't do much here, maybe log to console that file logging failed
            }
        }
    }

    private static void clearConsoleLine() {
        System.out.print("\r\u001B[K");
    }

    private static void reprintProgressLine() {
        System.out.print(lastProgressMessage);
    }

    public static void printSpacer() {
        String message = "========================================================================================";
        logToFile(message);
        clearConsoleLine();
        System.out.println(ANSI_CYAN + message + ANSI_RESET);
        reprintProgressLine();
    }

    public static void logStatus(String msg) {
        // This method logs only to the console, not to the file.
        clearConsoleLine();
        System.out.println(CONSOLE_PREFIX + ": " + ANSI_CYAN + msg + ANSI_RESET);
        reprintProgressLine();
    }

    public static void prompt(String msg) {
        // This method does not log to file, it's for interactive console prompts only.
        System.out.print(CONSOLE_PREFIX + ": " + msg + ": " + ANSI_RESET);
    }

    public static void log(String msg) {
        logToFile(msg);
        clearConsoleLine();
        System.out.println(CONSOLE_PREFIX + ": " + ANSI_CYAN + msg + ANSI_RESET);
        reprintProgressLine();
    }

    public static void rawLog(String msg) {
        logToFile(msg);
        clearConsoleLine();
        System.out.println(ANSI_GREEN + msg + ANSI_RESET);
        reprintProgressLine();
    }

    public static void printColoredKeyValue(String key, String value) {
        if (value == null || value.trim().isEmpty() || value.equalsIgnoreCase("null")) {
            value = "Not specified";
        }
        logToFile(key + ": " + value);
        clearConsoleLine();
        System.out.println(ANSI_BLUE + key + ANSI_RESET + ": " + ANSI_CYAN + value + ANSI_RESET);
        reprintProgressLine();
    }

    public static void printSuccess(String key, String value) {
        logToFile(key + ": " + value);
        clearConsoleLine();
        System.out.println(ANSI_GREEN + "[+] " + key + ANSI_RESET + ": " + value);
        reprintProgressLine();
    }

    public static void printWarning(String key, String value) {
        logToFile("[!] " + key + ": " + value);
        clearConsoleLine();
        System.out.println(ANSI_YELLOW + "[!] " + key + ANSI_RESET + ": " + value);
        reprintProgressLine();
    }

    public static void printError(String message) {
        logToFile("[ERROR] " + message);
        clearConsoleLine();
        System.out.println(ANSI_RED + "[ERROR] " + message + ANSI_RESET);
        reprintProgressLine();
    }

    public static void printProgress(String msg) {
        lastProgressMessage = "\r" + CONSOLE_PREFIX + ": " + msg;
        System.out.print(lastProgressMessage);
    }

    public static void clearProgress() {
        clearConsoleLine();
        lastProgressMessage = "";
    }
}
