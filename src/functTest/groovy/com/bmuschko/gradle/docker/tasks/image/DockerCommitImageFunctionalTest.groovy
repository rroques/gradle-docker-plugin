package com.bmuschko.gradle.docker.tasks.image

import com.bmuschko.gradle.docker.AbstractGroovyDslFunctionalTest
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

class DockerCommitImageFunctionalTest extends AbstractGroovyDslFunctionalTest {

    def "can commit image"() {
        given:
        String commitImage = """
            task commitImage(type: DockerCommitImage) {
                dependsOn startContainer
                targetContainerId createContainer.getContainerId()
                author = "john doe"
                message = "My image created"
                tag = "myimage:latest"
            }
        """

        buildFile << containerStart(commitImage)

        when:
        BuildResult result = build('commitImage')

        then:
        result.output.contains("Commiting image for container")
    }

    def "cannot commit image without container"() {
        given:
        String commitImage = """
            task commitImage(type: DockerCommitImage) {
                dependsOn startContainer
                finalizedBy removeContainer
            }
        """

        buildFile << containerStart(commitImage)

        when:
        BuildResult result = buildAndFail('commitImage')

        then:
        result.task(':commitImage').outcome == TaskOutcome.FAILED
        result.output.contains('No value has been specified for property \'containerId\'.')
    }

    def "cannot commit image with invalid container"() {
        given:
        String commitImage = """
            task commitImage(type: DockerCommitImage) {
                dependsOn startContainer
                finalizedBy removeContainer
                targetContainerId "idonotexist"
            }
        """

        buildFile << containerStart(commitImage)

        when:
        BuildResult result = buildAndFail('commitImage')

        then:
        result.task(':commitImage').outcome == TaskOutcome.FAILED
        result.output.contains('{"message":"No such container: idonotexist"}')
    }


    static String containerStart(containerCommitImageExecutionTask) {
        """
            import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
            import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
            import com.bmuschko.gradle.docker.tasks.container.DockerRemoveContainer
            import com.bmuschko.gradle.docker.tasks.image.DockerCommitImage
            import com.bmuschko.gradle.docker.tasks.image.DockerRemoveImage
            import com.bmuschko.gradle.docker.tasks.image.DockerPullImage

            task pullImage(type: DockerPullImage) {
                repository = '$TEST_IMAGE'
                tag = '$TEST_IMAGE_TAG'
            }

            task createContainer(type: DockerCreateContainer) {
                dependsOn pullImage
                targetImageId pullImage.getImageId()
                autoRemove = true
                entrypoint = ['tail', '-f', '/dev/null']
            }

            task startContainer(type: DockerStartContainer) {
                dependsOn createContainer
                targetContainerId createContainer.getContainerId()
            }
            
            task removeContainer(type: DockerRemoveContainer) {
                removeVolumes = true
                force = true
                targetContainerId startContainer.getContainerId()
            }

            ${containerCommitImageExecutionTask}

            task removeImage(type: DockerRemoveImage) {
                dependsOn removeContainer
                force = true
                targetImageId commitImage.getImageId()
            }

            commitImage.finalizedBy removeImage
        """
    }
}
