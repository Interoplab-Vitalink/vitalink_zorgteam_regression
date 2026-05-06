# Vitalink Zorgteam Regression

Automated regression test suite for the Zorgteam Backend API.

The suite is built with:

- Java 21
- Maven
- REST Assured
- Cucumber
- JUnit 4

It validates the main CareTeam API flow:

- query existing care teams for a patient
- create or reuse a care team
- read a care team
- add citizen, professional, and organization members
- remove those members again

## Repository

Expected GitHub repository:

```text
https://github.com/Interoplab-Vitalink/vitalink_zorgteam_regression
```

SSH remote:

```text
git@github.com:Interoplab-Vitalink/vitalink_zorgteam_regression.git
```

Push local changes:

```bash
cd /Users/gwenbleyen/Documents/Workspace/java_projects/Vitalink_Zorgteam_Regression
git push -u origin main
```

## Project Layout

```text
.
|-- Jenkinsfile
|-- Dockerfile
|-- globals.properties
|-- pom.xml
`-- src/test
    |-- java
    |   |-- TestRunner.java
    |   |-- helpers
    |   `-- step_definitions
    `-- resources
        |-- CareTeam.feature
        `-- requestBodies
```

## Runtime Configuration

The tests need these values at runtime.

| Name | Required | Description |
| --- | --- | --- |
| `VO_ZORGTEAM_API_TOKEN` | Yes | Bearer token for the Zorgteam API. Must be a complete JWT and normally starts with `eyJ`. |
| `PATIENT1_PSEUDO` | Yes | Pseudonymized patient SSIN payload. |
| `ZORGTEAM_BASE_URL` | No | Overrides the Zorgteam API base URL. |
| `TARGET_ENV` | No | Environment name passed with `-Denv`. Defaults to `ACC` in Jenkins. |
| `CUCUMBER_TAGS` | No | Optional Cucumber tag expression, for example `@smoke`. |

Default ACC base URL from `globals.properties`:

```text
https://apps-acpt.zorgteam-services.be/api
```

Do not commit real tokens or patient values to `globals.properties`.

## Run Locally

Prerequisites:

- Java 21+
- Maven 3.8+

Check versions:

```bash
java -version
mvn -version
```

Run the full suite:

```bash
VO_ZORGTEAM_API_TOKEN="<your-token>" \
PATIENT1_PSEUDO="<your-patient-pseudo>" \
mvn -B -Denv=ACC -Dtest=TestRunner clean test
```

Run only smoke scenarios:

```bash
VO_ZORGTEAM_API_TOKEN="<your-token>" \
PATIENT1_PSEUDO="<your-patient-pseudo>" \
mvn -B -Denv=ACC -Dtest=TestRunner -Dcucumber.filter.tags="@smoke" clean test
```

Override the base URL:

```bash
ZORGTEAM_BASE_URL="https://your-url/api" \
VO_ZORGTEAM_API_TOKEN="<your-token>" \
PATIENT1_PSEUDO="<your-patient-pseudo>" \
mvn -B -Denv=ACC -Dtest=TestRunner clean test
```

## Cucumber Tags

Available tags:

| Tag | Scope |
| --- | --- |
| `@careteam` | All care team scenarios. |
| `@smoke` | Overview, create/reuse, and read scenarios. |

The full feature file is:

```text
src/test/resources/CareTeam.feature
```

## Reports

After a local or Jenkins run, reports are generated under `target/`.

Important files:

```text
target/cucumber-reports/index.html
target/cucumber-reports/cucumber.json
target/cucumber-reports/cucumber.xml
target/surefire-reports/
```

Jenkins archives:

```text
target/cucumber-reports/**
target/surefire-reports/**
```

## Jenkins Pipeline

The Jenkins pipeline is defined in:

```text
Jenkinsfile
```

The pipeline runs on an agent with this label:

```text
synology-jenkins-agent-zorgteam
```

Important: Jenkins schedules jobs by label. If the node is named `synology-jenkins-agent-zorgteam`, also add the same value in the node's `Labels` field.

### Jenkins Agent Requirements

The Jenkins agent must have:

- Java 21
- Maven
- Git

