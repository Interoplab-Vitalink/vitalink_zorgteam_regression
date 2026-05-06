package step_definitions;

import helpers.ApiRequestLogger;
import io.cucumber.java.Before;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class StepDefinitions_General {

    private static final String DEFAULT_ENV = "ACC";
    private static final String GLOBALS_PROPERTIES_PATH = "globals.properties";

    private static Properties properties;
    private static String baseUrl;
    private static String apiToken;
    private static String patient1Pseudo;
    private static String env = "";

    @Before(order = 0)
    public void setEnvironment() throws IOException {
        RestAssured.reset();

        env = resolvePropertyValue("env", "ZORGTEAM_ENV", "properties.env", DEFAULT_ENV);
        System.out.println("Running tests for environment " + env);

        ensurePropertiesLoaded();

        apiToken = resolvePropertyValue("properties.vo-zorgteam-api-token", "VO_ZORGTEAM_API_TOKEN", "properties.vo-zorgteam-api-token", "");
        patient1Pseudo = resolvePropertyValue("properties.patient1_pseudo", "PATIENT1_PSEUDO", "properties.patient1_pseudo", "");

        System.out.println("Patient1 pseudo configured: " + (!patient1Pseudo.isEmpty()));

        setBaseURI(env);

        // Register API request/response logger so details appear in Surefire reports
        if (RestAssured.filters() == null
                || RestAssured.filters().stream().noneMatch(f -> f instanceof ApiRequestLogger)) {
            RestAssured.filters(new ApiRequestLogger());
        }

        // Set global default headers for all requests
        RestAssured.requestSpecification = new RequestSpecBuilder()
                .addHeader("Cache-Control", "no-cache")
                .addHeader("Intervault-EndUser-HcPartyType", "PERSPHYSICIAN")
                .build();
    }

    public static String getBaseUrl() {
        return baseUrl;
    }

    public static String getApiToken() {
        return apiToken;
    }

    public static String getPatient1Pseudo() {
        return patient1Pseudo;
    }

    public static String getEnv() {
        return env;
    }

    private void setBaseURI(String env) {
        ensurePropertiesLoaded();
        baseUrl = properties.getProperty("properties.baseURL.ACC");
        RestAssured.baseURI = baseUrl;
    }

    private static synchronized void ensurePropertiesLoaded() {
        if (properties != null) {
            return;
        }

        File src = new File(GLOBALS_PROPERTIES_PATH);
        Properties loadedProperties = new Properties();
        if (src.isFile()) {
            try (FileInputStream fis = new FileInputStream(src)) {
                loadedProperties.load(fis);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to load " + GLOBALS_PROPERTIES_PATH, e);
            }
        }

        properties = loadedProperties;
        applyRuntimeOverrides();
    }

    private static void applyRuntimeOverrides() {
        overrideProperty("properties.baseURL.ACC", "ZORGTEAM_BASE_URL");
        overrideProperty("properties.vo-zorgteam-api-token", "VO_ZORGTEAM_API_TOKEN");
        overrideProperty("properties.patient1_pseudo", "PATIENT1_PSEUDO");
        overrideProperty("properties.env", "ZORGTEAM_ENV");
    }

    private static void overrideProperty(String propertyKey, String envKey) {
        String override = System.getenv(envKey);
        if (override == null || override.isBlank()) {
            override = System.getProperty(propertyKey);
        }

        if (override != null && !override.isBlank()) {
            properties.setProperty(propertyKey, override);
        }
    }

    private static String resolvePropertyValue(String systemPropertyKey, String envKey, String propertyKey, String defaultValue) {
        String value = System.getenv(envKey);
        if (value == null || value.isBlank()) {
            value = System.getProperty(systemPropertyKey);
        }
        if ((value == null || value.isBlank()) && propertyKey != null && properties != null) {
            value = properties.getProperty(propertyKey);
        }
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
