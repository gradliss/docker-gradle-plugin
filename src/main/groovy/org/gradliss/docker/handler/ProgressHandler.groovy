package org.gradliss.docker.handler

import org.gradliss.docker.wrapper.ServiceDependency
import org.gradliss.docker.wrapper.ServiceDockerContainer
import de.gesellix.docker.client.DockerClient
import groovy.util.logging.Log
import org.apache.commons.io.IOUtils

@Log
class ProgressHandler {

    public static final String RECEIVED_DEPENDENCIES = "checked_deps"
    public static final String RUNNING = "running"

    DockerClient dockerClient
    List<String> dependingContainersList
    ProgressOutputGenerator progressOutputGenerator

    ProgressHandler(DockerClient dockerClient, List<ServiceDependency> dependingContainersList) {
        this.dockerClient = dockerClient
        this.dependingContainersList = dependingContainersList.collect { dep -> dep.getName() }
        this.progressOutputGenerator = new ProgressOutputGenerator()
    }

    public void waitUntilDependenciesRun() {
        boolean allRun = false;
        Map<String, Map<String, Boolean>> containerList = prepareStartMap()
        progressOutputGenerator.printServices(containerList)
        while (!allRun) {
            setRunningStates(containerList)
            updateDependenciesMap(containerList)
            progressOutputGenerator.printServices(containerList)
            allRun = checkAllRunning(containerList)
        }
        progressOutputGenerator.printServicesForce(containerList)
    }

    void setRunningStates(Map<String, Map<String, Boolean>> containerList) {
        List containers = dockerClient.ps().content
        containers.each { container ->
            setRunningStateForContainer(containerList, container)
        }
    }

    void updateDependenciesMap(Map<String, Map<String, Boolean>> containerList) {
        Map<String, Map<String, Boolean>> additionalContainer = new HashMap()
        containerList.each { container ->
            if (!container.getValue().get(RECEIVED_DEPENDENCIES) && container.getValue().get(RUNNING)) {
                log.info "Request dependencies of service ${container.getKey()}"
                Map<String, Map<String, Boolean>> newDependencies = getContainerDependencies(
                        container.getKey(), containerList)
                container.getValue().put(RECEIVED_DEPENDENCIES, true)
                if (newDependencies != null && newDependencies.size() > 0) {
                    additionalContainer.putAll(newDependencies)
                }
            }
        }
        if (additionalContainer.size() > 0) {
            containerList.putAll(additionalContainer)
        }
    }

    void setRunningStateForContainer(Map<String, Map<String, Boolean>> containerList, Map container) {
        ServiceDockerContainer serviceDockerContainer = new ServiceDockerContainer(container)
        if (containerList.containsKey(serviceDockerContainer.getName())) {
            if (serviceDockerContainer.isRunning()) {
                log.info "Set running state true for ${serviceDockerContainer.getName()}"
                containerList.get(serviceDockerContainer.getName()).put(RUNNING, true)
            }
        }
    }

    Map<String, Map<String, Boolean>> getContainerDependencies(String serviceName, Map<String, Map<String, Boolean>> containerList) {
        Map<String, Map<String, Boolean>> additionalContainer = new HashMap()
        List<String> dependencies = getServiceDependencies(serviceName)
        dependencies.each { dep ->
            if (!containerList.containsKey(dep)) {
                additionalContainer.put(dep, createNewContainerItem())
            }
        }
        return additionalContainer
    }

    Map<String, Map<String, Boolean>> prepareStartMap() {
        Map<String, Map<String, Boolean>> startList = new HashMap()
        dependingContainersList.each { String dep ->
            startList.put(dep, createNewContainerItem())
        }
        return startList
    }

    Map<String, Boolean> createNewContainerItem() {
        Map<String, Boolean> newDepItem = new HashMap()
        newDepItem.put(RECEIVED_DEPENDENCIES, false)
        newDepItem.put(RUNNING, false)
        return newDepItem
    }

    boolean checkAllRunning(Map<String, Map<String, Boolean>> containerList) {
        return containerList.findAll { container ->
            Map<String, Boolean> value = container.getValue()
            !value.get(RUNNING).and(value.get(RECEIVED_DEPENDENCIES))
        }.size() == 0
    }

    List<String> getServiceDependencies(String serviceName) {
        InputStream stream = dockerClient.exec(serviceName, ["cat", "gradle.properties"]).stream as InputStream
        DependingContainerParser parser = new DependingContainerParser(IOUtils.toString(stream), true);
        List<String> deps = parser.getParsedDependencies();
        return deps
    }
}
