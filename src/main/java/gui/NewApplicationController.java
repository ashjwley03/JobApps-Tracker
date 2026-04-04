package gui;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import logic.ApplicationController;
import logic.ApplicationStatus;

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

    private ApplicationController appController;

    /** Callback invoked after a successful save or a cancel action to return to the previous view. */
    private Runnable onSuccess;

    /**
     * Sets the ApplicationController used to persist new applications.
     * Must be called by MainController before the view is displayed.
     *
     * @param appController The application controller to use.
     */
    public void setAppController(ApplicationController appController) {
        this.appController = appController;
    }

    /**
     * Registers a callback to invoke when the form is submitted successfully or cancelled.
     * Typically called by {@link MainController} immediately after loading this view.
     *
     * @param onSuccess Runnable to execute on success or cancellation.
     */
    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    /**
     * Initialises the form after the FXML has been loaded.
     * Populates the status dropdown and sets the default value.
     */
    @FXML
    public void initialize() {
        statusChoice.getItems().setAll(ApplicationStatus.values());
        statusChoice.setValue(ApplicationStatus.APPLIED);
        errorLabel.setText("");
    }

    /**
     * Validates the given form field values without requiring JavaFX controls.
     * Package-private to allow direct invocation from tests.
     *
     * @param company  Company name entered by the user.
     * @param role     Role title entered by the user.
     * @param payText  Pay value entered by the user, may be empty.
     * @return An error message string if validation fails, or null if all inputs are valid.
     */
    String validateInput(String company, String role, String payText) {
        if (company.isEmpty() || role.isEmpty()) {
            return "Company, Role, and Status are required.";
        }
        if (!payText.isEmpty()) {
            try {
                Double.parseDouble(payText);
            } catch (NumberFormatException e) {
                return "Pay must be a valid number.";
            }
        }
        return null;
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

        if (status == null) {
            errorLabel.setText("Company, Role, and Status are required.");
            return;
        }

        String error = validateInput(company, role, payField.getText().trim());
        if (error != null) {
            errorLabel.setText(error);
            return;
        }

        double pay = 0;
        String payText = payField.getText().trim();
        if (!payText.isEmpty()) {
            pay = Double.parseDouble(payText);
        }

        String location = locationField.getText().trim();
        appController.addApplication(company, role, pay, location, status);

        if (onSuccess != null) {
            onSuccess.run();
        }
    }

    @FXML
    private void handleCancel() {
        if (onSuccess != null) {
            onSuccess.run();
        }
    }
}