Optional, only if `PUBLISH_AGENT_IMAGE=true`:

- Docker

### Jenkins Plugins

Required plugins:

- Pipeline
- Git
- JUnit
- Cucumber Reports
- Credentials Binding

Optional for Docker image publishing:

- Docker Pipeline

## Create The Jenkins Job

1. Open Jenkins.
2. Click `New Item`.
3. Enter job name:

   ```text
   vitalink_zorgteam_regression
   ```

4. Select `Pipeline`.
5. Click `OK`.
6. Scroll to the `Pipeline` section.
7. Set `Definition` to:

   ```text
   Pipeline script from SCM
   ```

8. Set `SCM` to:

   ```text
   Git
   ```

9. Set `Repository URL` to either HTTPS:

   ```text
   https://github.com/Interoplab-Vitalink/vitalink_zorgteam_regression.git
   ```

   or SSH:

   ```text
   git@github.com:Interoplab-Vitalink/vitalink_zorgteam_regression.git
   ```

10. Select GitHub credentials that can read the repository.
11. Set `Branch Specifier` to:

    ```text
    */main
    ```

12. Set `Script Path` to:

    ```text
    Jenkinsfile
    ```

13. Save.

## Jenkins Parameters

The current pipeline expects direct free-entry parameters, not Jenkins credential IDs.

| Parameter | Required | Example |
| --- | --- | --- |
| `TARGET_ENV` | No | `ACC` |
| `CUCUMBER_TAGS` | No | `@smoke` |
| `VO_ZORGTEAM_API_TOKEN` | Yes | Full JWT starting with `eyJ...` |
| `PATIENT1_PSEUDO` | Yes | Full patient pseudo value |
| `ZORGTEAM_BASE_URL` | No | Leave empty to use ACC default |
| `PUBLISH_AGENT_IMAGE` | No | `false` |

Typical manual run values:

```text
TARGET_ENV=ACC
CUCUMBER_TAGS=
VO_ZORGTEAM_API_TOKEN=<your-token>
PATIENT1_PSEUDO=<your-patient-pseudo>
ZORGTEAM_BASE_URL=
PUBLISH_AGENT_IMAGE=false
```

The pipeline executes:

```bash
mvn -B -Denv=ACC -Dtest=TestRunner clean test
```

If `CUCUMBER_TAGS` is set, the pipeline also passes:

```bash
-Dcucumber.filter.tags="<tag-expression>"
```

## Trigger Jenkins Remotely

Jenkins URL:

```text
http://shiftleftconsulting.hopto.org:8081/
```

Job URL:

```text
http://shiftleftconsulting.hopto.org:8081/job/vitalink_zorgteam_regression/
```

Recommended: use a Jenkins API token instead of a password.

Set environment variables locally:

```bash
export JENKINS_URL="http://shiftleftconsulting.hopto.org:8081"
export JENKINS_USER="<jenkins-user>"
export JENKINS_API_TOKEN="<jenkins-api-token>"
export JOB_NAME="vitalink_zorgteam_regression"
export VO_ZORGTEAM_API_TOKEN="<your-token>"
export PATIENT1_PSEUDO="<your-patient-pseudo>"
```

Fetch a Jenkins crumb:

```bash
CRUMB_JSON=$(curl -sS \
  --user "$JENKINS_USER:$JENKINS_API_TOKEN" \
  "$JENKINS_URL/crumbIssuer/api/json")

CRUMB_FIELD=$(printf '%s' "$CRUMB_JSON" | sed -n 's/.*"crumbRequestField":"\([^"]*\)".*/\1/p')
CRUMB=$(printf '%s' "$CRUMB_JSON" | sed -n 's/.*"crumb":"\([^"]*\)".*/\1/p')
```

Trigger the job:

```bash
curl -i -X POST \
  --user "$JENKINS_USER:$JENKINS_API_TOKEN" \
  -H "$CRUMB_FIELD: $CRUMB" \
  "$JENKINS_URL/job/$JOB_NAME/buildWithParameters" \
  --data-urlencode "TARGET_ENV=ACC" \
  --data-urlencode "CUCUMBER_TAGS=" \
  --data-urlencode "VO_ZORGTEAM_API_TOKEN=$VO_ZORGTEAM_API_TOKEN" \
  --data-urlencode "PATIENT1_PSEUDO=$PATIENT1_PSEUDO" \
  --data-urlencode "ZORGTEAM_BASE_URL=" \
  --data-urlencode "PUBLISH_AGENT_IMAGE=false"
```

