import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class ScheduleParser {
    public static List<CourseEvent> parse(Document classData, Map<String, String> enrollMap) {
        List<CourseEvent> events = new ArrayList<>();

        NodeList courses = classData.getElementsByTagName("course");
        for (int i = 0; i < courses.getLength(); i++) {
            Element course = (Element) courses.item(i);
            String cnKey = course.getAttribute("key");
            String enr = enrollMap.get(cnKey);
            if (!enrollMap.containsKey(cnKey)) continue;

            //Get title
            String title = "";
            NodeList offerings = course.getElementsByTagName("offering");
            if (offerings.getLength() > 0)
                title = ((Element) offerings.item(0)).getAttribute("title");

            //Check if one of the selection matches the enrolled course
            Element selection = null;
            NodeList selections = course.getElementsByTagName("selection");
            for (int j = 0; j < selections.getLength(); j++) {
                Element s = (Element) selections.item(j);
                if (enr.equals(s.getAttribute("key"))) {
                    selection = s;
                    break;
                }
            }
            if (selection == null) {
                System.out.println("Can't find course: " + cnKey);
                continue;
            }

            //Get teach and location from the selection
            String teacher = "", location = "";
            NodeList blocks = selection.getElementsByTagName("block");
            if (blocks.getLength() > 0) {
                Element block = (Element) blocks.item(0);
                teacher = block.getAttribute("teacher");
                location = block.getAttribute("location");
            }

            //Move to the parent node
            Node parent = selection.getParentNode();
            if (!(parent instanceof Element) || !"uselection".equals(parent.getNodeName())) {
                System.out.println("Unexpected parent structure around section: " + cnKey + " " + enr);
                continue;
            }
            Element uselection = (Element) parent;

            //Use timeblock id as reference to find the timeblock element
            Map<String, Element> tbById = new LinkedHashMap<>();
            for (Node n = uselection.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n instanceof Element && "timeblock".equals(n.getNodeName()))
                    tbById.put(((Element) n).getAttribute("id"), (Element) n);
            }

            for (int j = 0; j < blocks.getLength(); j++) {
                Element block = (Element) blocks.item(j);
                String timeblockids = block.getAttribute("timeblockids");
                if (timeblockids.isEmpty()) continue;

                for (String id : timeblockids.split(",")) {
                    Element timeblock = tbById.get(id.trim());
                    if (timeblock == null) continue;

                    int day = parseIntHelper(timeblock, "day");
                    int t1 = parseIntHelper(timeblock, "t1");
                    int t2 = parseIntHelper(timeblock, "t2");
                    int d1 = parseIntHelper(timeblock, "d1");
                    int d2 = parseIntHelper(timeblock, "d2");

                    DayOfWeek currentDay = dayOfWeek(day);
                    LocalTime startTime = LocalTime.of(t1 / 60, t1 % 60);
                    LocalTime endTime = LocalTime.of(t2 / 60, t2 % 60);
                    LocalDate semesterStart = LocalDate.of(2008, 1, 1).plusDays(d1 - 1);
                    LocalDate semesterEnd = LocalDate.of(2008, 1, 1).plusDays(d2 - 1);

                    LocalDate current = semesterStart;
                    while (current.getDayOfWeek() != currentDay)
                        current = current.plusDays(1);

                    while (!current.isAfter(semesterEnd)) {
                        CourseEvent event = new CourseEvent();
                        event.setCnkey(cnKey);
                        event.setTitle(title);
                        event.setTeacher(teacher);
                        event.setLocation(location);
                        event.setDate(current);
                        event.setStartTime(startTime);
                        event.setEndTime(endTime);
                        events.add(event);

                        current = current.plusWeeks(1);
                    }
                }
            }
        }

        return events;
    }

    private static DayOfWeek dayOfWeek(int day) {
        switch (day) {
            case 1 : return DayOfWeek.SUNDAY;
            case 2: return DayOfWeek.MONDAY;
            case 3: return DayOfWeek.TUESDAY;
            case 4: return DayOfWeek.WEDNESDAY;
            case 5: return DayOfWeek.THURSDAY;
            case 6: return DayOfWeek.FRIDAY;
            case 7: return DayOfWeek.SATURDAY;
            default: throw new IllegalArgumentException("Unknown day: " + day);
        }
    }

    private static int parseIntHelper(Element timeblock, String name) {
        String value = timeblock.getAttribute(name);
        return value.isEmpty() ? -1 : Integer.parseInt(value);
    }
}
