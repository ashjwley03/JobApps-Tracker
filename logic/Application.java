package logic;

/**
 * Represents a job/internship application.
 * Logic layer (Yugam) owns the full definition — this is a minimal stub for Storage to use.
 */
public class Application {
    private String id;
    private String company;
    private String status;
    private String salary;
    private String location;

    public Application(String id, String company, String status, String salary, String location) {
        this.id = id;
        this.company = company;
        this.status = status;
        this.salary = salary;
        this.location = location;
    }

    public String getId() { return id; }
    public String getCompany() { return company; }
    public String getStatus() { return status; }
    public String getSalary() { return salary; }
    public String getLocation() { return location; }

    public void setId(String id) { this.id = id; }
    public void setCompany(String company) { this.company = company; }
    public void setStatus(String status) { this.status = status; }
    public void setSalary(String salary) { this.salary = salary; }
    public void setLocation(String location) { this.location = location; }
}
