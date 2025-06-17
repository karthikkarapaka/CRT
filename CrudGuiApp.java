import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class CrudGuiApp extends Application {

    private final String url = "jdbc:oracle:thin:@localhost:1521:XE";
    private final String user = "system";
    private final String pass = "Kunn597"; // Reminder: Use a secure way to handle passwords in production.

    private VBox mainContainer;
    private TextArea outputArea;
    private static final String TABLE_TAG_MARKER = "--@table:";

    @Override
    public void start(Stage stage) {
        stage.setTitle("Database File Parser");

        Button createBtn = new Button("CREATE");
        Button insertBtn = new Button("INSERT");
        Button updateBtn = new Button("UPDATE");
        Button deleteBtn = new Button("DELETE");
        Button selectBtn = new Button("SELECT");

        String buttonStyle = "-fx-font-size: 14px; -fx-min-width: 150px; -fx-min-height: 40px; -fx-background-radius: 8;";
        createBtn.setStyle(buttonStyle + "-fx-background-color: #4CAF50; -fx-text-fill: white;");
        insertBtn.setStyle(buttonStyle + "-fx-background-color: #2196F3; -fx-text-fill: white;");
        updateBtn.setStyle(buttonStyle + "-fx-background-color: #FF9800; -fx-text-fill: white;");
        deleteBtn.setStyle(buttonStyle + "-fx-background-color: #F44336; -fx-text-fill: white;");
        selectBtn.setStyle(buttonStyle + "-fx-background-color: #9C27B0; -fx-text-fill: white;");

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPrefHeight(250);
        outputArea.setStyle("-fx-font-family: 'Monospaced', 'Courier New'; -fx-control-inner-background: #f0f0f0; -fx-border-color: #ccc; -fx-border-radius: 5;");
        outputArea.setWrapText(true);

        mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(10));
        mainContainer.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e0e0e0; -fx-border-radius: 8; -fx-padding: 15;");

        // --- ACTION HANDLERS ---
        createBtn.setOnAction(e -> showGenericForm("CREATE", "create.txt", "#4CAF50"));
        insertBtn.setOnAction(e -> showGenericForm("INSERT", "insert.txt", "#2196F3"));
        updateBtn.setOnAction(e -> showGenericForm("UPDATE", "update.txt", "#FF9800"));
        deleteBtn.setOnAction(e -> showGenericForm("DELETE", "delete.txt", "#F44336"));
        selectBtn.setOnAction(e -> showSelectForm()); // Select has its own form to show a results table

        HBox buttonRow = new HBox(15, createBtn, insertBtn, updateBtn, deleteBtn, selectBtn);
        buttonRow.setAlignment(Pos.CENTER);
        buttonRow.setPadding(new Insets(10, 0, 10, 0));

        VBox root = new VBox(20,
                new Label("Database File Parser") {{ setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333;"); }},
                buttonRow,
                new Separator() {{ setPadding(new Insets(10, 0, 0, 0)); }},
                mainContainer,
                new Label("Application Output:") {{ setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #555; -fx-padding: 10 0 5 0;"); }},
                outputArea
        );
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: #f5f5f5;");

        Scene scene = new Scene(new ScrollPane(root), 850, 800);
        stage.setScene(scene);
        stage.show();

        outputArea.setText("🚀 Database Management System Ready!\n");
        outputArea.appendText("Enter a table name to perform a file-based operation.\n");
        outputArea.appendText("Ensure .txt files use tags like '" + TABLE_TAG_MARKER + "TABLENAME'.\n\n");
    }

    private void clearMainContainer() {
        mainContainer.getChildren().clear();
    }

    private void showGenericForm(String title, String queryFileName, String colorHex) {
        clearMainContainer();
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");
        TextField tableNameField = new TextField();
        tableNameField.setPromptText("Enter target table name (e.g., EMPLOYEES)");
        tableNameField.setMaxWidth(400);
        Button executeBtn = new Button("Execute from " + queryFileName);
        executeBtn.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");

        executeBtn.setOnAction(e -> {
            String tableName = tableNameField.getText().trim();
            if (tableName.isEmpty()) {
                outputArea.appendText("❌ Error: Please enter a table name.\n");
                return;
            }
            String fileContent = readQueryFromFile(queryFileName);
            if (fileContent == null) return;

            String queryBlock = findQueryBlockForTable(fileContent, tableName);
            if (queryBlock == null) {
                outputArea.appendText("❌ Error: No query block found for table '" + tableName + "' in " + queryFileName + ".\n"
                        + "   Please ensure the file contains a tag like: " + TABLE_TAG_MARKER + " " + tableName.toUpperCase() + "\n\n");
                return;
            }
            executeStatements(queryBlock);
        });

        mainContainer.getChildren().addAll(
                titleLabel,
                new Label("Enter a table name to find its query block in " + queryFileName + "."),
                new HBox(15, new Label("Target Table:"), tableNameField),
                new Separator(),
                executeBtn
        );
    }

    private void showSelectForm() {
        clearMainContainer();
        Label titleLabel = new Label("SELECT DATA");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #9C27B0;");
        TextField tableNameField = new TextField();
        tableNameField.setPromptText("Enter target table name");
        tableNameField.setMaxWidth(400);
        Button selectDataBtn = new Button("Execute from select.txt");
        selectDataBtn.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");
        TableView<Map<String, Object>> resultTable = new TableView<>();
        resultTable.setPrefHeight(300);
        resultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        resultTable.setPlaceholder(new Label("Enter a table name and click the button to see results."));

        selectDataBtn.setOnAction(e -> {
            String tableName = tableNameField.getText().trim();
            if (tableName.isEmpty()) {
                outputArea.appendText("❌ Error: Please enter a table name.\n");
                return;
            }
            String fileContent = readQueryFromFile("select.txt");
            if (fileContent == null) return;

            String queryBlock = findQueryBlockForTable(fileContent, tableName);
            if (queryBlock == null) {
                outputArea.appendText("❌ Error: No SELECT query for '" + tableName + "' in select.txt.\n\n");
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
                outputArea.appendText("❌ Error: No executable statement found in the block for '" + tableName + "'.\n");
                return;
            }

            outputArea.appendText("🔍 Executing: " + finalSql + "\n");
            try (Connection con = DriverManager.getConnection(url, user, pass);
                 Statement stmt = con.createStatement();
                 ResultSet rs = stmt.executeQuery(finalSql)) {

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                resultTable.getItems().clear();
                resultTable.getColumns().clear();

                for (int i = 1; i <= columnCount; i++) {
                    final String columnName = metaData.getColumnName(i);
                    TableColumn<Map<String, Object>, String> column = new TableColumn<>(columnName);
                    column.setCellValueFactory(new MapValueFactory(columnName));
                    resultTable.getColumns().add(column);
                }

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnName(i), rs.getObject(i) != null ? rs.getObject(i).toString() : "NULL");
                    }
                    resultTable.getItems().add(row);
                }
                outputArea.appendText("✅ Retrieved " + resultTable.getItems().size() + " records.\n\n");
            } catch (SQLException ex) {
                outputArea.appendText("❌ Database error: " + ex.getMessage() + "\n\n");
            }
        });

        mainContainer.getChildren().addAll(
                titleLabel,
                new HBox(15, new Label("Target Table:"), tableNameField, selectDataBtn),
                new Separator(),
                new Label("Query Results:") {{ setStyle("-fx-font-weight: bold;"); }},
                resultTable
        );
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
            outputArea.appendText("⚠️ Warning: Query script was empty. Nothing to execute.\n\n");
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
                    outputArea.appendText("🔍 Executing: " + sql.trim().replaceAll("\\s+", " ") + "\n");
                    int affectedRows = stmt.executeUpdate(sql.trim());
                    totalAffectedRows += affectedRows;
                    executedCount++;
                }

                con.commit(); // Commit the transaction
                outputArea.appendText("✅ Success! Executed " + executedCount + " statement(s). Total rows affected: " + totalAffectedRows + ".\n\n");
            } catch (SQLException ex) {
                outputArea.appendText("❌ Transaction Failed: " + ex.getMessage() + "\n--- Rolling back changes ---\n\n");
                con.rollback(); // Rollback on any error within the block
            }
        } catch (SQLException ex) {
            outputArea.appendText("❌ Database Connection Error: " + ex.getMessage() + "\n\n");
        }
    }

    private String readQueryFromFile(String fileName) {
        try {
            return Files.readString(Paths.get(fileName));
        } catch (IOException e) {
            outputArea.appendText("❌ File Error: Could not read '" + fileName + "'.\n" + e.getMessage() + "\n\n");
            return null;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}