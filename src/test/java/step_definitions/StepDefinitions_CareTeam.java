package step_definitions;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static helpers.Helpers.*;

public class StepDefinitions_CareTeam {

    private Response response;

    private static String careTeamId;
    private static boolean careTeamReused;
    private static String member1Id; // citizen
    private static String member2Id; // professional
    private static String member3Id; // organization

    // --- Overview ---

    @Given("the Zorgteam API is available")
    public void theZorgteamAPIIsAvailable() {
        response = RestAssured.given()
                .header("Authorization", "Bearer " + StepDefinitions_General.getApiToken())
                .queryParam("patientSSIN", StepDefinitions_General.getPatient1Pseudo())
                .queryParam("type", "ALIVIA")
                .get("/careteams");
    }

    @Then("the overview response status code should be {int}")
    public void theOverviewResponseStatusCodeShouldBe(int expectedStatusCode) {
        assertStatus(response, expectedStatusCode);
    }

    @And("the overview response should be a list")
    public void theOverviewResponseShouldBeAList() {
        List<?> list = response.getBody().jsonPath().getList("$");
        assertTrueWithDetails("response should be an array", list != null, response);
    }

    // --- Create or reuse ---

    @When("the user creates a new care team or reuses an existing one")
    public void theUserCreatesANewCareTeamOrReusesExisting() throws IOException {
        String requestBody = loadPayload("requestBodies/CareTeamCreate.json");
        requestBody = requestBody.replace("{{patient1_pseudo}}", StepDefinitions_General.getPatient1Pseudo());

        response = RestAssured.given()
                .header("Authorization", "Bearer " + StepDefinitions_General.getApiToken())
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/careteams");

        if (response.getStatusCode() == 201) {
            careTeamId = response.getBody().jsonPath().getString("id");
            careTeamReused = false;
            System.out.println("Created care team with ID: " + careTeamId);
        } else if (response.getStatusCode() == 409) {
            System.out.println("Care team already exists (409 Conflict), fetching existing one...");
            Response overviewResponse = RestAssured.given()
                    .header("Authorization", "Bearer " + StepDefinitions_General.getApiToken())
                    .queryParam("patientSSIN", StepDefinitions_General.getPatient1Pseudo())
                    .queryParam("type", "ALIVIA")
                    .get("/careteams");

            List<Map<String, Object>> careTeams = overviewResponse.getBody().jsonPath().getList("$");
            assertTrueWithDetails("expected at least one existing care team after 409", careTeams != null && !careTeams.isEmpty(), overviewResponse);

            careTeamId = (String) careTeams.get(0).get("id");
            careTeamReused = true;
            System.out.println("Reusing existing care team with ID: " + careTeamId);

            // Clean up existing members so subsequent add/remove scenarios start fresh
            removeAllExistingMembers();

            // Re-read the care team so the response reflects the clean state
            response = RestAssured.given()
                    .header("Authorization", "Bearer " + StepDefinitions_General.getApiToken())
                    .header("patientSSIN", StepDefinitions_General.getPatient1Pseudo())
                    .get("/careteams/" + careTeamId);
        }
    }

    @Then("the care team should be available")
    public void theCareTeamShouldBeAvailable() {
        if (careTeamReused) {
            assertStatus(response, 200);
        } else {
            assertStatus(response, 201);
        }
    }

    @And("the response should contain a care team id")
    public void theResponseShouldContainACareTeamId() {
        assertHasProperty(response, "id");
    }

    @And("the response should contain care team properties")
    public void theResponseShouldContainCareTeamProperties() {
        assertHasProperty(response, "patientSsin");
        assertHasProperty(response, "type");
        assertHasProperty(response, "status");
        assertHasProperty(response, "period");
        assertHasProperty(response, "period.start");
    }

    private void removeAllExistingMembers() {
        Response readResponse = RestAssured.given()
                .header("Authorization", "Bearer " + StepDefinitions_General.getApiToken())
                .header("patientSSIN", StepDefinitions_General.getPatient1Pseudo())
                .get("/careteams/" + careTeamId);

        List<Map<String, Object>> existingMembers = readResponse.getBody().jsonPath().getList("members");
        if (existingMembers == null || existingMembers.isEmpty()) {
            System.out.println("No existing members to clean up.");
            return;
        }

        System.out.println("Removing " + existingMembers.size() + " existing member(s)...");
        for (Map<String, Object> member : existingMembers) {
            String memberId = (String) member.get("id");
            RestAssured.given()
                    .header("Authorization", "Bearer " + StepDefinitions_General.getApiToken())
                    .header("patientSSIN", StepDefinitions_General.getPatient1Pseudo())
                    .delete("/careteams/" + careTeamId + "/members/" + memberId);
            System.out.println("  Removed member: " + memberId);
        }
    }

    // --- Read ---

    @When("the user reads the care team")
    public void theUserReadsTheCareTeam() {
        response = RestAssured.given()
                .header("Authorization", "Bearer " + StepDefinitions_General.getApiToken())
                .header("patientSSIN", StepDefinitions_General.getPatient1Pseudo())
                .get("/careteams/" + careTeamId);
    }

    @Then("the read response status code should be {int}")
    public void theReadResponseStatusCodeShouldBe(int expectedStatusCode) {
        assertStatus(response, expectedStatusCode);
    }

    // --- Add citizen ---

