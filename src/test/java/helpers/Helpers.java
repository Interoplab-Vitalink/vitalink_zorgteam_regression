package helpers;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Helpers {

    private Response response;
    private static final ThreadLocal<String> currentScenarioName = new ThreadLocal<>();
    private static final ThreadLocal<String> currentScenarioSuite = new ThreadLocal<>();
    private static final ThreadLocal<String> currentStepName = new ThreadLocal<>();

    public static String getJsonPath(Response response, String key) {
        String complete = response.asString();
        JsonPath js = new JsonPath(complete);
        return js.get(key).toString();
    }
    public static String responseToString(Response response)
    {
        String string = response.getBody().asString();
        return string;
    }

    public static void setCurrentScenarioName(String name) {
        currentScenarioName.set(name);
    }

    public static void setCurrentScenarioSuite(String suite) {
        currentScenarioSuite.set(suite);
    }

    public static void setCurrentStepName(String step) {
        currentStepName.set(step);
    }

    private static String getCurrentScenarioName() {
        String name = currentScenarioName.get();
        return (name == null || name.isBlank()) ? "Unknown scenario" : name;
    }

    private static String getCurrentScenarioSuite() {
        String suite = currentScenarioSuite.get();
        return (suite == null || suite.isBlank()) ? "Unknown suite" : suite;
    }

    private static String getCurrentStepName() {
        String step = currentStepName.get();
        return (step == null || step.isBlank()) ? "Unknown step" : step;
    }

    private static String getScenarioFailureLabel() {
        return getCurrentScenarioName() + " (suite: " + getCurrentScenarioSuite() + ", step: " + getCurrentStepName() + ")";
    }

    private static String failureLabel(String assertion) {
        return getScenarioFailureLabel() + " | assertion: " + assertion;
    }

    public static void printResponseDetails(Response response) {
        if (response == null) {
            System.out.println("Response is null");
            return;
        }
        try {
            System.out.println("Status: " + response.getStatusLine());
        } catch (Exception ignored) {
            System.out.println("Status: <unavailable>");
        }
        try {
            System.out.println("Headers:");
            response.getHeaders().forEach(h -> System.out.println(h.getName() + ": " + h.getValue()));
        } catch (Exception ignored) {
            System.out.println("Headers: <unavailable>");
        }
        try {
            System.out.println("Body:");
            System.out.println(response.getBody().asString());
        } catch (Exception ignored) {
            System.out.println("Body: <unavailable>");
        }
    }

    public static String buildResponseDetails(Response response) {
        if (response == null) {
            return "Response is null";
        }
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("Status: ").append(response.getStatusLine()).append('\n');
        } catch (Exception ignored) {
            sb.append("Status: <unavailable>\n");
        }
        try {
            sb.append("Headers:\n");
            response.getHeaders().forEach(h -> sb.append(h.getName()).append(": ").append(h.getValue()).append('\n'));
        } catch (Exception ignored) {
            sb.append("Headers: <unavailable>\n");
        }
        try {
            sb.append("Body:\n").append(response.getBody().asString()).append('\n');
        } catch (Exception ignored) {
            sb.append("Body: <unavailable>\n");
        }
        return sb.toString();
    }

    public static void assertStatus(Response response, int expectedStatusCode) {
        int actualStatusCode = response.getStatusCode();
        if (actualStatusCode != expectedStatusCode) {
            throw new AssertionError(failureLabel("status == " + expectedStatusCode + " (actual " + actualStatusCode + ")")
                    + "\n" + buildResponseDetails(response));
        }
    }

    public static void assertStatus4xx(Response response) {
        int actualStatusCode = response.getStatusCode();
        boolean is4xx = actualStatusCode >= 400 && actualStatusCode < 500;
        if (!is4xx) {
            throw new AssertionError(failureLabel("status in 4xx (actual " + actualStatusCode + ")"));
        }
    }

    public static void assertEqualsWithDetails(Object expected, Object actual, Response response) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(failureLabel("equals expected=" + expected + " actual=" + actual));
        }
    }

    public static void assertTrueWithDetails(String message, boolean condition, Response response) {
        if (!condition) {
            throw new AssertionError(failureLabel(message));
        }
    }

    public static void assertHasProperty(Response response, String property) {
        Object value = response.getBody().jsonPath().get(property);
        if (value == null) {
            throw new AssertionError(failureLabel("response should have property '" + property + "'")
                    + "\n" + buildResponseDetails(response));
        }
    }

    public static void assertNotHasProperty(Response response, String property) {
        Object value = response.getBody().jsonPath().get(property);
        if (value != null) {
            throw new AssertionError(failureLabel("response should NOT have property '" + property + "'")
                    + "\n" + buildResponseDetails(response));
        }
    }

    public static String getTimeStamp() {
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneOffset.UTC);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return currentTime.format(formatter) + "+00:00";
    }
}
