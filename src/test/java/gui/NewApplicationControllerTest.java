package gui;

import logic.ApplicationController;
import logic.InMemoryStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the input validation logic in NewApplicationController.
 * Each test calls validateInput() directly (package-private) using plain strings,
 * which avoids the need to set up JavaFX text fields or the full form view.
 * A null return value means the input is valid; a non-null string is the error message.
 */
class NewApplicationControllerTest {

    private NewApplicationController controller;

    @BeforeEach
    void setUp() {
        controller = new NewApplicationController();
        controller.setAppController(new ApplicationController(new InMemoryStorage()));
    }

    /**
     * Verifies that providing a non-empty company name, role title, and a valid
     * numeric pay string passes validation with no error returned.
     */
    @Test
    void validateInput_validInput_returnsNull() {
        assertNull(controller.validateInput("Google", "SWE Intern", "5000"));
    }

    /**
     * Verifies that leaving the pay field empty is accepted, since pay is an
     * optional field — the application can be saved without a salary value.
     */
    @Test
    void validateInput_emptyPay_accepted() {
        assertNull(controller.validateInput("Google", "SWE Intern", ""));
    }

    /**
     * Verifies that a pay value of zero is accepted as a valid numeric input.
     * Zero pay is a legitimate value (e.g., unpaid internship).
     */
    @Test
    void validateInput_zeroPay_accepted() {
        assertNull(controller.validateInput("Google", "SWE Intern", "0"));
    }

    /**
     * Verifies that an empty company name is rejected and an error message is returned.
     * Company name is a required field and must not be blank.
     */
    @Test
    void validateInput_emptyCompany_returnsError() {
        assertNotNull(controller.validateInput("", "SWE Intern", "5000"));
    }

    /**
     * Verifies that an empty role title is rejected and an error message is returned.
     * Role title is a required field and must not be blank.
     */
    @Test
    void validateInput_emptyRole_returnsError() {
        assertNotNull(controller.validateInput("Google", "", "5000"));
    }

    /**
     * Verifies that both company name and role title being empty is rejected.
     * Ensures the combined required-fields check handles the all-empty case.
     */
    @Test
    void validateInput_emptyCompanyAndRole_returnsError() {
        assertNotNull(controller.validateInput("", "", "5000"));
    }

    /**
     * Verifies that a non-numeric string in the pay field is rejected and an
     * error message is returned. The pay field only accepts parseable doubles.
     */
    @Test
    void validateInput_nonNumericPay_returnsError() {
        assertNotNull(controller.validateInput("Google", "SWE Intern", "abc"));
    }

    /**
     * Verifies that a mixed alphanumeric pay string (e.g. "50abc") is rejected.
     * Double.parseDouble() will throw on this input, triggering the error path.
     */
    @Test
    void validateInput_mixedCharacterPay_returnsError() {
        assertNotNull(controller.validateInput("Google", "SWE Intern", "50abc"));
    }
}
