/*
 * Mutation Analysis Plugin
 * Copyright (C) 2015-2018 DevCon5 GmbH, Switzerland
 * info@devcon5.ch
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package ch.devcon5.sonar.plugins.mutationanalysis.sensors;

import static ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin.EXPERIMENTAL_FEATURE_ENABLED;
import static ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin.REPORT_DIRECTORY_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant;
import ch.devcon5.sonar.plugins.mutationanalysis.testharness.SensorTestHarness;
import ch.devcon5.sonar.plugins.mutationanalysis.testharness.TestConfiguration;
import ch.devcon5.sonar.plugins.mutationanalysis.testharness.TestSensorContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collection;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Report Collector Tests
 */
public class ReportCollectorTest {

  public static final String DEFAULT_PIT_REPORTS_DIR = "target/pit-reports";

  @TempDir
  public Path folder;

  private TestConfiguration configuration;

  private SensorTestHarness harness;

  @BeforeEach
  public void setUp() throws Exception {
    this.harness = SensorTestHarness.builder().withTempFolder(folder).build();
    this.configuration = harness.createConfiguration();
  }

  @Test
  void findProjectRoot_noMavenOrGradleProject_noModules() throws IOException {
    final Path moduleRoot = Files.createDirectories(folder.resolve("test-module"));
    final Path childModuleRoot1 = Files.createDirectories(moduleRoot.resolve("child-module-1"));

    final TestSensorContext context = harness.changeBasePath(moduleRoot).createSensorContext();
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Path root = collector.findProjectRoot(childModuleRoot1);

    //the collector should take the childModuleRoot as project root as it should
    //not recognize the moduleRoot as root as there is no pom
    assertEquals(childModuleRoot1, root);
  }

  @Test
  void findProjectRoot_MavenAndGradleProject_findRootFromAllChildren() throws IOException {
    final Path moduleRoot = Files.createDirectories(folder.resolve("test-module"));
    final Path mvnChildModuleRoot = Files.createDirectories(moduleRoot.resolve("child-module-mvn"));
    final Path gradleChildModuleRoot = Files.createDirectories(moduleRoot.resolve("child-module-grdl"));

    createPom(moduleRoot, "child-module-mvn");
    createPom(mvnChildModuleRoot);
    createSettings(moduleRoot, "child-module-grdl");
    createSettings(gradleChildModuleRoot);

    final TestSensorContext context = harness.changeBasePath(moduleRoot).createSensorContext();
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    assertEquals(moduleRoot, collector.findProjectRoot(mvnChildModuleRoot));
    assertEquals(moduleRoot, collector.findProjectRoot(gradleChildModuleRoot));
  }

  @Test
  void findProjectRoot_singleNonMavenNonGradleProject() throws IOException {
    final Path moduleRoot = Files.createFile(folder.resolve("test-module"));

    final TestSensorContext context = harness.changeBasePath(moduleRoot).createSensorContext();
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Path root = collector.findProjectRoot(moduleRoot);

    assertEquals(moduleRoot, root);
  }

  @Test
  void findProjectRoot_singleModuleMavenProject() throws IOException {
    final Path moduleRoot = Files.createDirectories(folder.resolve("test-module")).toAbsolutePath();
    createPom(moduleRoot);

    final TestSensorContext context = harness.changeBasePath(moduleRoot).createSensorContext();
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Path root = collector.findProjectRoot(moduleRoot);

    assertEquals(moduleRoot, root);
  }

  @Test
  void findProjectRoot_multiModuleMavenProject() throws IOException {
    final Path moduleRoot = Files.createDirectories(folder.resolve("root-module"));
    final Path childModuleRoot = Files.createDirectories(moduleRoot.resolve("child-module"));

    createPom(moduleRoot, "child-module");
    createPom(childModuleRoot);

    final TestSensorContext context = harness.changeBasePath(moduleRoot).createSensorContext();
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Path actualRoot = collector.findProjectRoot(childModuleRoot);

    assertEquals(moduleRoot, actualRoot);
  }

  @Test
  void findProjectRoot_multiModuleMavenProjectWithMalformedPom() throws IOException {
    final Path moduleRoot = Files.createDirectories(folder.resolve("root-module"));
    final Path childModuleRoot = Files.createDirectories(moduleRoot.resolve("child-module"));

    createMalFormedPom(moduleRoot, "child-module");
    createPom(childModuleRoot);

    final TestSensorContext context = harness.changeBasePath(moduleRoot).createSensorContext();
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Path actualRoot = collector.findProjectRoot(childModuleRoot);

    assertEquals(childModuleRoot, actualRoot);
  }

