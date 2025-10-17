package xyz.becvar.websiteinspector.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import xyz.becvar.websiteinspector.OutputFormat;
import xyz.becvar.websiteinspector.core.AnalysisResult;
import xyz.becvar.websiteinspector.core.Config;
import xyz.becvar.websiteinspector.dto.JsonReport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
    private static OutputFormat outputFormat = OutputFormat.NORMAL;
    private static List<AnalysisResult> analysisResults = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

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
     * Sets the output format for the logger.
     *
     * @param format The desired output format (NORMAL or JSON).
     */
    public static void setOutputFormat(OutputFormat format) {
        outputFormat = format;
    }

    /**
     * Adds an AnalysisResult to the list for JSON output.
     *
     * @param result The AnalysisResult to add.
     */
    public static void addAnalysisResult(AnalysisResult result) {
        if (outputFormat == OutputFormat.JSON) {
            analysisResults.add(result);
        }
    }

    /**
     * Prints the collected analysis results as a JSON report to System.out.
     * This method should only be called when the output format is JSON.
     * 
     * @param scanDuration The duration of the scan in milliseconds.
     */
    public static void printJsonReport(long scanDuration) {
        if (outputFormat == OutputFormat.JSON) {
            JsonReport report = new JsonReport(scanDuration, analysisResults);
            System.out.println(GSON.toJson(report));
        }
    }

    /**
     * Formats duration in milliseconds to a human-readable format.
     * 
     * @param millis The duration in milliseconds.
     * 
     * @return A string representing the duration.
     */
    private static String formatDuration(long millis) {
        long hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(millis);
        millis -= java.util.concurrent.TimeUnit.HOURS.toMillis(hours);
        long minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= java.util.concurrent.TimeUnit.MINUTES.toMillis(minutes);
        long seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(millis);
        millis -= java.util.concurrent.TimeUnit.SECONDS.toMillis(seconds);

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (seconds > 0) {
            sb.append(seconds).append("s ");
        }
        if (millis > 0 || sb.length() == 0) {
            sb.append(millis).append("ms");
        }

        return sb.toString().trim();
    }

    /**
     * Prints the scan duration.
     * 
     * @param scanDuration The duration of the scan in milliseconds.
     */
    public static synchronized void printScanDuration(long scanDuration) {
        log("Scan finished in " + formatDuration(scanDuration));
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
    public static synchronized void printSpacer() {
        String message = "========================================================================================";
        logToFile(message);
        if (outputFormat == OutputFormat.NORMAL) {
            clearConsoleLine();
            System.out.println(ANSI_CYAN + message + ANSI_RESET);
            reprintProgressLine();
        } else {
            System.err.println(ANSI_CYAN + message + ANSI_RESET);
        }
    }

    /**
     * Logs a status message to the console
     * 
     * @param msg The message to log
     */
    public static synchronized void logStatus(String msg) {
        if (outputFormat == OutputFormat.NORMAL) {
            clearConsoleLine();
            System.out.println(CONSOLE_PREFIX + ": " + ANSI_CYAN + msg + ANSI_RESET);
            reprintProgressLine();
        } else {
            System.err.print("\r" + CONSOLE_PREFIX + ": " + ANSI_CYAN + msg + ANSI_RESET + "\u001B[K"); // Clear to end of line
            System.err.flush();
        }
    }

    /**
     * Prompts the user for input
     * 
     * @param msg The message to prompt with
     */
    public static void prompt(String msg) {
        System.err.print(CONSOLE_PREFIX + ": " + msg + ": " + ANSI_RESET);
    }

    /**
     * Logs a message to the console
     * 
     * @param msg The message to log
     */
    public static synchronized void log(String msg) {
        logToFile(msg);
        if (outputFormat == OutputFormat.NORMAL) {
            clearConsoleLine();
            System.out.println(CONSOLE_PREFIX + ": " + ANSI_CYAN + msg + ANSI_RESET);
            reprintProgressLine();
        } else {
            System.err.println(CONSOLE_PREFIX + ": " + ANSI_CYAN + msg + ANSI_RESET);
        }
    }

    /**
     * Logs a raw message to the console
     * 
     * @param msg The message to log
     */
    public static synchronized void rawLog(String msg) {
        logToFile(msg);
        if (outputFormat == OutputFormat.NORMAL) {
            clearConsoleLine();
            System.out.print(ANSI_GREEN + msg + ANSI_RESET);
            reprintProgressLine();
        } else {
            System.err.print(ANSI_GREEN + msg + ANSI_RESET);
            System.err.flush();
        }
    }

    /**
     * Prints a key-value pair in a colored format
     * 
     * @param key The key to print
     * @param value The value to print
     */
    public static synchronized void printColoredKeyValue(String key, String value) {
        if (value == null || value.trim().isEmpty() || value.equalsIgnoreCase("null")) {
            value = "Not specified";
        }
        logToFile(key + ": " + value);
        if (outputFormat == OutputFormat.NORMAL) {
            clearConsoleLine();
            System.out.println(ANSI_BLUE + key + ANSI_RESET + ": " + ANSI_CYAN + value + ANSI_RESET);
            reprintProgressLine();
        } else {
            System.err.println(ANSI_BLUE + key + ANSI_RESET + ": " + ANSI_CYAN + value + ANSI_RESET);
        }
    }

    /**
     * Prints a success message in a colored format
     * 
     * @param key The key to print
     * @param value The value to print
     */
    public static synchronized void printSuccess(String key, String value) {
        logToFile(key + ": " + value);
        if (outputFormat == OutputFormat.NORMAL) {
            clearConsoleLine();
            System.out.println(ANSI_GREEN + "[+] " + key + ANSI_RESET + ": " + value);
            reprintProgressLine();
        } else {
            System.err.println(ANSI_GREEN + "[+] " + key + ANSI_RESET + ": " + value);
        }
    }

    /**
     * Prints a warning message in a colored format
     * 
     * @param key The key to print
     * @param value The value to print
     */
    public static synchronized void printWarning(String key, String value) {
        String separator = value.isEmpty() ? "" : ": ";
        logToFile("[!] " + key + separator + value);
        if (outputFormat == OutputFormat.NORMAL) {
            clearConsoleLine();
            System.out.println(ANSI_YELLOW + "[!] " + key + ANSI_RESET + separator + value);
            reprintProgressLine();
        } else {
            System.err.println(ANSI_YELLOW + "[!] " + key + ANSI_RESET + separator + value);
        }
    }

    /**
     * Prints an error message in a colored format
     * 
     * @param message The message to print
     */
    public static synchronized void printError(String message) {
        logToFile("[ERROR] " + message);
        if (outputFormat == OutputFormat.NORMAL) {
            clearConsoleLine();
            System.out.println(ANSI_RED + "[ERROR] " + message + ANSI_RESET);
            reprintProgressLine();
        } else {
            System.err.println(ANSI_RED + "[ERROR] " + message + ANSI_RESET);
        }
    }

    /**
     * Prints a progress message
     * 
     * @param msg The message to print
     */ 
    public static synchronized void printProgress(String msg) {
        lastProgressMessage = "\r" + CONSOLE_PREFIX + ": " + msg;
        if (outputFormat == OutputFormat.NORMAL) {
            System.out.print(lastProgressMessage);
        } else {
            System.err.print(lastProgressMessage);
            System.err.flush();
        }
    }

    /**
     * Clears the progress message  
     */
    public static synchronized void clearProgress() {
        if (outputFormat == OutputFormat.NORMAL) {
            clearConsoleLine();
        } else { // For JSON output, clear the current line on System.err
            System.err.print("\r\u001B[K");
            System.err.flush();
        }
        lastProgressMessage = "";
    }
}
