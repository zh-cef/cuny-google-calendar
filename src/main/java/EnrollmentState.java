import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EnrollmentState {
    private List<CourseEntry> cnfs;

    public List<CourseEntry> getCnfs() {
        return cnfs;
    }

    public void setCnfs(List<CourseEntry> cnfs) {
        this.cnfs = cnfs;
    }
}
