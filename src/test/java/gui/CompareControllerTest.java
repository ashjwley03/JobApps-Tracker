package gui;

import logic.Application;
import logic.ApplicationController;
import logic.ApplicationStatus;
import logic.InMemoryStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the comparison and selection logic in CompareController.
 * Each test calls getComparedApplications() directly (package-private) with a
 * list of application IDs, verifying that the returned list is correctly sorted
 * by pay descending and handles edge cases such as an empty selection.
 */
class CompareControllerTest {

    private CompareController controller;
    private ApplicationController appController;

    @BeforeEach
    void setUp() {
        appController = new ApplicationController(new InMemoryStorage());
        controller = new CompareController();
        controller.setAppController(appController);
    }

    /**
     * Verifies that passing an empty ID list returns an empty result rather than
     * throwing an exception. This covers the case where no checkboxes are selected.
     */
    @Test
    void getComparedApplications_emptyIds_returnsEmptyList() {
        assertTrue(controller.getComparedApplications(List.of()).isEmpty());
    }

    /**
     * Verifies that a single selected application is returned correctly in a
     * one-element list, with no sorting issues caused by a trivial comparison.
     */
    @Test
    void getComparedApplications_singleId_returnsSingleApplication() {
        Application app = appController.addApplication(
                "Shopee", "Backend Intern", 4500, "SG", ApplicationStatus.APPLIED);

        List<Application> result = controller.getComparedApplications(List.of(app.getId()));

        assertEquals(1, result.size());
        assertEquals("Shopee", result.get(0).getCompanyName());
    }

    /**
     * Verifies that when two applications are selected, the one with the higher
     * pay is placed first in the returned list, and the lower-pay one second.
     */
    @Test
    void getComparedApplications_twoIds_sortedByPayDescending() {
        Application low = appController.addApplication(
                "StartupA", "Intern", 2000, "SG", ApplicationStatus.APPLIED);
        Application high = appController.addApplication(
                "BigTech", "Intern", 6000, "SG", ApplicationStatus.OFFER);

        List<Application> result = controller.getComparedApplications(
                List.of(low.getId(), high.getId()));

        assertEquals(2, result.size());
        assertEquals("BigTech", result.get(0).getCompanyName()); // highest pay first
        assertEquals("StartupA", result.get(1).getCompanyName());
    }

    /**
     * Verifies that the sort order is determined by pay value, not by the order
     * the IDs are passed in. IDs are passed in an order that does not match the
     * expected output to confirm sorting is applied correctly.
     */
    @Test
    void getComparedApplications_highestPayAlwaysFirst_regardlessOfInputOrder() {
        Application a = appController.addApplication("A", "Intern", 3000, "SG", ApplicationStatus.APPLIED);
        Application b = appController.addApplication("B", "Intern", 5000, "SG", ApplicationStatus.APPLIED);
        Application c = appController.addApplication("C", "Intern", 4000, "SG", ApplicationStatus.APPLIED);

        List<Application> result = controller.getComparedApplications(
                List.of(c.getId(), a.getId(), b.getId()));

        // B has the highest pay and must always be first
        assertEquals("B", result.get(0).getCompanyName());
        assertEquals(5000, result.get(0).getPay(), 0.001);
    }
}
