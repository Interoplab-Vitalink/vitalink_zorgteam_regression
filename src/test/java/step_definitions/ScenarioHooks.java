package step_definitions;

import helpers.Helpers;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

public class ScenarioHooks {
    @Before
    public void captureScenarioName(Scenario scenario) {
        Helpers.setCurrentScenarioName(scenario.getName());
        try {
            Helpers.setCurrentScenarioSuite(String.valueOf(scenario.getUri()));
        } catch (Exception ignored) {
            Helpers.setCurrentScenarioSuite("Unknown suite");
        }
    }
}
