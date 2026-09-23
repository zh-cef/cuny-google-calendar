import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import java.io.*;
import java.security.GeneralSecurityException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

public class GoogleCalendarParser {
    private static final String APPLICATION_NAME = "CUNY Google Calendar";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final String CATEGORY_CALENDAR_NAME = "CUNY Classes";
    private static final ZoneId ZONE = ZoneId.of("America/New_York");

    /**
     * Direct the user to Google login page to get access to the user's Google calendar.
     * @param HTTP_TRANSPORT
     * @return credential of the user
     * @throws IOException
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        InputStream in = GoogleCalendarParser.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null)
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     * Either create a new calendar called CUNY Classes or return the existing CUNY Classes calendar ID
     * @param service
     * @return Google Calendar ID
     * @throws IOException
     */
    private static String getOrCreateCategoryCalendar(Calendar service) throws IOException {
        String pageToken = null;
        do {
            CalendarList list = service.calendarList().list()
                    .setPageToken(pageToken)
                    .execute();

            for (CalendarListEntry entry : list.getItems()) {
                if (CATEGORY_CALENDAR_NAME.equals(entry.getSummary())) return entry.getId();
            }

            pageToken = list.getNextPageToken();
        } while (pageToken != null);

        com.google.api.services.calendar.model.Calendar newCalendar =
                new com.google.api.services.calendar.model.Calendar()
                        .setSummary(CATEGORY_CALENDAR_NAME)
                        .setDescription("Course events imported from CUNYfirst")
                        .setTimeZone(ZONE.getId());

        return service.calendars().insert(newCalendar).execute().getId();
    }

    /**
     * Parse course events into Google calendar events and insert them into user's Google Calendar
     * @param courseEvents
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public static void insert(List<CourseEvent> courseEvents) throws GeneralSecurityException, IOException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        Calendar service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        String calendarId = getOrCreateCategoryCalendar(service);
        for (CourseEvent courseEvent : courseEvents) {
            ZonedDateTime startTime = ZonedDateTime.of(courseEvent.getDate(), courseEvent.getStartTime(), ZONE);
            ZonedDateTime endTime = ZonedDateTime.of(courseEvent.getDate(), courseEvent.getStartTime(), ZONE);

            //Unique id for each event to prevent duplicate
            String eventId = (courseEvent.getCnkey() + courseEvent.getDate() + courseEvent.getStartTime())
                    .replaceAll("[^a-zA-Z0-9]", "");

            //Creating calendar events from course events
            Event googleEvent = new Event()
                    .setSummary(courseEvent.getTitle())
                    .setLocation(courseEvent.getLocation())
                    .setDescription("Teacher: " + courseEvent.getTeacher())
                    .setStart(new EventDateTime()
                            .setDateTime(new DateTime(startTime.toInstant().toEpochMilli()))
                            .setTimeZone(ZONE.getId()))
                    .setEnd(new EventDateTime()
                            .setDateTime(new DateTime(endTime.toInstant().toEpochMilli()))
                            .setTimeZone(ZONE.getId()))
                    .setId(eventId);

            try {
                service.events().insert(calendarId, googleEvent).execute();
            } catch (GoogleJsonResponseException e) {
                if (e.getStatusCode() == 409)
                    System.out.println(courseEvent.getCnkey() + " already exists");
                else throw e;
            }
        }
    }
}
