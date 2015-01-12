package com.devbliss.docker.task

import de.gesellix.gradle.docker.tasks.AbstractDockerTask
import de.gesellix.gradle.docker.tasks.DockerPullTask
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by Christian Soth <christian.soth@devbliss.com> on 09.01.15.
 */
class StartDependenciesTask extends AbstractDockerTask {

  private static Logger LOGGER = LoggerFactory.getLogger(StartDependenciesTask)

  def dependingContainers = project.devblissDocker.dependingContainers
  def dockerRepository = project.devblissDocker.repositoryName
  def dockerRegistry = project.devblissDocker.registryName
  def versionTag = project.devblissDocker.versionTag

  def runningContainer = []
  def existingContainer = []

  StartDependenciesTask() {
    description = "Pull images and start depending containers for this container"
    group = "Devbliss"
  }


  @TaskAction
  public void run() {
    splitDependingContainersString()

  }

  //TODO pull image wird nur auf erstes element in dependingContainers ausgeführt
  def splitDependingContainersString() {
    LOGGER.info("Depending container: " + dependingContainers)

    dependingContainers.replaceAll("\\s", "").split(",").each() { dep ->
      def (name, port) = dep.split("#").toList()
      println "---------"
      println "name" + name
      println "port" + port
      println "---------"
      pullImage(name.split("_")[0])

    }
  }

  def pullImage(name) {

    def taskToRun = Task (type: new DockerPullTask())

    taskToRun.imageName = "${dockerRepository}/${name}"
    taskToRun.tag = versionTag
    taskToRun.registry = dockerRegistry


    println "what is running?" + taskToRun.getDidWork()
    taskToRun.execute()
    println "what is running after execution?" + taskToRun.getDidWork()

  }

  def setContainerExts() {

    containers.each() { container ->
      def name = container.Names[0].substring(1, container.Names[0].length())
      project.ext.existingContainers.add(name)
      if (container.Status.contains('Up')) {
        project.ext.runningContainers.add(name)
      }
    }
  }
}
