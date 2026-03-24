package logic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationControllerTest {

    private ApplicationController controller;

    @BeforeEach
    void setUp() {
        controller = new ApplicationController(new InMemoryStorage());
    }

    @Test
    void addApplication_validInput_returnsApplication() {
        Application app = controller.addApplication(
                "Google", "SWE Intern", 5000, "Singapore", ApplicationStatus.APPLIED);
        assertNotNull(app);
        assertEquals("Google", app.getCompanyName());
        assertEquals(ApplicationStatus.APPLIED, app.getStatus());
    }

    @Test
    void addApplication_emptyCompanyName_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                controller.addApplication("", "SWE Intern", 5000, "Singapore", ApplicationStatus.APPLIED));
    }

    @Test
    void addApplication_nullRoleTitle_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                controller.addApplication("Google", null, 5000, "Singapore", ApplicationStatus.APPLIED));
    }

    @Test
    void getAllApplications_emptyList_returnsEmptyList() {
        assertTrue(controller.getAllApplications().isEmpty());
    }

    @Test
    void getAllApplications_afterAdding_returnsAll() {
        controller.addApplication("Meta", "PM Intern", 4000, "Remote", ApplicationStatus.APPLIED);
        controller.addApplication("Grab", "Data Intern", 3500, "Singapore", ApplicationStatus.APPLIED);
        assertEquals(2, controller.getAllApplications().size());
    }

    @Test
    void updateStatus_validTransition_updatesCorrectly() {
        Application app = controller.addApplication(
                "Shopee", "Backend Intern", 4500, "Singapore", ApplicationStatus.APPLIED);
        Application updated = controller.updateStatus(app.getId(), ApplicationStatus.INTERVIEWING);
        assertEquals(ApplicationStatus.INTERVIEWING, updated.getStatus());
    }

    @Test
    void updateStatus_invalidId_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                controller.updateStatus("nonexistent-id", ApplicationStatus.OFFER));
    }

    @Test
    void deleteApplication_existingId_removesFromList() {
        Application app = controller.addApplication(
                "ByteDance", "iOS Intern", 5000, "Singapore", ApplicationStatus.APPLIED);
        controller.deleteApplication(app.getId());
        assertTrue(controller.getAllApplications().isEmpty());
    }

    @Test
    void deleteApplication_invalidId_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                controller.deleteApplication("fake-id"));
    }

    @Test
    void compareApplications_sortedByPayDescending() {
        Application low = controller.addApplication(
                "StartupA", "Intern", 2000, "Singapore", ApplicationStatus.APPLIED);
        Application high = controller.addApplication(
                "BigTech", "Intern", 6000, "Singapore", ApplicationStatus.OFFER);
        List<Application> result = controller.compareApplications(List.of(low.getId(), high.getId()));
        assertEquals("BigTech", result.get(0).getCompanyName());
        assertEquals("StartupA", result.get(1).getCompanyName());
    }

    @Test
    void filterByStatus_returnsOnlyMatchingStatus() {
        controller.addApplication("A", "Intern", 3000, "SG", ApplicationStatus.APPLIED);
        controller.addApplication("B", "Intern", 4000, "SG", ApplicationStatus.OFFER);
        controller.addApplication("C", "Intern", 3500, "SG", ApplicationStatus.APPLIED);
        List<Application> applied = controller.filterByStatus(ApplicationStatus.APPLIED);
        assertEquals(2, applied.size());
        assertTrue(applied.stream().allMatch(a -> a.getStatus() == ApplicationStatus.APPLIED));
    }
}