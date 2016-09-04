node {
    // container and image names
    def containerName = 'konfettiBackend'
    def imageName = 'konfetti/service/backend'

    // docker run parameters
    def runPorts = '8080:8080'
    def runNetwork = 'konfettiNetwork'

    // branches to build docker image and publish
    def branch = env.BRANCH_NAME
    def develop = 'develop'
    def production = 'master'
    def testingDocker = 'catarata02_removeall_except_api'
    def dockerBranches = [develop, production]

    // ========================================================================
    stage 'Compile'
    // ========================================================================
    checkout scm
    sh "./mvnw compile"

    // ========================================================================
    stage 'Tests'
    // ========================================================================
    sh "./mvnw verify"
    step([$class: 'JUnitResultArchiver', testResults: '**/target/*-reports/TEST*.xml'])

    echo "Git Branch : ${branch}"
    if (dockerBranches.contains(branch)) {
        echo "Building docker container for branch: ${branch}"

        docker.withRegistry('https://index.docker.io/v1/', 'docker-credentials') {

            // ========================================================================
            stage 'Build Docker image'
            // ========================================================================
            echo "build image"
            def dockerImg = docker.build("${imageName}:${env.BRANCH_NAME}")

            // ========================================================================
            stage 'Tag and push image'
            // ========================================================================
            echo "tag image"
            dockerImg.tag()
            echo "push image"
            dockerImg.push()

            if (production.equals(branch)) {
                // ========================================================================
                stage 'Deploy to Production'
                // ========================================================================

                // stop the old container
                sh "docker ps --filter name=${containerName} -q > /tmp/jenkinsTempFile"
                def container = readFile '/tmp/jenkinsTempFile'
                echo "Found running container : ${container}"
                if (container) {
                    echo "Stopping container : ${container}"
                    sh "docker stop ${container}"
                }

                // remove the old container
                sh "docker ps --filter name=${containerName} -q -a > /tmp/jenkinsTempFile"
                container = readFile '/tmp/jenkinsTempFile'
                echo "Found stopped container : ${container}"
                if (container) {
                    echo "Removing container : ${container}"
                    sh "docker rm ${container}"
                }

                // starth the new container
                def myApp = docker.image("${imageName}:${env.BRANCH_NAME}");
                def dockerContainer = myApp.run("-p ${runPorts} --net ${runNetwork} --name ${containerName} --restart=always -d");
                echo "dockerContainer started with id : ${dockerContainer.id}"
            }
        }
    }

}