Expected successful trigger response:

```text
HTTP/1.1 201 Created
Location: http://shiftleftconsulting.hopto.org:8081/queue/item/<queue-id>/
```

Check queue item:

```bash
curl -sS \
  --user "$JENKINS_USER:$JENKINS_API_TOKEN" \
  "$JENKINS_URL/queue/item/<queue-id>/api/json"
```

Check build console:

```bash
curl -sS \
  --user "$JENKINS_USER:$JENKINS_API_TOKEN" \
  "$JENKINS_URL/job/$JOB_NAME/<build-number>/consoleText"
```

Check build result:

```bash
curl -sS \
  --user "$JENKINS_USER:$JENKINS_API_TOKEN" \
  "$JENKINS_URL/job/$JOB_NAME/<build-number>/api/json?tree=building,result,duration"
```

## Troubleshooting

### Jenkins returns `403 No valid crumb was included in the request`

Fetch a crumb from:

```text
/crumbIssuer/api/json
```

Then pass the returned crumb request field and crumb as an HTTP header.

### Jenkins job stays queued

Check that the Jenkins node is online and has this label:

```text
synology-jenkins-agent-zorgteam
```

The node name alone is not always enough. The label field must contain the value used by the `Jenkinsfile`.

### All API tests fail with `401 invalid_token`

Check the `VO_ZORGTEAM_API_TOKEN` parameter.

The token must be the complete JWT. A valid JWT normally starts with:

```text
eyJ
```

If the first character is missing, the API returns:

```text
401 invalid_token
Malformed token
```

### Token expires during or before the run

JWTs and patient pseudo values can be time-limited. Generate fresh values before starting a Jenkins build.

You can decode the JWT payload locally to inspect the expiry:

```bash
python3 - <<'PY'
import base64, datetime as dt, json, os
token = os.environ["VO_ZORGTEAM_API_TOKEN"]
payload = token.split(".")[1]
payload += "=" * (-len(payload) % 4)
data = json.loads(base64.urlsafe_b64decode(payload))
print(dt.datetime.fromtimestamp(data["exp"], dt.UTC).isoformat())
PY
```

### Local run passes but Jenkins fails

Compare these values between local and Jenkins:

- `VO_ZORGTEAM_API_TOKEN`
- `PATIENT1_PSEUDO`
- `ZORGTEAM_BASE_URL`
- `TARGET_ENV`
- Java version
- Maven version
- checked-out git commit

The Jenkins console prints the checked-out commit near the top of the log.

### Cucumber report fails after successful tests

Verify that this file exists:

```text
target/cucumber-reports/cucumber.json
```

The Jenkinsfile publishes reports from the `target` directory using this pattern:

```text
**/cucumber.json
```

## Security Notes

- Do not commit real API tokens or patient values.
- Prefer Jenkins API tokens over account passwords for remote triggers.
- Rotate credentials if they are pasted into chat, logs, tickets, or documentation.
- Avoid printing full tokens in Jenkins console logs.
- Use Jenkins credential masking or secret parameters if these values need stronger protection later.

## Docker Agent Image

The `Jenkinsfile` contains an optional stage to build and push a Jenkins agent image.

It is controlled by:

```text
PUBLISH_AGENT_IMAGE
```

Default:

```text
false
```

Image name:

```text
interoplabvitalink/vitalink-zorgteam-jenkins-agent
```

If enabled, Jenkins needs:

- Docker available on the agent
- Docker Pipeline plugin
- Docker Hub credential ID:

  ```text
  dockerhub-interoplabvitalink
  ```

## Current Known Good Jenkins Run

A remote Jenkins run with direct parameters completed successfully:

```text
Build #6
Result: SUCCESS
Tests run: 9
Failures: 0
Errors: 0
Skipped: 0
```
