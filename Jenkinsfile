pipeline {
    agent {
        label 'synology-jenkins-agent-zorgteam'
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
            name: 'VO_ZORGTEAM_API_TOKEN',
            defaultValue: '',
            description: 'vo-zorgteam-api-token value.'
        )
        string(
            name: 'PATIENT1_PSEUDO',
            defaultValue: '',
            description: 'patient1_pseudo value.'
        )
        string(
            name: 'ZORGTEAM_BASE_URL',
            defaultValue: '',
            description: 'Optional Zorgteam API base URL.'
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
                            parameterValue: params.VO_ZORGTEAM_API_TOKEN?.trim(),
                            propertyKey: 'properties.vo-zorgteam-api-token'
                        ],
                        'PATIENT1_PSEUDO': [
                            parameterValue: params.PATIENT1_PSEUDO?.trim(),
                            propertyKey: 'properties.patient1_pseudo'
                        ]
                    ]

                    // Optional vars: resolved if provided, but won't block the build
                    def optionalVars = [
                        'ZORGTEAM_BASE_URL': [
                            parameterValue: params.ZORGTEAM_BASE_URL?.trim(),
                            propertyKey: 'properties.baseURL.ACC'
                        ]
                    ]

                    def allVars = requiredVars + optionalVars

                    def readEnvValue = { variableName ->
                        sh(
                                script: "printenv ${variableName} || true",
                                returnStdout: true
                        ).trim()
                    }

                    allVars.each { varName, config ->
                        def resolvedValue = readEnvValue(varName)
                        if (!resolvedValue) {
                            resolvedValue = config.get('parameterValue')
                        }
                        if (!resolvedValue) {
                            resolvedValue = propertiesFallback.get(config.get('propertyKey'))?.trim()
                        }
                        if (resolvedValue) {
                            env.setProperty(varName, resolvedValue)
                        }
                    }

                    def missingVars = requiredVars.keySet().findAll { !readEnvValue(it) }
                    if (!missingVars.isEmpty()) {
                        error("Missing required runtime configuration. Set env vars, provide Jenkins parameters, or populate globals.properties for: ${missingVars.join(', ')}")
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
