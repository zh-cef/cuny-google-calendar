import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseEntry {
    private String cnKey;
    private String enr;

    public String getCnKey() {
        return cnKey;
    }

    public void setCnKey(String cnKey) {
        this.cnKey = cnKey;
    }

    public String getEnr() {
        return enr;
    }

    public void setEnr(String enr) {
        this.enr = enr;
    }
}
