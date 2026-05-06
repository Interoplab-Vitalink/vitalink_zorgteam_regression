@careteam
Feature: Manage CareTeam Resources via Zorgteam Backend API

  @smoke
  Scenario: Overview - Query existing care teams for a patient
    Given the Zorgteam API is available
    Then the overview response status code should be 200
    And the overview response should be a list

  @smoke
  Scenario: Create a new care team or reuse existing
    When the user creates a new care team or reuses an existing one
    Then the care team should be available
    And the response should contain a care team id
    And the response should contain care team properties

  @smoke
  Scenario: Read a care team
    When the user reads the care team
    Then the read response status code should be 200
    And the response should contain a care team id
    And the response should contain care team properties

  Scenario: Add a citizen member to the care team
    When the user adds a citizen member to the care team
    Then the add citizen response status code should be 200
    And the response should contain a care team id
    And the response should contain care team properties
    And the response should have 1 member(s)

  Scenario: Add a professional member to the care team
    When the user adds a professional member to the care team
    Then the add professional response status code should be 200
    And the response should contain a care team id
    And the response should contain care team properties
    And the response should have 2 member(s)

  Scenario: Add an organization member to the care team
    When the user adds an organization member to the care team
    Then the add organization response status code should be 200
    And the response should contain a care team id
    And the response should contain care team properties
    And the response should have 3 member(s)

  Scenario: Remove the citizen member from the care team
    When the user removes the citizen member from the care team
    Then the remove citizen response status code should be 200
    And the response should contain a care team id
    And the response should contain care team properties
    And the response should have 2 member(s)

  Scenario: Remove the professional member from the care team
    When the user removes the professional member from the care team
    Then the remove professional response status code should be 200
    And the response should contain a care team id
    And the response should contain care team properties
    And the response should have 1 member(s)

  Scenario: Remove the organization member from the care team
    When the user removes the organization member from the care team
    Then the remove organization response status code should be 200
    And the response should contain a care team id
    And the response should contain care team properties
    And the response should have 0 member(s)
