import com.fasterxml.jackson.databind.ObjectMapper;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

public class Main {
    private final static boolean SKIP_FETCH = true;
    public static void main(String[] args)
            throws IOException, ParserConfigurationException, SAXException, GeneralSecurityException {

        if (!SKIP_FETCH) {
            if (!FetchData.fetch()) {
                System.out.println("Fetch Failed");
                return;
            }
        }

        Path xmlPath = Path.of("class-data.xml");
        Path jsonPath = Path.of("enrollment.json");

        if ((!Files.exists(xmlPath)) || (!Files.exists(jsonPath))) {
            System.out.println("Didn't find required files. Run fetch again.");
            return;
        }

        String xml = Files.readString(xmlPath, StandardCharsets.UTF_8);
        String json = Files.readString(jsonPath, StandardCharsets.UTF_8);

        if (!xml.contains("<classdata")) {
            System.out.println("class-data.xml has not <classdata");
            return;
        }
        if (!json.trim().startsWith("{")) {
            System.out.println("enrollment.json is not JSON");
            return;
        }

        //Using the ObjectMapper from Jackson api to read from JSON files
        ObjectMapper mapper = new ObjectMapper();
        EnrollmentState enrollmentState = mapper.readValue(json, EnrollmentState.class);

        Map<String, String> enrollMap = new HashMap<>();
        for (CourseEntry courseEntry : enrollmentState.getCnfs()) {
            enrollMap.put(courseEntry.getCnKey(), courseEntry.getEnr());
        }

        //Using DocumentBuilder to parse xml into DOM
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        doc.getDocumentElement().normalize();

        GoogleCalendarParser.insert(ScheduleParser.parse(doc, enrollMap));
    }
}
