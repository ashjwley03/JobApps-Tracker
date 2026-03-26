package gui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import logic.ApplicationController;
import logic.ApplicationStatus;
import storage.FileStorage;

/**
 * Controls the new application form view.
 * Validates input and delegates to ApplicationController to persist the new entry.
 */
public class NewApplicationController {

    @FXML private TextField companyField;
    @FXML private TextField roleField;
    @FXML private ChoiceBox<ApplicationStatus> statusChoice;
    @FXML private TextField payField;
    @FXML private TextField locationField;
    @FXML private Label errorLabel;

    private final ApplicationController appController =
            new ApplicationController(new FileStorage());

    /** Called by MainController after load — navigates back to dashboard on save or cancel. */
    private Runnable onSuccess;

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    /**
     * Initializes the form after the FXML has been loaded.
     * Populates the status dropdown and sets the default value.
     */
    @FXML
    public void initialize() {
        statusChoice.getItems().setAll(ApplicationStatus.values());
        statusChoice.setValue(ApplicationStatus.APPLIED);
        errorLabel.setText("");
    }

    /**
     * Validates required fields and saves the new application via ApplicationController.
     * Deadline and notes can be set later via an edit screen.
     */
    @FXML
    private void handleSubmit() {
        String company = companyField.getText().trim();
        String role    = roleField.getText().trim();
        ApplicationStatus status = statusChoice.getValue();

        if (company.isEmpty() || role.isEmpty() || status == null) {
            errorLabel.setText("Company, Role, and Status are required.");
            return;
        }

        double pay = 0;
        String payText = payField.getText().trim();
        if (!payText.isEmpty()) {
            try {
                pay = Double.parseDouble(payText);
            } catch (NumberFormatException e) {
                errorLabel.setText("Pay must be a valid number.");
                return;
            }
        }

        String location = locationField.getText().trim();

        appController.addApplication(company, role, pay, location, status);

        if (onSuccess != null) onSuccess.run();
    }
    @FXML
    private void handleCancel() {
        if (onSuccess != null) onSuccess.run();
    }
}