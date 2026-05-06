pipeline {
    agent {
        label 'synology-docker'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    parameters {
        string(
            name: 'TARGET_ENV',
            defaultValue: 'ACC',
            description: 'Runtime environment passed to the suite with -Denv.'
        )
        string(
            name: 'CUCUMBER_TAGS',
            defaultValue: '',
            description: 'Optional Cucumber tag expression. Leave empty to use runner defaults.'
        )
        string(
            name: 'VO_ZORGTEAM_API_TOKEN_CREDENTIAL_ID',
            defaultValue: '',
            description: 'Jenkins Secret Text credential ID for vo-zorgteam-api-token.'
        )
        string(
            name: 'PATIENT1_PSEUDO_CREDENTIAL_ID',
            defaultValue: '',
            description: 'Jenkins Secret Text credential ID for patient1_pseudo.'
        )
        string(
            name: 'ZORGTEAM_BASE_URL_CREDENTIAL_ID',
            defaultValue: '',
            description: 'Optional Jenkins Secret Text credential ID for the Zorgteam API base URL.'
        )
        booleanParam(
            name: 'PUBLISH_AGENT_IMAGE',
            defaultValue: false,
            description: 'Build and push the Jenkins agent image to Docker Hub.'
        )
    }

    environment {
        DOCKER_IMAGE = 'interoplabvitalink/vitalink-zorgteam-jenkins-agent'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Validate Runtime Config') {
            steps {
                script {
                    def propertiesFallback = [:]
                    if (fileExists('globals.properties')) {
                        readFile('globals.properties')
                                .split('\n')
                                .each { line ->
                                    def trimmedLine = line.trim()
                                    if (!trimmedLine || trimmedLine.startsWith('#') || !trimmedLine.contains('=')) {
                                        return
                                    }

                                    def separatorIndex = trimmedLine.indexOf('=')
                                    def key = trimmedLine.substring(0, separatorIndex).trim()
                                    def value = trimmedLine.substring(separatorIndex + 1).trim()
                                    propertiesFallback.put(key, value)
                                }
                    }
                    def requiredVars = [
                        'VO_ZORGTEAM_API_TOKEN': [
                            credentialId: params.VO_ZORGTEAM_API_TOKEN_CREDENTIAL_ID?.trim(),
                            propertyKey: 'properties.vo-zorgteam-api-token'
                        ],
                        'PATIENT1_PSEUDO': [
                            credentialId: params.PATIENT1_PSEUDO_CREDENTIAL_ID?.trim(),
                            propertyKey: 'properties.patient1_pseudo'
                        ]
                    ]

                    // Optional vars: resolved if provided, but won't block the build
                    def optionalVars = [
                        'ZORGTEAM_BASE_URL': [
                            credentialId: params.ZORGTEAM_BASE_URL_CREDENTIAL_ID?.trim(),
                            propertyKey: 'properties.baseURL.ACC'
                        ]
                    ]

                    def allVars = requiredVars + optionalVars

                    def credentialBindings = []
                    def credentialVarNames = [:]
                    allVars.each { varName, config ->
                        def credentialId = config.get('credentialId')
                        if (credentialId) {
                            def credentialVarName = "${varName}_FROM_CREDENTIAL"
                            credentialVarNames.put(varName, credentialVarName)
                            credentialBindings << string(
                                    credentialsId: credentialId,
                                    variable: credentialVarName
                            )
                        }
                    }

                    def readEnvValue = { variableName ->
                        sh(
                                script: "printenv ${variableName} || true",
                                returnStdout: true
                        ).trim()
                    }

                    def resolveValues = {
                        allVars.each { varName, config ->
                            def resolvedValue = readEnvValue(varName)
                            def credentialVarName = credentialVarNames.get(varName)
                            if (!resolvedValue && credentialVarName) {
                                resolvedValue = readEnvValue(credentialVarName)
                            }
                            if (!resolvedValue) {
                                resolvedValue = propertiesFallback.get(config.get('propertyKey'))?.trim()
                            }
                            if (resolvedValue) {
                                env.setProperty(varName, resolvedValue)
                            }
                        }
                    }

                    if (credentialBindings) {
                        withCredentials(credentialBindings) {
                            resolveValues()
                        }
                    } else {
                        resolveValues()
                    }

                    def missingVars = requiredVars.keySet().findAll { !readEnvValue(it) }
                    if (!missingVars.isEmpty()) {
                        error("Missing required runtime configuration. Set env vars, provide credential IDs, or populate globals.properties for: ${missingVars.join(', ')}")
                    }
                }
            }
        }

        stage('Run Suite') {
            steps {
                script {
                    def command = "mvn -B -Denv=${params.TARGET_ENV} -Dtest=TestRunner"
                    if (params.CUCUMBER_TAGS?.trim()) {
                        def escapedTags = params.CUCUMBER_TAGS.replace("\"", "\\\"")
                        command += " -Dcucumber.filter.tags=\\\"${escapedTags}\\\""
                    }
                    command += " clean test"

                    sh command
                }
            }
        }

        stage('Build And Push Agent Image') {
            when {
                expression { params.PUBLISH_AGENT_IMAGE }
            }
            steps {
                script {
                    def image = docker.build("${env.DOCKER_IMAGE}:${env.BUILD_NUMBER}")
                    docker.withRegistry('https://index.docker.io/v1/', 'dockerhub-interoplabvitalink') {
                        image.push()
                        image.push('latest')
                    }
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml,target/cucumber-reports/*.xml'
            script {
                def hasCucumberReport = fileExists('target/cucumber-reports/cucumber.json')

                if (hasCucumberReport) {
                    cucumber(
                            buildStatus: 'UNSTABLE',
                            reportTitle: 'Zorgteam Cucumber Report',
                            jsonReportDirectory: 'target',
                            fileIncludePattern: '**/cucumber.json',
                            skipEmptyJSONFiles: true,
                            trendsLimit: 10,
                            classifications: [
                                    [key: 'Target Env', value: "${params.TARGET_ENV}"],
                                    [key: 'Tags', value: params.CUCUMBER_TAGS?.trim() ? params.CUCUMBER_TAGS : 'runner default']
                            ]
                    )
                }
            }
            archiveArtifacts allowEmptyArchive: true, artifacts: 'target/cucumber-reports/**,target/surefire-reports/**'
        }
    }
}
