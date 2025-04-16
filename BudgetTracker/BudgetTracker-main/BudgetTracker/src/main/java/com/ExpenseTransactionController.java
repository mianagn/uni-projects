package com;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class ExpenseTransactionController implements Initializable {

    @FXML private ComboBox<String> expenseCategoryComboBox;
    @FXML private TextField amountTextField;
    @FXML private DatePicker datePicker;
    @FXML private Button submitButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ObservableList<String> expenseCategories = FXCollections.observableArrayList(
                "Transport", "Rent", "Entertainment", "Utilities", "Healthcare",
                "Groceries", "Clothing", "Gas", "Taxes", "Miscellaneous"
        );
        datePicker.setValue(LocalDate.now());
        expenseCategoryComboBox.setItems(expenseCategories);
    }

    @FXML
    private void handleSubmit() {
        if (!validateInputs()) {
            return;
        }

        try {
            String category = expenseCategoryComboBox.getValue();
            double amountValue = Double.parseDouble(amountTextField.getText());
            String date = datePicker.getValue().toString();

            // Insert expense transaction (false for expense)
            boolean success = SQLiteDatabase.insertTransaction(category, amountValue, date, false);

            if (success) {
                // Close the window after submission
                Stage stage = (Stage) submitButton.getScene().getWindow();
                stage.close();
            } else {
                showAlert(AlertType.ERROR, "Error", "Failed to add expense transaction.");
            }
        } catch (NumberFormatException e) {
            showAlert(AlertType.ERROR, "Invalid Input", "Please enter a valid number for amount.");
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Error", "An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean validateInputs() {
        StringBuilder errorMessage = new StringBuilder();

        if (expenseCategoryComboBox.getValue() == null) {
            errorMessage.append("Please select a category.\n");
        }

        if (amountTextField.getText().isEmpty()) {
            errorMessage.append("Please enter an amount.\n");
        } else {
            try {
                double amount = Double.parseDouble(amountTextField.getText());
                if (amount <= 0) {
                    errorMessage.append("Amount must be greater than zero.\n");
                }
            } catch (NumberFormatException e) {
                errorMessage.append("Please enter a valid number for amount.\n");
            }
        }

        if (datePicker.getValue() == null) {
            errorMessage.append("Please select a date.\n");
        }

        if (errorMessage.length() > 0) {
            showAlert(AlertType.WARNING, "Validation Error", errorMessage.toString());
            return false;
        }

        return true;
    }

    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

