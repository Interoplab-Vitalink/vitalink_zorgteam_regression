# Vitalink Zorgteam Regression

Automated regression test suite for the Zorgteam Backend API, built with Cucumber, REST Assured, and JUnit 4.

## Prerequisites

- Java 21+
- Maven 3.8+

## Running tests locally

### Required environment variables

| Variable | Description |
|----------|-------------|
| `VO_ZORGTEAM_API_TOKEN` | Bearer token for the Zorgteam API (JWT) |
| `PATIENT1_PSEUDO` | Pseudonymized patient SSIN |

### Run all tests

```bash
VO_ZORGTEAM_API_TOKEN="<your-token>" \
PATIENT1_PSEUDO="<your-pseudo>" \
mvn -Dtest=TestRunner clean test
```

### Run a subset using Cucumber tags

```bash
VO_ZORGTEAM_API_TOKEN="<your-token>" \
PATIENT1_PSEUDO="<your-pseudo>" \
mvn -Dtest=TestRunner -Dcucumber.filter.tags="@smoke" clean test
```

Available tags:
- `@careteam` — all care team scenarios
- `@smoke` — overview, create, and read only

### Override the base URL

By default tests target the ACC environment (`https://apps-acpt.zorgteam-services.be/api`). To override:

```bash
ZORGTEAM_BASE_URL="https://your-custom-url/api" \
VO_ZORGTEAM_API_TOKEN="<your-token>" \
PATIENT1_PSEUDO="<your-pseudo>" \
mvn -Dtest=TestRunner clean test
```

### View reports

After a test run, reports are available at:
- **Cucumber HTML:** `target/cucumber-reports/index.html`
- **Surefire XML:** `target/surefire-reports/`

## Jenkins

The pipeline is configured via `Jenkinsfile`. Provide the following Jenkins credential IDs as build parameters:

- `VO_ZORGTEAM_API_TOKEN_CREDENTIAL_ID`
- `PATIENT1_PSEUDO_CREDENTIAL_ID`
- `ZORGTEAM_BASE_URL_CREDENTIAL_ID` (optional)