  @Test
  void findProjectRoot_multiModuleMavenProjectWithSiblingParent_withoutSharedReactorPom() throws IOException {
    final Path root = Files.createDirectories(folder.resolve("root"));
    final Path parentModuleRoot = Files.createDirectories(root.resolve("parent-module"));
    final Path childModuleRoot = Files.createDirectories(root.resolve("child-module"));

    createPom(parentModuleRoot, "../child-module", "../parent-module");
    createPomWithRelativeParent(childModuleRoot, "../parent-module");

    final TestSensorContext context = harness.changeBasePath(root).createSensorContext();
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Path actualRoot = collector.findProjectRoot(childModuleRoot);
    System.out.println(actualRoot);

    assertEquals(parentModuleRoot.toAbsolutePath(), actualRoot.normalize());
  }

  @Test
  void findProjectRoot_multiModuleMavenProjectWithSiblingParent_withSharedReactorPom() throws IOException {
    final Path moduleRoot = Files.createDirectories(folder.resolve("root-module"));
    final Path parentModuleRoot = Files.createDirectories(moduleRoot.resolve("parent-module"));
    final Path childModuleRoot = Files.createDirectories(moduleRoot.resolve("child-module"));

    createPom(moduleRoot, "child-module", "parent-module");
    createPom(parentModuleRoot);
    createPomWithRelativeParent(childModuleRoot, "../parent-module");

    final TestSensorContext context = harness.changeBasePath(moduleRoot).createSensorContext();
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Path actualRoot = collector.findProjectRoot(childModuleRoot);
    System.out.println(actualRoot);

    assertEquals(moduleRoot, actualRoot);
  }


  @Test
  void findProjectRoot_singleModuleGradleProject() throws IOException {
    final Path moduleRoot = Files.createDirectories(folder.resolve("test-module"));
    createSettings(moduleRoot);

    final TestSensorContext context = harness.changeBasePath(moduleRoot).createSensorContext();
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Path root = collector.findProjectRoot(moduleRoot);

    assertEquals(moduleRoot, root);
  }

  @Test
  void findProjectRoot_multiModuleGradleProject() throws IOException {
    final Path moduleRoot = Files.createDirectories(folder.resolve("root-module"));
    final Path childModuleRoot = Files.createDirectories(moduleRoot.resolve("child-module"));

    createSettings(moduleRoot, "child-module");
    createSettings(childModuleRoot);

    final TestSensorContext context = harness.changeBasePath(moduleRoot).createSensorContext();
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Path actualRoot = collector.findProjectRoot(childModuleRoot);

    assertEquals(moduleRoot, actualRoot);
  }

  @Test
  void findProjectRoot_multiModuleProjectWithoutConfigurationFiles() throws IOException {
    final Path moduleRoot = Files.createDirectories(folder.resolve("root-module"));
    final Path childModuleRoot = Files.createDirectories(moduleRoot.resolve("child-module"));

    final TestSensorContext context = harness.changeBasePath(moduleRoot).createSensorContext();
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Path actualRoot = collector.findProjectRoot(childModuleRoot);

    assertEquals(childModuleRoot, actualRoot);
  }

  @Test
  void findProjectRoot_multiModuleGradleProjectWithSettingsFileAndPomFileMalformed() throws IOException {
    final Path moduleRoot = Files.createDirectories(folder.resolve("root-module"));
    final Path childModuleRoot = Files.createDirectories(moduleRoot.resolve("child-module"));

    createMalFormedSettings(moduleRoot, "child-module");
    createSettings(childModuleRoot);
    createMalFormedPom(moduleRoot, "child-module");
    createPom(childModuleRoot);

    final TestSensorContext context = harness.changeBasePath(moduleRoot).createSensorContext();
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Path actualRoot = collector.findProjectRoot(childModuleRoot);

    assertEquals(childModuleRoot, actualRoot);
  }

  @Test
  void findProjectRoot_multiGradleProjectWithSettingsFileMalformedAndAValidPomFile() throws IOException {
    final Path moduleRoot = Files.createDirectories(folder.resolve("root-module"));
    final Path childModuleRoot = Files.createDirectories(moduleRoot.resolve("child-module"));

    createMalFormedSettings(moduleRoot, "child-module");
    createSettings(childModuleRoot);
    createPom(moduleRoot, "child-module");
    createPom(childModuleRoot);

    final TestSensorContext context = harness.changeBasePath(moduleRoot).createSensorContext();
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Path actualRoot = collector.findProjectRoot(childModuleRoot);

    assertEquals(moduleRoot, actualRoot);
  }

