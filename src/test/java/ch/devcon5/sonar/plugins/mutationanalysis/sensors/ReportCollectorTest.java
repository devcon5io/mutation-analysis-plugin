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
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

import ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.config.Configuration;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ReportCollectorTest {

  public static final String DEFAULT_PIT_REPORTS_DIR = "target/pit-reports";
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();
  @Mock
  private Configuration configuration;

  @Test
  public void findProjectRoot_singleModuleProject() throws IOException {

    final Path moduleRoot = folder.newFolder("test-module").toPath();
    createPom(moduleRoot);

    final TestSensorContext context = TestSensorContext.create(moduleRoot, "test-module");
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Path root = collector.findProjectRoot(moduleRoot);

    assertEquals(moduleRoot, root);
  }

  @Test
  public void findProjectRoot_multiModuleProject() throws IOException {

    final Path moduleRoot = Files.createDirectories(folder.getRoot().toPath().resolve("root-module"));
    final Path childModuleRoot = Files.createDirectories(moduleRoot.resolve("child-module"));

    createPom(moduleRoot, "child-module");
    createPom(childModuleRoot);

    final TestSensorContext context = TestSensorContext.create(moduleRoot, "test-module");
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Path actualRoot = collector.findProjectRoot(childModuleRoot);

    assertEquals(moduleRoot, actualRoot);
  }

  @Test
  public void collectLocalMutants_defaultReportDirectory() throws IOException {
    when(configuration.get(REPORT_DIRECTORY_KEY)).thenReturn(Optional.empty());
    final Path moduleRoot = folder.newFolder("test-module").toPath();

    createPom(moduleRoot);
    createMutationReportsFile(moduleRoot, "target/pit-reports", "ReportCollectorTest_mutations.xml");

    final TestSensorContext context = TestSensorContext.create(moduleRoot, "test-module");
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Collection<Mutant> mutants = collector.collectLocalMutants();

    assertEquals(6, mutants.size());
  }

  @Test
  public void collectLocalMutants_customReportDirectory() throws IOException {

    final String reportsDirectory = "target/reports";
    when(configuration.get(REPORT_DIRECTORY_KEY)).thenReturn(Optional.of(reportsDirectory));

    final Path moduleRoot = folder.newFolder("test-module").toPath();
    createPom(moduleRoot);
    createMutationReportsFile(moduleRoot, reportsDirectory, "ReportCollectorTest_mutations.xml");

    final TestSensorContext context = TestSensorContext.create(moduleRoot, "test-module");
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Collection<Mutant> mutants = collector.collectLocalMutants();

    assertEquals(6, mutants.size());
  }

  @Test
  public void collectGlobalMutants_experimentalFeaturesDisable_noMutants() throws IOException {
    when(configuration.getBoolean(EXPERIMENTAL_FEATURE_ENABLED)).thenReturn(Optional.empty());
    when(configuration.get(REPORT_DIRECTORY_KEY)).thenReturn(Optional.empty());

    final Path moduleRoot = folder.newFolder("test-module").toPath();
    createPom(moduleRoot);
    createMutationReportsFile(moduleRoot, DEFAULT_PIT_REPORTS_DIR, "ReportCollectorTest_mutations.xml");

    final TestSensorContext context = TestSensorContext.create(moduleRoot, "test-module");
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Collection<Mutant> mutants = collector.collectGlobalMutants(context);

    assertEquals(0, mutants.size());
  }

  @Test
  public void collectGlobalMutants_singleModule_customReportDirectory() throws IOException {
    final String reportsDirectory = "target/reports";
    when(configuration.get(REPORT_DIRECTORY_KEY)).thenReturn(Optional.of(reportsDirectory));
    when(configuration.getBoolean(EXPERIMENTAL_FEATURE_ENABLED)).thenReturn(Optional.of(Boolean.TRUE));

    final Path moduleRoot = folder.newFolder("test-module").toPath();
    createPom(moduleRoot);
    createMutationReportsFile(moduleRoot, reportsDirectory, "ReportCollectorTest_mutations.xml");

    final TestSensorContext context = TestSensorContext.create(moduleRoot, "test-module");
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Collection<Mutant> mutants = collector.collectGlobalMutants(context);

    assertEquals(6, mutants.size());
  }

  @Test
  public void collectGlobalMutants_singleModule_defaultReportDirectory() throws IOException {
    when(configuration.get(REPORT_DIRECTORY_KEY)).thenReturn(Optional.empty());
    when(configuration.getBoolean(EXPERIMENTAL_FEATURE_ENABLED)).thenReturn(Optional.of(Boolean.TRUE));

    final File moduleRoot = folder.newFolder("test-module");
    createPom(moduleRoot.toPath());
    createMutationReportsFile(moduleRoot.toPath(), DEFAULT_PIT_REPORTS_DIR, "ReportCollectorTest_mutations.xml");

    final TestSensorContext context = TestSensorContext.create(moduleRoot.toPath(), "test-module");
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Collection<Mutant> mutants = collector.collectGlobalMutants(context);

    assertEquals(6, mutants.size());
  }

  @Test
  public void collectGlobalMutants_multiModule_defaultReportDirectory() throws IOException {
    when(configuration.get(REPORT_DIRECTORY_KEY)).thenReturn(Optional.empty());
    when(configuration.getBoolean(EXPERIMENTAL_FEATURE_ENABLED)).thenReturn(Optional.of(Boolean.TRUE));

    final Path moduleRoot = Files.createDirectories(folder.getRoot().toPath().resolve("root-module"));
    final Path childModule1Root = Files.createDirectories(moduleRoot.resolve("child-module1"));
    final Path childModule2Root = Files.createDirectories(moduleRoot.resolve("child-module2"));

    createPom(moduleRoot, "child-module1","child-module2");
    createPom(childModule1Root);
    createPom(childModule2Root);

    createMutationReportsFile(childModule1Root, DEFAULT_PIT_REPORTS_DIR, "ReportCollectorTest_mutations.xml");
    createMutationReportsFile(childModule2Root, DEFAULT_PIT_REPORTS_DIR, "ReportCollectorTest_mutations.xml");

    final TestSensorContext context = TestSensorContext.create(moduleRoot, "test-module");
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Collection<Mutant> mutants = collector.collectGlobalMutants(context);

    assertEquals(12, mutants.size());
  }

  @Test
  public void collectGlobalMutants_multiModule_customReportDirectory() throws IOException {
    final String reportDirectory = "target/custom";
    when(configuration.get(REPORT_DIRECTORY_KEY)).thenReturn(Optional.of(reportDirectory));
    when(configuration.getBoolean(EXPERIMENTAL_FEATURE_ENABLED)).thenReturn(Optional.of(Boolean.TRUE));

    final Path moduleRoot = Files.createDirectories(folder.getRoot().toPath().resolve("root-module"));
    final Path childModule1Root = Files.createDirectories(moduleRoot.resolve("child-module1"));
    final Path childModule2Root = Files.createDirectories(moduleRoot.resolve("child-module2"));

    createPom(moduleRoot, "child-module1","child-module2");
    createPom(childModule1Root);
    createPom(childModule2Root);

    createMutationReportsFile(childModule1Root, reportDirectory, "ReportCollectorTest_mutations.xml");
    createMutationReportsFile(childModule2Root, reportDirectory, "ReportCollectorTest_mutations.xml");

    final TestSensorContext context = TestSensorContext.create(moduleRoot, "test-module");
    final ReportCollector collector = new ReportCollector(configuration, context.fileSystem());

    final Collection<Mutant> mutants = collector.collectGlobalMutants(context);

    assertEquals(12, mutants.size());
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

    Files.write(moduleRoot.resolve("pom.xml"), b.toString().getBytes("UTF-8"));
  }

  private void createMutationReportsFile(final Path moduleRoot, final String reportsDirectory, final String resourceName) throws IOException {

    final Path reportsDir = Files.createDirectories(moduleRoot.resolve(reportsDirectory));
    try (InputStream is = ReportCollectorTest.class.getResourceAsStream(resourceName);
         OutputStream os = Files.newOutputStream(reportsDir.resolve("mutations.xml"))) {
      IOUtils.copy(is, os);
    }
  }
}