    @When("the user adds a citizen member to the care team")
    public void theUserAddsACitizenMemberToTheCareTeam() throws IOException {
        String requestBody = loadPayload("requestBodies/AddCitizen.json");
        requestBody = requestBody.replace("{{patient1_pseudo}}", StepDefinitions_General.getPatient1Pseudo());

        response = RestAssured.given()
                .header("Authorization", "Bearer " + StepDefinitions_General.getApiToken())
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/careteams/" + careTeamId + "/members");

        if (response.getStatusCode() == 200) {
            List<Map<String, Object>> members = response.getBody().jsonPath().getList("members");
            for (Map<String, Object> member : members) {
                Map<String, Object> actor = (Map<String, Object>) member.get("actor");
                if ("Citizen".equals(actor.get("hcpType"))) {
                    member1Id = (String) member.get("id");
                    break;
                }
            }
            System.out.println("Added citizen member with ID: " + member1Id);
        }
    }

    @Then("the add citizen response status code should be {int}")
    public void theAddCitizenResponseStatusCodeShouldBe(int expectedStatusCode) {
        assertStatus(response, expectedStatusCode);
    }

    @And("the response should have {int} member\\(s)")
    public void theResponseShouldHaveMembers(int expectedCount) {
        List<?> members = response.getBody().jsonPath().getList("members");
        assertTrueWithDetails(
                "expected " + expectedCount + " members but got " + (members == null ? 0 : members.size()),
                members != null && members.size() == expectedCount,
                response
        );
    }

    // --- Add professional ---

    @When("the user adds a professional member to the care team")
    public void theUserAddsAProfessionalMemberToTheCareTeam() throws IOException {
        String requestBody = loadPayload("requestBodies/AddProfessional.json");
        requestBody = requestBody.replace("{{patient1_pseudo}}", StepDefinitions_General.getPatient1Pseudo());

        response = RestAssured.given()
                .header("Authorization", "Bearer " + StepDefinitions_General.getApiToken())
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/careteams/" + careTeamId + "/members");

        if (response.getStatusCode() == 200) {
            List<Map<String, Object>> members = response.getBody().jsonPath().getList("members");
            for (Map<String, Object> member : members) {
                Map<String, Object> actor = (Map<String, Object>) member.get("actor");
                if ("Professional".equals(actor.get("hcpType"))) {
                    member2Id = (String) member.get("id");
                    break;
                }
            }
            System.out.println("Added professional member with ID: " + member2Id);
        }
    }

    @Then("the add professional response status code should be {int}")
    public void theAddProfessionalResponseStatusCodeShouldBe(int expectedStatusCode) {
        assertStatus(response, expectedStatusCode);
    }

    // --- Add organization ---

    @When("the user adds an organization member to the care team")
    public void theUserAddsAnOrganizationMemberToTheCareTeam() throws IOException {
        String requestBody = loadPayload("requestBodies/AddOrganization.json");
        requestBody = requestBody.replace("{{patient1_pseudo}}", StepDefinitions_General.getPatient1Pseudo());

        response = RestAssured.given()
                .header("Authorization", "Bearer " + StepDefinitions_General.getApiToken())
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post("/careteams/" + careTeamId + "/members");

        if (response.getStatusCode() == 200) {
            List<Map<String, Object>> members = response.getBody().jsonPath().getList("members");
            for (Map<String, Object> member : members) {
                Map<String, Object> actor = (Map<String, Object>) member.get("actor");
                if ("Organization".equals(actor.get("hcpType"))) {
                    member3Id = (String) member.get("id");
                    break;
                }
            }
            System.out.println("Added organization member with ID: " + member3Id);
        }
    }

    @Then("the add organization response status code should be {int}")
    public void theAddOrganizationResponseStatusCodeShouldBe(int expectedStatusCode) {
        assertStatus(response, expectedStatusCode);
    }

    // --- Remove citizen ---

    @When("the user removes the citizen member from the care team")
    public void theUserRemovesTheCitizenMemberFromTheCareTeam() {
        response = RestAssured.given()
                .header("Authorization", "Bearer " + StepDefinitions_General.getApiToken())
                .header("patientSSIN", StepDefinitions_General.getPatient1Pseudo())
                .delete("/careteams/" + careTeamId + "/members/" + member1Id);
    }

    @Then("the remove citizen response status code should be {int}")
    public void theRemoveCitizenResponseStatusCodeShouldBe(int expectedStatusCode) {
        assertStatus(response, expectedStatusCode);
    }

    // --- Remove professional ---

    @When("the user removes the professional member from the care team")
    public void theUserRemovesTheProfessionalMemberFromTheCareTeam() {
        response = RestAssured.given()
                .header("Authorization", "Bearer " + StepDefinitions_General.getApiToken())
                .header("patientSSIN", StepDefinitions_General.getPatient1Pseudo())
                .delete("/careteams/" + careTeamId + "/members/" + member2Id);
    }

    @Then("the remove professional response status code should be {int}")
    public void theRemoveProfessionalResponseStatusCodeShouldBe(int expectedStatusCode) {
        assertStatus(response, expectedStatusCode);
    }

    // --- Remove organization ---

    @When("the user removes the organization member from the care team")
    public void theUserRemovesTheOrganizationMemberFromTheCareTeam() {
        response = RestAssured.given()
                .header("Authorization", "Bearer " + StepDefinitions_General.getApiToken())
                .header("patientSSIN", StepDefinitions_General.getPatient1Pseudo())
                .delete("/careteams/" + careTeamId + "/members/" + member3Id);
    }

    @Then("the remove organization response status code should be {int}")
    public void theRemoveOrganizationResponseStatusCodeShouldBe(int expectedStatusCode) {
        assertStatus(response, expectedStatusCode);
    }

    // --- Helpers ---

    private String loadPayload(String payloadFile) throws IOException {
        String jsonFilePath = "src/test/resources/" + payloadFile;
        return new String(Files.readAllBytes(Paths.get(jsonFilePath)));
    }
}
