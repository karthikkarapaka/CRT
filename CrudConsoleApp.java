import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CrudConsoleApp {

    // --- Database Connection Details ---
    private final String url = "jdbc:oracle:thin:@localhost:1521:XE";
    private final String user = "system";
    private final String pass = "Kunn597"; // Reminder: Use a secure way to handle passwords in production.

    // --- Parser Configuration ---
    private static final String TABLE_TAG_MARKER = "--@table:";

    // --- THIS IS THE CORRECTED SECTION ---
    public static void main(String[] args) {
        // The class now creates an instance of itself correctly.
        CrudConsoleApp dbManager = new CrudConsoleApp();
        dbManager.run();
    }

    /**
     * The main application loop that displays the menu and handles user input.
     */
    public void run() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("🚀 Console Database Management System Ready!");
        System.out.println("Ensure .txt files use tags like '" + TABLE_TAG_MARKER + "TABLENAME'.\n");

        while (true) {
            printMenu();
            System.out.print("Enter your choice: ");
            String choice = scanner.nextLine();

            switch (choice.trim()) {
                case "1":
                    handleGenericOperation("CREATE", "create.txt", scanner);
                    break;
                case "2":
                    handleGenericOperation("INSERT", "insert.txt", scanner);
                    break;
                case "3":
                    handleGenericOperation("UPDATE", "update.txt", scanner);
                    break;
                case "4":
                    handleGenericOperation("DELETE", "delete.txt", scanner);
                    break;
                case "5":
                    handleSelectOperation(scanner);
                    break;
                case "6":
                    System.out.println("👋 Exiting application.");
                    // Do not close the main System.in scanner here
                    return; // Exit the program
                default:
                    System.err.println("❌ Invalid choice. Please enter a number between 1 and 6.");
                    break;
            }
            System.out.println("\n--------------------------------------------------\n");
        }
    }

    private void printMenu() {
        System.out.println("========= MAIN MENU =========");
        System.out.println("1. CREATE (from create.txt)");
        System.out.println("2. INSERT (from insert.txt)");
        System.out.println("3. UPDATE (from update.txt)");
        System.out.println("4. DELETE (from delete.txt)");
        System.out.println("5. SELECT (from select.txt)");
        System.out.println("6. Exit to Main Menu");
        System.out.println("=============================");
    }

    private void handleGenericOperation(String operation, String fileName, Scanner scanner) {
        System.out.println("\n--- Performing " + operation + " Operation ---");
        System.out.print("Enter target table name: ");
        String tableName = scanner.nextLine().trim();

        if (tableName.isEmpty()) {
            System.err.println("❌ Error: Table name cannot be empty.");
            return;
        }

        String fileContent = readQueryFromFile(fileName);
        if (fileContent == null) return;

        String queryBlock = findQueryBlockForTable(fileContent, tableName);
        if (queryBlock == null) {
            System.err.println("❌ Error: No query block found for table '" + tableName + "' in " + fileName + ".");
            return;
        }

        executeStatements(queryBlock);
    }

    private void handleSelectOperation(Scanner scanner) {
        System.out.println("\n--- Performing SELECT Operation ---");
        System.out.print("Enter target query name (e.g., EMPLOYEES, FULL_REPORT): ");
        String queryName = scanner.nextLine().trim();

        if (queryName.isEmpty()) {
            System.err.println("❌ Error: Query name cannot be empty.");
            return;
        }

        String fileContent = readQueryFromFile("select.txt");
        if (fileContent == null) return;

        String queryBlock = findQueryBlockForTable(fileContent, queryName);
        if (queryBlock == null) {
            System.err.println("❌ Error: No SELECT query for '" + queryName + "' in select.txt.");
            return;
        }

        String finalSql = "";
        for (String s : queryBlock.split(";")) {
            if (!s.trim().isEmpty()) {
                finalSql = s.trim();
                break;
            }
        }

        if (finalSql.isEmpty()) {
            System.err.println("❌ Error: No executable statement found in the block for '" + queryName + "'.");
            return;
        }

        System.out.println("🔍 Executing: " + finalSql.replaceAll("\\s+", " "));
        try (Connection con = DriverManager.getConnection(url, user, pass);
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(finalSql)) {

            printResultSet(rs);

        } catch (SQLException ex) {
            System.err.println("❌ Database error: " + ex.getMessage());
        }
    }

    private String findQueryBlockForTable(String fileContent, String tableName) {
        String content = fileContent.replaceAll("\r\n", "\n");
        String[] lines = content.split("\n");
        int lineIndex = -1;

        for (int i = 0; i < lines.length; i++) {
            String trimmedLine = lines[i].trim();
            if (trimmedLine.toUpperCase().startsWith(TABLE_TAG_MARKER.toUpperCase())) {
                String extractedTable = trimmedLine.substring(TABLE_TAG_MARKER.length()).trim();
                if (extractedTable.equalsIgnoreCase(tableName)) {
                    lineIndex = i;
                    break;
                }
            }
        }

        if (lineIndex == -1) {
            return null; // Tag not found
        }

        StringBuilder queryBlock = new StringBuilder();
        for (int i = lineIndex + 1; i < lines.length; i++) {
            if (lines[i].trim().toUpperCase().startsWith(TABLE_TAG_MARKER.toUpperCase())) {
                break;
            }
            queryBlock.append(lines[i]).append("\n");
        }

        return queryBlock.toString().trim();
    }

    private void executeStatements(String queryBlock) {
        if (queryBlock == null || queryBlock.trim().isEmpty()) {
            System.out.println("⚠️ Warning: Query script was empty. Nothing to execute.");
            return;
        }
        String[] statements = queryBlock.split(";");
        try (Connection con = DriverManager.getConnection(url, user, pass)) {
            con.setAutoCommit(false); // Start transaction
            try (Statement stmt = con.createStatement()) {
                int executedCount = 0;
                int totalAffectedRows = 0;
                for (String sql : statements) {
                    if (sql.trim().isEmpty()) {
                        continue;
                    }
                    System.out.println("🔍 Executing: " + sql.trim().replaceAll("\\s+", " "));
                    int affectedRows = stmt.executeUpdate(sql.trim());
                    totalAffectedRows += affectedRows;
                    executedCount++;
                }
                con.commit(); // Commit the transaction
                System.out.println("✅ Success! Executed " + executedCount + " statement(s). Total rows affected: " + totalAffectedRows + ".");
            } catch (SQLException ex) {
                System.err.println("❌ Transaction Failed: " + ex.getMessage());
                System.err.println("--- Rolling back changes ---");
                con.rollback(); // Rollback on any error within the block
            }
        } catch (SQLException ex) {
            System.err.println("❌ Database Connection Error: " + ex.getMessage());
        }
    }

    private String readQueryFromFile(String fileName) {
        try {
            return Files.readString(Paths.get(fileName));
        } catch (IOException e) {
            System.err.println("❌ File Error: Could not read '" + fileName + "'. " + e.getMessage());
            return null;
        }
    }

    private void printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        List<String> headers = new ArrayList<>();
        int[] columnWidths = new int[columnCount];
        for (int i = 1; i <= columnCount; i++) {
            String header = metaData.getColumnName(i);
            headers.add(header);
            columnWidths[i - 1] = header.length();
        }
        List<List<String>> allRows = new ArrayList<>();
        while (rs.next()) {
            List<String> row = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                String value = rs.getObject(i) != null ? rs.getObject(i).toString() : "NULL";
                row.add(value);
                if (value.length() > columnWidths[i - 1]) {
                    columnWidths[i - 1] = value.length();
                }
            }
            allRows.add(row);
        }
        StringBuilder headerLine = new StringBuilder("| ");
        StringBuilder separatorLine = new StringBuilder("+");
        for (int i = 0; i < columnCount; i++) {
            headerLine.append(String.format("%-" + columnWidths[i] + "s", headers.get(i))).append(" | ");
            separatorLine.append("-".repeat(columnWidths[i] + 2)).append("+");
        }
        System.out.println(separatorLine);
        System.out.println(headerLine);
        System.out.println(separatorLine);
        if (allRows.isEmpty()) {
            System.out.println("| " + String.format("%-" + (separatorLine.length() - 4) + "s", "No records found.") + " |");
        } else {
            for (List<String> row : allRows) {
                StringBuilder rowLine = new StringBuilder("| ");
                for (int i = 0; i < columnCount; i++) {
                    rowLine.append(String.format("%-" + columnWidths[i] + "s", row.get(i))).append(" | ");
                }
                System.out.println(rowLine);
            }
        }
        System.out.println(separatorLine);
        System.out.println("✅ Retrieved " + allRows.size() + " records.");
    }
}