  @Test
  void findProjectRoot_multiModuleGradleProjectWithMalformedSettings() throws IOException {
    final Path moduleRoot = Files.createDirectories(folder.resolve("root-module"));
    final Path childModuleRoot = Files.createDirectories(moduleRoot.resolve("child-module"));

    createMalFormedSettings(moduleRoot, "child-module");
    createPom(childModuleRoot);

    final TestSensorContext context = harness.changeBasePath(moduleRoot).createSensorContext();
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Path actualRoot = collector.findProjectRoot(childModuleRoot);

    assertEquals(childModuleRoot, actualRoot);
  }

  @Test
  void collectLocalMutants_defaultReportDirectory() throws IOException {
    final Path moduleRoot = Files.createDirectories(folder.resolve("test-module"));

    createPom(moduleRoot);
    createMutationReportsFile(moduleRoot, "target/pit-reports", "ReportCollectorTest_mutations.xml");

    final TestSensorContext context = harness.changeBasePath(moduleRoot).createSensorContext();
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Collection<Mutant> mutants = collector.collectLocalMutants();

    assertEquals(6, mutants.size());
  }

  @Test
  void collectLocalMutants_customReportDirectory() throws IOException {
    final String reportsDirectory = "target/reports";
    configuration.set(REPORT_DIRECTORY_KEY, reportsDirectory);

    final Path moduleRoot = Files.createDirectories(folder.resolve("test-module"));
    createPom(moduleRoot);
    createMutationReportsFile(moduleRoot, reportsDirectory, "ReportCollectorTest_mutations.xml");

    final TestSensorContext context = harness.changeBasePath(moduleRoot).createSensorContext();
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Collection<Mutant> mutants = collector.collectLocalMutants();

    assertEquals(6, mutants.size());
  }

  @Test
  void collectGlobalMutants_experimentalFeaturesDisable_noMutants() throws IOException {
    final Path moduleRoot = Files.createDirectories(folder.resolve("test-module"));
    createPom(moduleRoot);
    createMutationReportsFile(moduleRoot, DEFAULT_PIT_REPORTS_DIR, "ReportCollectorTest_mutations.xml");

    final TestSensorContext context = harness.changeBasePath(moduleRoot).createSensorContext();
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Collection<Mutant> mutants = collector.collectGlobalMutants(context);

    assertEquals(0, mutants.size());
  }

  @Test
  void collectGlobalMutants_singleModule_defaultReportDirectory() throws IOException {
    configuration.set(EXPERIMENTAL_FEATURE_ENABLED, true);

    final Path moduleRoot = Files.createDirectories(folder.resolve("test-module"));
    createPom(moduleRoot);
    createMutationReportsFile(moduleRoot, DEFAULT_PIT_REPORTS_DIR, "ReportCollectorTest_mutations.xml");

    final TestSensorContext context = harness.changeBasePath(moduleRoot).createSensorContext();
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Collection<Mutant> mutants = collector.collectGlobalMutants(context);

    assertEquals(6, mutants.size());
  }

  @Test
  void collectGlobalMutants_singleModule_customReportDirectory() throws IOException {
    final String reportsDirectory = "target/reports";
    configuration.set(REPORT_DIRECTORY_KEY, reportsDirectory);
    configuration.set(EXPERIMENTAL_FEATURE_ENABLED, true);

    final Path moduleRoot = Files.createDirectories(folder.resolve("test-module"));
    createPom(moduleRoot);
    createMutationReportsFile(moduleRoot, reportsDirectory, "ReportCollectorTest_mutations.xml");

    final TestSensorContext context = harness.changeBasePath(moduleRoot).createSensorContext();
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Collection<Mutant> mutants = collector.collectGlobalMutants(context);

    assertEquals(6, mutants.size());
  }

