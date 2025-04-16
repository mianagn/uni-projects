package com;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public class HistoryController {
    public Button StatisticsButton;
    public Button HomeButton;
    @FXML
    private TableView<Transaction> allTransactionsTable;
    @FXML
    private TableColumn<Transaction, String> dateColumn;
    @FXML
    private TableColumn<Transaction, String> categoryColumn;
    @FXML
    private TableColumn<Transaction, Double> amountColumn;
    @FXML
    private TableColumn<Transaction, Void> deleteColumn;
    @FXML
    private LineChart<String, Number> balanceTrendChart;
    @FXML
    public Label dateTime;

    public void initialize() {
        setupTransactionTable();
        setupBalanceTrendChart();
        DateTimeUpdater.start(dateTime);

        // Add delete buttons to the table
        addDeleteButtonToTable();

        // Style the chart axis labels to be white
        balanceTrendChart.lookupAll(".axis-label").forEach(node ->
                node.setStyle("-fx-text-fill: white;"));

        // You can also style the chart title if needed
        balanceTrendChart.lookup(".chart-title").setStyle("-fx-text-fill: white;");

        // Style the transaction table
        styleTransactionTable();
    }

    private void styleTransactionTable() {
        // Apply CSS styling to the table
        String tableStyle =
                "-fx-background-color: rgba(255, 255, 255, 0.1);" +  // Semi-transparent background
                        "-fx-text-fill: white;" +                            // Text color
                        "-fx-font-size: 14px;";                              // Font size

        String headerStyle =
                "-fx-background-color: rgba(0, 0, 0, 0.5);" +        // Darker header background
                        "-fx-text-fill: white;" +                            // Header text color
                        "-fx-font-weight: bold;";                            // Bold headers

        // Apply styles
        allTransactionsTable.setStyle(tableStyle);

        // Style column headers
        for (TableColumn<?, ?> column : allTransactionsTable.getColumns()) {
            column.setStyle(headerStyle);
        }

        // Apply cell styling (this requires a CSS file)
        Scene scene = allTransactionsTable.getScene();
        if (scene != null) {
            scene.getStylesheets().add(getClass().getResource("/com/styles.css").toExternalForm());
        } else {
            // If the table is not yet in a scene, we need to wait
            allTransactionsTable.sceneProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    newValue.getStylesheets().add(getClass().getResource("/com/styles.css").toExternalForm());
                }
            });
        }
    }

    private void setupTransactionTable() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));

        // Format the amount column to show currency
        amountColumn.setCellFactory(column -> new TableCell<Transaction, Double>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    // Format with currency symbol and color based on positive/negative
                    String formattedAmount = String.format("%.2f €", amount);
                    setText(formattedAmount);

                    if (amount < 0) {
                        setStyle("-fx-text-fill: #ff6666;"); // Red for negative amounts
                    } else {
                        setStyle("-fx-text-fill: #66ff66;"); // Green for positive amounts
                    }
                }
            }
        });

        refreshTransactionTable();
    }

    private void refreshTransactionTable() {
        List<Transaction> allTransactions = SQLiteDatabase.getAllTransactions();
        ObservableList<Transaction> observableTransactions = FXCollections.observableArrayList(allTransactions);
        allTransactionsTable.setItems(observableTransactions);
    }

    private void setupBalanceTrendChart() {
        balanceTrendChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Balance Over Time");

        List<Transaction> transactions = SQLiteDatabase.getAllTransactions();
        double runningBalance = 0;

        // Use each transaction, not grouped by date
        for (int i = 0; i < transactions.size(); i++) {
            Transaction t = transactions.get(i);
            runningBalance += t.getAmount();
            String label = t.getDate() + " #" + (i + 1); // makes labels unique for same-day transactions
            series.getData().add(new XYChart.Data<>(label, runningBalance));
        }

        balanceTrendChart.getData().add(series);
    }
    @FXML
    private void handleStatisticsButtonClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/statistics-view.fxml"));
            Scene statisticsScene = new Scene(loader.load());
            Stage stage = (Stage) StatisticsButton.getScene().getWindow();
            stage.setScene(statisticsScene);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleHomeButtonClick() {
        try {
            // Load Home view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Home-view.fxml"));
            Parent homeView = loader.load();

            // Get the current stage and set the new scene
            Stage stage = (Stage) HomeButton.getScene().getWindow();
            stage.setScene(new Scene(homeView));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addDeleteButtonToTable() {
        deleteColumn.setCellFactory(col -> new TableCell<>() {
            private final Button deleteButton = new Button("X");

            {
                deleteButton.setStyle("-fx-background-color: #ff3333; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5; -fx-background-radius: 3;");
                deleteButton.setOnAction(event -> {
                    Transaction transaction = getTableView().getItems().get(getIndex());
                    int transactionId = transaction.getId();

                    // Show confirmation dialog
                    Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmDialog.setTitle("Delete Transaction");
                    confirmDialog.setHeaderText("Delete Transaction");
                    confirmDialog.setContentText("Are you sure you want to delete this transaction?");

                    // Style the dialog (optional)
                    DialogPane dialogPane = confirmDialog.getDialogPane();
                    dialogPane.getStylesheets().add(getClass().getResource("/com/styles.css").toExternalForm());

                    Optional<ButtonType> result = confirmDialog.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        boolean success = SQLiteDatabase.deleteTransaction(transactionId);
                        if (success) {
                            refreshTransactionTable(); // Refresh the table after deletion
                            setupBalanceTrendChart(); // Refresh chart after deletion

                            // Show success notification
                            Alert successNotification = new Alert(Alert.AlertType.INFORMATION);
                            successNotification.setTitle("Success");
                            successNotification.setHeaderText(null);
                            successNotification.setContentText("Transaction deleted successfully!");
                            successNotification.showAndWait();
                        } else {
                            // Show error notification
                            Alert errorNotification = new Alert(Alert.AlertType.ERROR);
                            errorNotification.setTitle("Error");
                            errorNotification.setHeaderText(null);
                            errorNotification.setContentText("Failed to delete transaction. Please try again.");
                            errorNotification.showAndWait();
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteButton);
                }
            }
        });
    }
}
