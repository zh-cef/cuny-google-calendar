import com.microsoft.playwright.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

public class FetchData {
    /**
     * Capture the class-data.xml and enrollment.json response from CUNYfirst Schedule Builder
     * Or the timeout expires
     * @return class-data.xml and enrollment.json. return true if success and false if timeout
     * @throws IOException
     */
    public static boolean fetch() throws IOException {
        try (Playwright pw = Playwright.create()) {
            BrowserContext ctx = pw.chromium().launchPersistentContext(
                    Path.of("browser-profile"),
                    new BrowserType.LaunchPersistentContextOptions()
                            .setChannel("chrome")
                            .setHeadless(false));

            AtomicReference<Response> classData = new AtomicReference<>();
            AtomicReference<Response> enrollmentState = new AtomicReference<>();

            //Only remember the response
            ctx.onResponse(response -> {
                String url = response.url();
                if (url.contains("/api/class-data")) classData.set(response);
                else if (url.contains("/api/getEnrollmentState")) enrollmentState.set(response);
            });

            Page page = ctx.pages().isEmpty() ? ctx.newPage() : ctx.pages().getFirst();
            page.navigate("https://sb.cunyfirst.cuny.edu/");
            System.out.println("Login in to CUNYfirst");

            String xml = null, json = null;
            long deadline = System.currentTimeMillis() + 10 * 60_000;
            while (System.currentTimeMillis() < deadline && (xml == null || json == null)) {
                page.waitForTimeout(1000);

                //Read the response
                Response cd = classData.getAndSet(null);
                if (cd != null) {
                    try {
                        String body = cd.text();
                        if (body.contains("<classdata")) xml = body;
                        else System.out.println("Something went wrong with class data: "
                                + body.substring(0, Math.min(150, body.length())));
                    } catch (PlaywrightException e) {}
                }

                Response en = enrollmentState.getAndSet(null);
                if (en != null ) {
                    try {
                        String body = en.text();
                        if (body.trim().startsWith("{")) json = body;
                    } catch (PlaywrightException e) {}
                }
            }

            ctx.close();

            if (xml == null || json == null) {
                System.out.println("Timeout: class data: " + (xml != null) + " enrollment state: " + (json != null));
                return false;
            }

            Files.writeString(Path.of("class-data.xml"), xml);
            Files.writeString(Path.of("enrollment.json"), json);
            return true;
        }
    }
}
