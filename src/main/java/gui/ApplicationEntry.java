package gui;

public class ApplicationEntry {

    private String companyName;
    private String roleTitle;
    private String status;
    private String deadline;

    public ApplicationEntry(String companyName, String roleTitle,
                            String status, String deadline) {
        this.companyName = companyName;
        this.roleTitle   = roleTitle;
        this.status      = status;
        this.deadline    = deadline;
    }

    public String getCompanyName() { return companyName; }
    public void   setCompanyName(String v) { this.companyName = v; }

    public String getRoleTitle()   { return roleTitle; }
    public void   setRoleTitle(String v)   { this.roleTitle = v; }

    public String getStatus()      { return status; }
    public void   setStatus(String v)      { this.status = v; }

    public String getDeadline()    { return deadline; }
    public void   setDeadline(String v)    { this.deadline = v; }
}