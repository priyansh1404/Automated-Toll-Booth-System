package com.tollsystem.service;

import com.tollsystem.model.Transaction;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for writing and reading audit log entries to/from a persistent
 * text file using BufferedWriter/FileWriter and BufferedReader/FileReader.
 *
 * Log file location: toll_audit_log.txt (relative to working directory)
 *
 * Log format per line (pipe-delimited):
 *   txnId|registrationNum|vehicleType|tollAmount|remainingBalance|lane|timestamp|status|message
 *
 * This class is thread-safe: all write operations are synchronized so that
 * concurrent lane workers can safely append to the same file.
 */
public class AuditLoggerService {

    public static final String LOG_FILE_PATH = "toll_audit_log.txt";

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Creates a new AuditLoggerService.
     * If the log file does not exist, writes a header line; otherwise appends.
     */
    public AuditLoggerService() {
        initLogFile();
    }

    /**
     * Ensures the log file exists and writes a session-start header.
     */
    private void initLogFile() {
        File logFile = new File(LOG_FILE_PATH);
        boolean isNew = !logFile.exists() || logFile.length() == 0;

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(logFile, true))) {
            if (isNew) {
                writer.write("TxnID|RegNum|VehicleType|TollAmount|RemainingBalance|Lane|Timestamp|Status|Message");
                writer.newLine();
            }
            writer.write(String.format("--- SESSION START: %s ---",
                    LocalDateTime.now().format(FORMATTER)));
            writer.newLine();
        } catch (IOException e) {
            System.err.println("[AuditLogger] WARNING: Could not initialise log file: "
                    + e.getMessage());
        }
    }

    /**
     * Appends a single transaction record to the audit log file.
     * Synchronized to ensure thread safety when multiple lane workers call it concurrently.
     *
     * @param transaction the completed transaction to log
     */
    public synchronized void logTransaction(Transaction transaction) {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(LOG_FILE_PATH, true))) {
            writer.write(transaction.toAuditLogLine());
            writer.newLine();
        } catch (IOException e) {
            System.err.printf("[AuditLogger] Failed to write transaction %d: %s%n",
                    transaction.getTransactionId(), e.getMessage());
        }
    }

    /**
     * Writes an arbitrary message to the audit log file with a timestamp prefix.
     *
     * @param message the message to log
     */
    public synchronized void logSystemEvent(String message) {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(LOG_FILE_PATH, true))) {
            writer.write(String.format("[EVENT | %s] %s",
                    LocalDateTime.now().format(FORMATTER), message));
            writer.newLine();
        } catch (IOException e) {
            System.err.println("[AuditLogger] Failed to write system event: " + e.getMessage());
        }
    }

    /**
     * Reads and returns all lines from the audit log file as a List.
     *
     * @return list of all raw log lines (empty list if file not found)
     */
    public List<String> readAllLogLines() {
        List<String> lines = new ArrayList<>();
        File logFile = new File(LOG_FILE_PATH);

        if (!logFile.exists()) {
            System.err.println("[AuditLogger] Log file not found: " + LOG_FILE_PATH);
            return lines;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            System.err.println("[AuditLogger] Error reading log file: " + e.getMessage());
        }

        return lines;
    }

    /**
     * Reads only the transaction data lines (skipping header and event lines).
     *
     * @return list of raw transaction log lines
     */
    public List<String> readTransactionLines() {
        List<String> transactionLines = new ArrayList<>();
        for (String line : readAllLogLines()) {
            if (line == null || line.trim().isEmpty() || line.startsWith("TxnID") || line.startsWith("---") || line.startsWith("[EVENT")) {
                continue;
            }
            transactionLines.add(line);
        }
        return transactionLines;
    }

    /** Prints the full contents of the audit log to standard output. */
    public void exportAndDisplayLog() {
        System.out.println("\n========================================");
        System.out.println("       TOLL AUDIT LOG -- FILE EXPORT    ");
        System.out.println("========================================");
        System.out.println("File: " + new File(LOG_FILE_PATH).getAbsolutePath());
        System.out.println("----------------------------------------");

        List<String> lines = readAllLogLines();
        if (lines.isEmpty()) {
            System.out.println("[AuditLogger] Log file is empty.");
        } else {
            lines.forEach(System.out::println);
        }
        System.out.println("========================================");
        System.out.println("Total lines in log: " + lines.size());
    }

    /**
     * Clears the audit log file and re-writes the header.
     */
    public synchronized void clearLog() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE_PATH, false))) {
            writer.write("TxnID|RegNum|VehicleType|TollAmount|RemainingBalance|Lane|Timestamp|Status|Message");
            writer.newLine();
            writer.write(String.format("--- LOG CLEARED: %s ---",
                    LocalDateTime.now().format(FORMATTER)));
            writer.newLine();
            System.out.println("[AuditLogger] Log file cleared successfully.");
        } catch (IOException e) {
            System.err.println("[AuditLogger] Failed to clear log file: " + e.getMessage());
        }
    }

    /** Returns the absolute path of the audit log file. */
    public String getLogFilePath() {
        return new File(LOG_FILE_PATH).getAbsolutePath();
    }
}