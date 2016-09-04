node {
    // branches to build docker image and publish
    def branch = env.BRANCH_NAME
    def develop = 'develop'
    def production = 'master'
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

}