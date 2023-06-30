pipeline {
    agent {
        label 'master'
    }
    
    environment {
        NEXUS_VERSION = "nexus3"
        NEXUS_PROTOCOL = "http"
        NEXUS_URL = "192.168.1.41:8081"
        NEXUS_REPOSITORY = "maven-releases"
        NEXUS_CREDENTIAL_ID = "jenkins"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout([$class: 'GitSCM', branches: [[name: '*/master']], extensions: [], userRemoteConfigs: [[credentialsId: 'github_gabrielperullini', url: 'git@github.com:gabrielperullini/holamundo.git']]])
            }
        }
        
        stage('Build artifact') {
            agent {
                label 'master'
            }
            steps {
                sh '''
                    /usr/bin/mvn package
                '''
            }
        }
        
        stage('Upload to Nexus') {
            agent {
                label 'master'
            }
            steps {
                script {
                    pom = readMavenPom file: 'pom.xml'
                    echo "Number of files found: ${filesByGlob.size()}"
                    echo "Workspace directory: ${env.WORKSPACE}"
                    
                    filesByGlob = findFiles(glob: "target/*.${pom.packaging}")
                    echo "${filesByGlob[0].name} ${filesByGlob[0].path} ${filesByGlob[0].directory} ${filesByGlob[0].length} ${filesByGlob[0].lastModified}"
                    
                    artifactPath = filesByGlob[0].path
                    artifactExists = fileExists artifactPath

                    if (artifactExists) {
                        echo "*** File: ${artifactPath}, group: ${pom.groupId}, packaging: ${pom.packaging}, version ${pom.version}"
                        
                        nexusArtifactUploader(
                            nexusVersion: NEXUS_VERSION,
                            protocol: NEXUS_PROTOCOL,
                            nexusUrl: NEXUS_URL,
                            groupId: pom.groupId,
                            version: pom.version,
                            repository: NEXUS_REPOSITORY,
                            credentialsId: NEXUS_CREDENTIAL_ID,
                            artifacts: [
                                [artifactId: pom.artifactId,
                                 classifier: '',
                                 file: artifactPath,
                                 type: pom.packaging],
                                [artifactId: pom.artifactId,
                                 classifier: '',
                                 file: "pom.xml",
                                 type: "pom"]
                            ]
                        )
                    } else {
                        error "*** File: ${artifactPath}, could not be found"
                    }
                }
            }
        }
        
        stage("Post") {
            agent {
                label 'maven'
            }
            steps {
                sh '''
                    pwd
                    echo "Clean up workfolder"
                    rm -Rf *
                '''
            }
        }
    }
}
