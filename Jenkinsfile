pipeline {
    agent any

    environment {
        PROJECT_NAME = 'vertstorage'
        JAR_NAME = 'vertstorage-1.0.0'
        VERSION = '1.0.0'
    }

    stages {
        stage('setup') {
          steps {
            git(url: "https://github.com/VertCodeDev/${env.PROJECT_NAME}.git", branch: env.BRANCH_NAME, credentialsId: 'github-app-vertcodedev')
          }
        }

        stage('build') {
            steps {
                sh 'chmod +x gradlew'
                sh './gradlew build'
            }
        }

        stage('nexus-publish') {
            steps {
                nexusPublisher nexusInstanceId: '1', nexusRepositoryId: 'vertcodedevelopment', packages: [[$class: 'MavenPackage', mavenAssetList: [[classifier: '', extension: '', filePath: "${env.WORKSPACE}/build/libs/${env.JAR_NAME}.jar"]], mavenCoordinate: [artifactId: "vertstorage", groupId: 'dev.vertcode', packaging: 'jar', version: env.VERSION]]]
            }
        }
    }

    post {
        always {
            discordSend description: "`${env.PROJECT_NAME}` build finished!\n\n__**Build Number**__\n${currentBuild.number}\n\n__**Link**__\n[Click Here](${env.BUILD_URL})", customUsername: "CI | VertCode Development", customAvatarUrl: "https://cdn.vertcodedevelopment.com/logo.png", thumbnail: "https://cdn.vertcodedevelopment.com/logo.png", result: currentBuild.currentResult, webhookURL: "https://canary.discord.com/api/webhooks/1034106603969982484/41HTtCnkDptcbBdRj5l-xzD5xEtFDcztW6aPRKr3BInTkJWM5CMhDPFBxgYLnzCqwSbK"
            cleanWs()
        }
    }
}