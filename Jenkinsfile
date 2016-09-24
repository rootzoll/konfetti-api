node {
    // container and image names
    def imageName = 'konfetti/api'

    // branches to build docker image and publish
    def branch = env.BRANCH_NAME
    def develop = 'develop'
    def production = 'master'
    def dockerBranches = [develop, production]

    // ========================================================================
    stage 'Compile'
    // ========================================================================
    checkout scm
    sh "./mvnw clean compile"

    // ========================================================================
    stage 'Tests'
    // ========================================================================
    sh "./mvnw verify"
    step([$class: 'JUnitResultArchiver', testResults: '**/target/*-reports/TEST*.xml'])

    echo "Git Branch : ${branch}"
    if (dockerBranches.contains(branch)) {
        echo "Building docker container for branch: ${branch}"

        docker.withRegistry('https://docker.io', 'docker-credentials') {

            def dockerImg;
            // ========================================================================
            stage 'Build Docker image'
            // ========================================================================
            echo "build image : ${imageName}:latest"
            dockerImg = docker.build("${imageName}:latest")

            // ========================================================================
            stage 'Tag and push image'
            // ========================================================================
            echo "tag image"
            dockerImg.tag()
            echo "push image"
            dockerImg.push()

            if (production.equals(branch)) {
                stage "Deploy On Production"
                build job: 'DeployKonfettiOnProduction', wait: false
            }

        }
    }

}