  @Test
  void collectGlobalMutants_multiModule_defaultReportDirectory() throws IOException {
    configuration.set(EXPERIMENTAL_FEATURE_ENABLED, true);

    final Path moduleRoot = Files.createDirectories(folder.resolve("root-module"));
    final Path childModule1Root = Files.createDirectories(moduleRoot.resolve("child-module1"));
    final Path childModule2Root = Files.createDirectories(moduleRoot.resolve("child-module2"));

    createPom(moduleRoot, "child-module1", "child-module2");
    createPom(childModule1Root);
    createPom(childModule2Root);

    createMutationReportsFile(childModule1Root, DEFAULT_PIT_REPORTS_DIR, "ReportCollectorTest_mutations.xml");
    createMutationReportsFile(childModule2Root, DEFAULT_PIT_REPORTS_DIR, "ReportCollectorTest_mutations.xml");

    final TestSensorContext context = harness.changeBasePath(moduleRoot).createSensorContext();
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Collection<Mutant> mutants = collector.collectGlobalMutants(context);

    assertEquals(12, mutants.size());
  }

  @Test
  void collectGlobalMutants_multiModule_rootFolderFromSettings() throws IOException {
    configuration.set(EXPERIMENTAL_FEATURE_ENABLED, true);

    final Path parentRoot = Files.createDirectories(folder.resolve("root-module"));
    final Path moduleRoot = Files.createDirectories(parentRoot.resolve("parent"));
    final Path childModule1Root = Files.createDirectories(moduleRoot.resolve("child-module1"));
    final Path childModule2Root = Files.createDirectories(parentRoot.resolve("child-module2"));

    configuration.set("dc5.mutationAnalysis.project.root", moduleRoot);

    createPom(parentRoot); //no child modules defined here
    createPom(moduleRoot, "child-module1", "../child-module2");
    createPom(childModule1Root);
    createPom(childModule2Root);

    createMutationReportsFile(childModule1Root, DEFAULT_PIT_REPORTS_DIR, "ReportCollectorTest_mutations.xml");
    createMutationReportsFile(childModule2Root, DEFAULT_PIT_REPORTS_DIR, "ReportCollectorTest_mutations.xml");

    final TestSensorContext context = harness.changeBasePath(parentRoot).createSensorContext();
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Collection<Mutant> mutants = collector.collectGlobalMutants(context);

    assertEquals(12, mutants.size());
  }

  @Test
  void collectGlobalMutants_multiModule_rootFolderFromRelativePathInPom() throws IOException {
    configuration.set(EXPERIMENTAL_FEATURE_ENABLED, true);

    final Path parentRoot = Files.createDirectories(folder.resolve("root-module"));
    final Path intermediate = Files.createDirectories(parentRoot.resolve("intermediate"));
    final Path childModule1Root = Files.createDirectories(intermediate.resolve("child-module1"));
    final Path childModule2Root = Files.createDirectories(intermediate.resolve("child-module2"));

    createPom(parentRoot, "intermediate/child-module1", "intermediate/child-module2");
    createPomWithRelativeParent(childModule1Root, "../");
    createPomWithRelativeParent(childModule1Root, "../");

    createMutationReportsFile(childModule1Root, DEFAULT_PIT_REPORTS_DIR, "ReportCollectorTest_mutations.xml");
    createMutationReportsFile(childModule2Root, DEFAULT_PIT_REPORTS_DIR, "ReportCollectorTest_mutations.xml");

    final TestSensorContext context = harness.changeBasePath(parentRoot).createSensorContext();
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Collection<Mutant> mutants = collector.collectGlobalMutants(context);

    assertEquals(12, mutants.size());
  }

  @Test
  void collectGlobalMutants_multiModule_customReportDirectory() throws IOException {
    final String reportDirectory = "target/custom";
    configuration.set(REPORT_DIRECTORY_KEY, reportDirectory);
    configuration.set(EXPERIMENTAL_FEATURE_ENABLED, true);

    final Path moduleRoot = Files.createDirectories(folder.resolve("root-module"));
    final Path childModule1Root = Files.createDirectories(moduleRoot.resolve("child-module1"));
    final Path childModule2Root = Files.createDirectories(moduleRoot.resolve("child-module2"));

    createPom(moduleRoot, "child-module1", "child-module2");
    createPom(childModule1Root);
    createPom(childModule2Root);

    createMutationReportsFile(childModule1Root, reportDirectory, "ReportCollectorTest_mutations.xml");
    createMutationReportsFile(childModule2Root, reportDirectory, "ReportCollectorTest_mutations.xml");

    final TestSensorContext context = harness.changeBasePath(moduleRoot).createSensorContext();
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Collection<Mutant> mutants = collector.collectGlobalMutants(context);

    assertEquals(12, mutants.size());
  }

