package logic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for ApplicationController.
 * Verifies business logic, input validation, and status flow enforcement.
 */
class ApplicationControllerTest {

    private ApplicationController controller;

    /**
     * Initializes the controller with an in-memory storage stub before each test.
     */
    @BeforeEach
    void setUp() {
        controller = new ApplicationController(new InMemoryStorage());
    }

    /**
     * Verifies that providing valid application details successfully creates and stores the application.
     */
    @Test
    void addApplication_validInput_returnsApplication() {
        Application app = controller.addApplication(
                "Google", "SWE Intern", 5000, "Singapore", ApplicationStatus.APPLIED);
        assertNotNull(app);
        assertEquals("Google", app.getCompanyName());
        assertEquals(ApplicationStatus.APPLIED, app.getStatus());
    }

    /**
     * Verifies that attempting to create an application with an empty company name throws an exception.
     */
    @Test
    void addApplication_emptyCompanyName_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            controller.addApplication("", "SWE Intern", 5000, "Singapore", ApplicationStatus.APPLIED);
        });
    }

    /**
     * Verifies that attempting to create an application with a null role title throws an exception.
     */
    @Test
    void addApplication_nullRoleTitle_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            controller.addApplication("Google", null, 5000, "Singapore", ApplicationStatus.APPLIED);
        });
    }

    /**
     * Verifies that loading applications from an empty storage returns an empty list.
     */
    @Test
    void getAllApplications_emptyList_returnsEmptyList() {
        assertTrue(controller.getAllApplications().isEmpty());
    }

    /**
     * Verifies that all saved applications are successfully retrieved from storage.
     */
    @Test
    void getAllApplications_afterAdding_returnsAll() {
        controller.addApplication("Meta", "PM Intern", 4000, "Remote", ApplicationStatus.APPLIED);
        controller.addApplication("Grab", "Data Intern", 3500, "Singapore", ApplicationStatus.APPLIED);
        assertEquals(2, controller.getAllApplications().size());
    }

    /**
     * Verifies that a valid status transition (e.g., APPLIED to INTERVIEWING) updates the application correctly.
     */
    @Test
    void updateStatus_validTransition_updatesCorrectly() {
        Application app = controller.addApplication(
                "Shopee", "Backend Intern", 4500, "Singapore", ApplicationStatus.APPLIED);
        Application updated = controller.updateStatus(app.getId(), ApplicationStatus.INTERVIEWING);
        assertEquals(ApplicationStatus.INTERVIEWING, updated.getStatus());
    }

    // --- NEW EXCEPTION TESTS FOR STATUS FLOW ---

    /**
     * Verifies the business rule that a REJECTED application is in a terminal state and cannot be revived.
     */
    @Test
    void updateStatus_rejectedToOffer_throwsException() {
        Application app = controller.addApplication(
                "DeadCo", "Role", 1000, "SG", ApplicationStatus.REJECTED);

        assertThrows(IllegalStateException.class, () -> {
            controller.updateStatus(app.getId(), ApplicationStatus.OFFER);
        }, "Should throw exception when reviving a REJECTED application");
    }

    /**
     * Verifies the business rule that an application must pass through
     * the INTERVIEWING stage before receiving an OFFER.
     */
    @Test
    void updateStatus_appliedToOffer_throwsException() {
        Application app = controller.addApplication(
                "FastCo", "Role", 2000, "SG", ApplicationStatus.APPLIED);

        assertThrows(IllegalStateException.class, () -> {
            controller.updateStatus(app.getId(), ApplicationStatus.OFFER);
        }, "Should throw exception when jumping straight to OFFER without an interview");
    }

    // -------------------------------------------

    /**
     * Verifies that attempting to update the status of a non-existent application throws an exception.
     */
    @Test
    void updateStatus_invalidId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            controller.updateStatus("nonexistent-id", ApplicationStatus.OFFER);
        });
    }

    /**
     * Verifies that deleting a valid application removes it completely from storage.
     */
    @Test
    void deleteApplication_existingId_removesFromList() {
        Application app = controller.addApplication(
                "ByteDance", "iOS Intern", 5000, "Singapore", ApplicationStatus.APPLIED);
        controller.deleteApplication(app.getId());
        assertTrue(controller.getAllApplications().isEmpty());
    }

    /**
     * Verifies that attempting to delete an application with an invalid ID throws an exception.
     */
    @Test
    void deleteApplication_invalidId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            controller.deleteApplication("fake-id");
        });
    }

    /**
     * Verifies that the compareApplications method sorts the specified applications by pay in descending order.
     */
    @Test
    void compareApplications_sortedByPayDescending() {
        Application low = controller.addApplication(
                "StartupA", "Intern", 2000, "Singapore", ApplicationStatus.APPLIED);
        Application high = controller.addApplication(
                "BigTech", "Intern", 6000, "Singapore", ApplicationStatus.OFFER);
        List<Application> result = controller.compareApplications(
                List.of(low.getId(), high.getId()));

        assertEquals("BigTech", result.get(0).getCompanyName());
        assertEquals("StartupA", result.get(1).getCompanyName());
    }

    // --- updateDetails tests ---

    /**
     * Verifies that updateDetails correctly updates all four core fields
     * (company, role, pay, location) on an existing application.
     */
    @Test
    void updateDetails_validInput_updatesAllFields() {
        Application app = controller.addApplication(
                "OldCo", "OldRole", 1000, "OldCity", ApplicationStatus.APPLIED);

        Application updated = controller.updateDetails(
                app.getId(), "NewCo", "NewRole", 9999, "NewCity");

        assertEquals("NewCo", updated.getCompanyName());
        assertEquals("NewRole", updated.getRoleTitle());
        assertEquals(9999, updated.getPay(), 0.001);
        assertEquals("NewCity", updated.getLocation());
    }

    /**
     * Verifies that updateDetails throws an exception when given a blank company name.
     */
    @Test
    void updateDetails_blankCompany_throwsException() {
        Application app = controller.addApplication(
                "TestCo", "Role", 1000, "SG", ApplicationStatus.APPLIED);

        assertThrows(IllegalArgumentException.class, () -> {
            controller.updateDetails(app.getId(), "", "Role", 1000, "SG");
        });
    }

    /**
     * Verifies that updateDetails throws an exception when given a null role title.
     */
    @Test
    void updateDetails_nullRole_throwsException() {
        Application app = controller.addApplication(
                "TestCo", "Role", 1000, "SG", ApplicationStatus.APPLIED);

        assertThrows(IllegalArgumentException.class, () -> {
            controller.updateDetails(app.getId(), "TestCo", null, 1000, "SG");
        });
    }

    /**
     * Verifies that updateDetails throws an exception when given a non-existent ID.
     */
    @Test
    void updateDetails_invalidId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            controller.updateDetails("fake-id", "Co", "Role", 1000, "SG");
        });
    }

    // --- updateDeadline tests ---

    /**
     * Verifies that updateDeadline sets a new deadline on an existing application.
     */
    @Test
    void updateDeadline_validDate_updatesDeadline() {
        Application app = controller.addApplication(
                "Google", "SWE", 5000, "SG", ApplicationStatus.APPLIED);
        LocalDate deadline = LocalDate.now().plusDays(30);

        Application updated = controller.updateDeadline(app.getId(), deadline);

        assertEquals(deadline, updated.getDeadline());
    }

    /**
     * Verifies that passing null clears the deadline on an application.
     */
    @Test
    void updateDeadline_nullDate_clearsDeadline() {
        Application app = controller.addApplication(
                "Google", "SWE", 5000, "SG", ApplicationStatus.APPLIED);
        controller.updateDeadline(app.getId(), LocalDate.now().plusDays(10));

        Application updated = controller.updateDeadline(app.getId(), null);

        assertNull(updated.getDeadline());
    }

    // --- updateNotes tests ---

    /**
     * Verifies that updateNotes saves new notes text on an existing application.
     */
    @Test
    void updateNotes_validText_updatesNotes() {
        Application app = controller.addApplication(
                "Meta", "PM", 4000, "Remote", ApplicationStatus.APPLIED);

        Application updated = controller.updateNotes(app.getId(), "Referred by a friend");

        assertEquals("Referred by a friend", updated.getNotes());
    }

    /**
     * Verifies that passing null to updateNotes stores an empty string, not null.
     */
    @Test
    void updateNotes_nullText_storesEmptyString() {
        Application app = controller.addApplication(
                "Meta", "PM", 4000, "Remote", ApplicationStatus.APPLIED);

        Application updated = controller.updateNotes(app.getId(), null);

        assertEquals("", updated.getNotes());
    }

    // --- filterByStatus tests ---

    /**
     * Verifies that filterByStatus returns only applications matching the given status.
     */
    @Test
    void filterByStatus_mixedStatuses_returnsOnlyMatching() {
        controller.addApplication(
                "A", "Role", 1000, "SG", ApplicationStatus.APPLIED);
        controller.addApplication(
                "B", "Role", 2000, "SG", ApplicationStatus.INTERVIEWING);
        controller.addApplication(
                "C", "Role", 3000, "SG", ApplicationStatus.APPLIED);

        List<Application> applied = controller.filterByStatus(ApplicationStatus.APPLIED);

        assertEquals(2, applied.size());
        assertTrue(applied.stream().allMatch(
                a -> a.getStatus() == ApplicationStatus.APPLIED));
    }

    /**
     * Verifies that filterByStatus returns an empty list when no applications match.
     */
    @Test
    void filterByStatus_noMatches_returnsEmptyList() {
        controller.addApplication(
                "A", "Role", 1000, "SG", ApplicationStatus.APPLIED);

        List<Application> offers = controller.filterByStatus(ApplicationStatus.OFFER);

        assertTrue(offers.isEmpty());
    }
}