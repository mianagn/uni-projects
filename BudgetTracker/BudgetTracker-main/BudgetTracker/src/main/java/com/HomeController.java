package com;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

import java.util.List;

public class HomeController {

    @FXML public Pane headerContainer;
    @FXML public Text welcome;
    @FXML public ImageView profilePic;
    @FXML public Button StatisticsButton;
    @FXML public Button HomeButton;
    @FXML public Button HistoryButton;
    @FXML public Label dateTime;
    @FXML public Pane midContainer;
    @FXML public Label balance;
    @FXML public Label balanceText;

    @FXML private TableView<Transaction> recentTable;
    @FXML private TableColumn<Transaction, String> descriptionCol;
    @FXML private TableColumn<Transaction, Double> amountCol;
    @FXML private TableColumn<Transaction, String> dateCol;


    private static HomeController instance;

    @FXML
    public void initialize() {
        instance = this;
        DateTimeUpdater.start(dateTime);
        // Set up table columns
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));

        // Load initial data
        updateBalanceLabel();
        loadRecentTransactions();
        styleTransactionTable();
    }

    // Make it accessible from other controllers
    public static HomeController getInstance() {
        return instance;
    }

    @FXML
    private void handleIncomeButtonClick() {
        openTransactionWindow("Income");
    }

    @FXML
    private void handleExpenseButtonClick() {
        openTransactionWindow("Expense");
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

        String cellStyle =
                "-fx-text-fill: white;";                             // Cell text color

        // Apply styles
        recentTable.setStyle(tableStyle);

        // Style column headers
        for (TableColumn<?, ?> column : recentTable.getColumns()) {
            column.setStyle(headerStyle);
        }

        // Apply cell styling (this requires a CSS file)
        // Create a file called styles.css in your resources folder with the content from the CSS artifact below
        Scene scene = recentTable.getScene();
        if (scene != null) {
            scene.getStylesheets().add(getClass().getResource("/com/styles.css").toExternalForm());
        } else {
            // If the table is not yet in a scene, we need to wait
            recentTable.sceneProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    newValue.getStylesheets().add(getClass().getResource("/com/styles.css").toExternalForm());
                }
            });
        }
    }
    private void openTransactionWindow(String transactionType) {
        try {
            FXMLLoader loader;
            Stage transactionStage = new Stage();

            if (transactionType.equalsIgnoreCase("Income")) {
                loader = new FXMLLoader(getClass().getResource("/com/income-transaction.fxml"));
            } else {
                loader = new FXMLLoader(getClass().getResource("/com/expense-transaction.fxml"));
            }

            Scene scene = new Scene(loader.load());
            transactionStage.setTitle(transactionType + " Transaction");
            transactionStage.setScene(scene);

            // Set modality to block input to other windows
            transactionStage.initModality(Modality.APPLICATION_MODAL);

            // When transaction window is closed, refresh table and balance
            transactionStage.setOnHidden(e -> {
                loadRecentTransactions();
                updateBalanceLabel();
            });

            transactionStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
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



    public void updateBalanceLabel() {
        double currentBalance = SQLiteDatabase.getCurrentBalance();
        balance.setText(String.format("%.2f €", currentBalance));
    }

    // In HomeController.java, modify the loadRecentTransactions method:
    public void loadRecentTransactions() {
        List<Transaction> recentTransactions = SQLiteDatabase.getRecentTransactions(5);
        ObservableList<Transaction> observableTransactions = FXCollections.observableArrayList(recentTransactions);
        recentTable.setItems(observableTransactions);
    }
    @FXML
    private void handleHistoryButtonClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/History-view.fxml"));
            Scene historyScene = new Scene(loader.load());
            Stage stage = (Stage) HistoryButton.getScene().getWindow();
            stage.setScene(historyScene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}