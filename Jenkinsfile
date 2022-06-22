pipeline {
    agent any

    environment {
        JENKINS = 'true'
    }

    tools {
        jdk 'jdk-17'
    }

    options {
        skipDefaultCheckout(true) // we use git LFS so we cant use the default checkout
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        skipStagesAfterUnstable()
        buildDiscarder(logRotator(numToKeepStr: '30'))
    }

    stages {
        // This stage is required to do the checkout of the code as we need to use git LFS (this does require git-lfs to have been installed on the jenkins server)
        stage('Checkout') {
            steps {
//                // Clean workspace before build
//                cleanWs() // hopefully not needed
                script {
                    def gitRemoteOriginUrl = scm.getUserRemoteConfigs()[0].getUrl()
                    echo 'The remote URL is ' + gitRemoteOriginUrl
                    scmVars = checkout(
                        [$class           : 'GitSCM',
                         branches         : [[name: 'refs/heads/$BRANCH_NAME']],
                         extensions       : [[$class: 'GitLFSPull'], [$class: 'LocalBranch', localBranch: '**']],
                         gitTool          : 'git',
                         userRemoteConfigs : [[credentialsId: "58b31599-1344-416d-8c14-66886fad260e", url: gitRemoteOriginUrl]]
                        ])
                }
            }
        }


        stage('Clean') {
            // Only clean when the last build failed
            when {
                expression {
                    currentBuild.previousBuild?.currentResult == 'FAILURE'
                }
            }
            steps {
                sh "./gradlew clean"
            }
        }

        stage('Info') {
            steps {
                sh './gradlew -v' // Output gradle version for verification checks
                sh './gradlew jvmArgs sysProps'
                sh './grailsw -v' // Output grails version for verification checks
            }
        }

        stage('Test cleanup & Compile') {
            steps {
                sh "./gradlew jenkinsClean"
                sh './gradlew compile'
            }
        }

        stage('License Header Check') {
            steps {
                warnError('Missing License Headers') {
                    sh './gradlew --build-cache license'
                }
            }
        }

        stage('Unit Test') {

            steps {
                sh "./gradlew --build-cache test"
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
                }
            }
        }

        stage('Integration Test') {

            steps {
                sh "./gradlew --build-cache integrationTest"
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'build/test-results/integrationTest/*.xml'
                }
            }
        }

        stage('Static Code Analysis') {
            steps {
                sh "./gradlew -PciRun=true staticCodeAnalysis jacocoTestReport"
            }
        }

        stage('Sonarqube') {
            when {
                branch 'develop'
            }
            steps {
                withSonarQubeEnv('JenkinsQube') {
                    sh "./gradlew sonarqube"
                }
            }
        }

        stage('Deploy to Artifactory') {
            when {
                allOf {
                    anyOf {
                        branch 'main'
                        branch 'develop'
                    }
                    expression {
                        currentBuild.currentResult == 'SUCCESS'
                    }
                }

            }
            steps {
                script {
                    sh "./gradlew publish"
                }
            }
        }
    }

    post {
        always {
            publishHTML([
                allowMissing         : false,
                alwaysLinkToLastBuild: true,
                keepAll              : true,
                reportDir            : 'build/reports/tests',
                reportFiles          : 'index.html',
                reportName           : 'Test Report',
                reportTitles         : 'Test'
            ])

            recordIssues enabledForFailure: true, tools: [java(), javaDoc()]
            recordIssues enabledForFailure: true, tool: checkStyle(pattern: '**/reports/checkstyle/*.xml')
            recordIssues enabledForFailure: true, tool: codeNarc(pattern: '**/reports/codenarc/*.xml')
            recordIssues enabledForFailure: true, tool: spotBugs(pattern: '**/reports/spotbugs/*.xml', useRankAsPriority: true)
            recordIssues enabledForFailure: true, tool: pmdParser(pattern: '**/reports/pmd/*.xml')

            publishCoverage adapters: [jacocoAdapter('**/reports/jacoco/jacocoTestReport.xml')]
            outputTestResults()
            jacoco classPattern: '**/build/classes', execPattern: '**/build/jacoco/*.exec', sourceInclusionPattern: '**/*.java,**/*.groovy',
                   sourcePattern: '**/src/main/groovy,**/grails-app/controllers,**/grails-app/domain,**/grails-app/services,**/grails-app/utils'
            archiveArtifacts allowEmptyArchive: true, artifacts: '**/*.log'
            zulipNotification(topic: 'mdm-plugin-nhs-data-dictionary')
        }
    }
}