  @Test
  void exceptionHandling_of_isSamePath_fsMockCausesIOException_false() throws Exception {
    //I found no other efficient way than using mockito to induce IOException on working with Path
    FileSystemProvider fsProv = mock(FileSystemProvider.class);
    FileSystem fs = mock(FileSystem.class);
    Path aPath = mock(Path.class);

    when(aPath.getFileSystem()).thenReturn(fs);
    when(fs.provider()).thenReturn(fsProv);
    when(fsProv.isSameFile(any(Path.class), any(Path.class))).thenThrow(IOException.class);

    final ReportCollector collector = new ReportCollector(configuration, harness.createSensorContext().fileSystem());

    assertFalse(collector.isSamePath(aPath, aPath));
  }

  @Test
  void exceptionHandling_of_readMutantsFromReport_fsMockCausesIOException_emptyResult() throws Exception {
    //I found no other efficient way than using mockito to induce IOException on working with Path
    FileSystemProvider fsProv = mock(FileSystemProvider.class);
    FileSystem fs = mock(FileSystem.class);
    File file = mock(File.class);
    Path reportPath = mock(Path.class);

    when(file.exists()).thenReturn(true);
    when(reportPath.toFile()).thenReturn(file);
    when(reportPath.getFileSystem()).thenReturn(fs);
    when(fs.provider()).thenReturn(fsProv);
    when(fsProv.newInputStream(eq(reportPath), any(OpenOption.class))).thenThrow(IOException.class);

    final ReportCollector collector = new ReportCollector(configuration, harness.createSensorContext().fileSystem());

    assertEquals(0, collector.readMutantsFromReport(reportPath).count());
  }

  private void createPomWithRelativeParent(final Path moduleRoot, String relParent) throws IOException {
    StringBuilder b = new StringBuilder(128);
    b.append("<project>");
    b.append("<parent>");
    b.append("<groupId>test</groupId>");
    b.append("<artifactId>test</artifactId>");
    if (relParent.isEmpty()) {
      b.append("<relativePath />");
    } else {
      b.append("<relativePath>").append(relParent).append("</relativePath>");
    }
    b.append("</parent>");
    b.append("</project>");

    Files.write(moduleRoot.resolve("pom.xml"), b.toString().getBytes(StandardCharsets.UTF_8));
  }

  private void createPom(final Path moduleRoot, String... moduleNames) throws IOException {
    StringBuilder b = new StringBuilder(128);
    b.append("<project>");
    if (moduleNames.length > 0) {
      b.append("<modules>");
      for (String module : moduleNames) {
        b.append("<module>").append(module).append("</module>");
      }
      b.append("</modules>");
    }
    b.append("</project>");

    Files.write(moduleRoot.resolve("pom.xml"), b.toString().getBytes(StandardCharsets.UTF_8));
  }

  private void createMalFormedPom(final Path moduleRoot, String... moduleNames) throws IOException {
    StringBuilder b = new StringBuilder(128);
    b.append("<project>");
    if (moduleNames.length > 0) {
      b.append("<malformed>");
      for (String module : moduleNames) {
        b.append("<module>").append(module).append("</module>");
      }
      b.append("</realMalformed>");
    }
    b.append("</project>");

    Files.write(moduleRoot.resolve("pom.xml"), b.toString().getBytes(StandardCharsets.UTF_8));
  }

  private void createSettings(Path moduleRoot, String... moduleNames) throws IOException {
    StringBuilder b = new StringBuilder(128);
    b.append("rootProject.name = \'test-root-project\'\n\n");
    for (String module : moduleNames) {
      b.append("include \'").append(module).append("\'\n)");
    }
    Files.write(moduleRoot.resolve("settings.gradle"), b.toString().getBytes(StandardCharsets.UTF_8));
  }

  private void createMalFormedSettings(Path moduleRoot, String... moduleNames) throws IOException {
    StringBuilder b = new StringBuilder(128);
    b.append("rootProject.name = \'test-root-project\'\n\n");
    for (String module : moduleNames) {
      b.append("malformed \'").append(module).append("\'\n)");
    }
    Files.write(moduleRoot.resolve("settings.gradle"), b.toString().getBytes(StandardCharsets.UTF_8));
  }

  private Path createMutationReportsFile(final Path moduleRoot, final String reportsDirectory, final String resourceName) throws IOException {
    final Path reportsDir = Files.createDirectories(moduleRoot.resolve(reportsDirectory));
    final Path reportFile = reportsDir.resolve("mutations.xml");
    try (InputStream is = ReportCollectorTest.class.getResourceAsStream(resourceName); OutputStream os = Files.newOutputStream(reportFile)) {
      IOUtils.copy(is, os);
    }
    return reportFile;
  }

}
