package com.devbliss.docker

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

/**
 * Created by Christian Soth <christian.soth@devbliss.com> on 07.01.15.
 */

class DockerPluginTest extends GroovyTestCase{

  private final DockerPlugin dockerPlugin = new DockerPlugin()

  @Test
  public void test() {
    Project project = ProjectBuilder.builder().build()
    dockerPlugin.apply(project)
    assertTrue(true)

  }
}