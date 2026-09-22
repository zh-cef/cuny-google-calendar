import java.time.LocalDate;
import java.time.LocalTime;

public class CourseEvent {
    private String cnkey;
    private String title;
    private String teacher;
    private String location;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    public String getCourseKey() {
        return cnkey;
    }

    public void setCnkey(String cnkey) {
        this.cnkey = cnkey;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return "CourseEvent{" +
                "cnkey='" + cnkey + '\'' +
                ", title='" + title + '\'' +
                ", teacher='" + teacher + '\'' +
                ", location='" + location + '\'' +
                ", date=" + date +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
