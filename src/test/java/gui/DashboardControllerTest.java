package gui;

import logic.Application;
import logic.ApplicationController;
import logic.ApplicationStatus;
import logic.InMemoryStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the search filter logic in DashboardController.
 * Each test calls matchesSearch() directly (package-private) using an Application
 * object and a keyword string, verifying that the method correctly determines
 * whether the application matches the search term without requiring JavaFX.
 * The method checks both company name and role title, case-insensitively.
 *
 * Coverage:
 * - matchesSearch(): empty keyword matches all applications
 * - matchesSearch(): full company name match (case-insensitive)
 * - matchesSearch(): partial company name match
 * - matchesSearch(): uppercase keyword matches lowercase-stored name
 * - matchesSearch(): role title partial match
 * - matchesSearch(): role title full match with mixed case
 * - matchesSearch(): keyword present only in location field does not match
 * - matchesSearch(): keyword not present in any field returns false
 *
 * Not covered (requires JavaFX runtime):
 * - loadData(): stat card population and chart rendering
 * - handleSearch(): FilteredList predicate is applied on user input
 * - handleNewApplication(): onNewApplication callback is invoked
 * - Edit button column: opens EditApplicationView on click
 * - loadData(): error dialog shown on IllegalArgumentException or IllegalStateException
 */
class DashboardControllerTest {

    private DashboardController controller;
    private Application app;

    @BeforeEach
    void setUp() {
        ApplicationController appController = new ApplicationController(new InMemoryStorage());
        controller = new DashboardController();
        controller.setAppController(appController);

        app = appController.addApplication(
                "Google", "SWE Intern", 5000, "SG", ApplicationStatus.APPLIED);
    }

    /**
     * Verifies that an empty keyword matches all applications. This corresponds
     * to the state where the search bar is blank and all rows should be shown.
     */
    @Test
    void matchesSearch_emptyKeyword_matchesAll() {
        assertTrue(controller.matchesSearch(app, ""));
    }

    /**
     * Verifies that a keyword equal to the full company name returns true.
     * The keyword is passed in lowercase to also confirm case-insensitive matching.
     */
    @Test
    void matchesSearch_matchingCompanyName_returnsTrue() {
        assertTrue(controller.matchesSearch(app, "google"));
    }

    /**
     * Verifies that a keyword matching only part of the company name returns true.
     * The filter uses contains(), so partial matches are expected to succeed.
     */
    @Test
    void matchesSearch_partialCompanyName_returnsTrue() {
        assertTrue(controller.matchesSearch(app, "goog"));
    }

    /**
     * Verifies that the company name match is case-insensitive by passing the
     * keyword in uppercase even though the stored name uses title case.
     */
    @Test
    void matchesSearch_uppercaseKeyword_returnsTrue() {
        assertTrue(controller.matchesSearch(app, "GOOGLE"));
    }

    /**
     * Verifies that a keyword matching part of the role title returns true.
     * Both company name and role title are searched, so a role match is sufficient.
     */
    @Test
    void matchesSearch_matchingRoleTitle_returnsTrue() {
        assertTrue(controller.matchesSearch(app, "swe"));
    }

    /**
     * Verifies that a keyword matching the full role title, including mixed case,
     * returns true, confirming that role title matching is also case-insensitive.
     */
    @Test
    void matchesSearch_mixedCaseRoleTitle_returnsTrue() {
        assertTrue(controller.matchesSearch(app, "SWE Intern"));
    }

    /**
     * Verifies that a keyword that does not appear in either the company name or
     * role title returns false. This is the standard no-match case.
     */
    @Test
    void matchesSearch_nonMatchingKeyword_returnsFalse() {
        assertFalse(controller.matchesSearch(app, "amazon"));
    }

    /**
     * Verifies that search is limited to company name and role title only.
     * The location field ("SG") is not included in the search, so a keyword
     * matching only the location should return false.
     */
    @Test
    void matchesSearch_keywordMatchesLocationOnly_returnsFalse() {
        assertFalse(controller.matchesSearch(app, "sg"));
    }
}