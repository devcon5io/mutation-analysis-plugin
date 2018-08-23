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

import static ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin.EFFORT_FACTOR_MISSING_COVERAGE;
import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.MUTATIONS_KILLED_KEY;
import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.MUTATIONS_TOTAL_KEY;
import static ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition.PARAM_MUTANT_COVERAGE_THRESHOLD;
import static ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition.REPOSITORY_KEY;
import static ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition.RULE_SURVIVED_MUTANT;
import static ch.devcon5.sonar.plugins.mutationanalysis.testharness.TestUtils.assertContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin;
import ch.devcon5.sonar.plugins.mutationanalysis.testharness.SensorTestHarness;
import ch.devcon5.sonar.plugins.mutationanalysis.testharness.TestConfiguration;
import ch.devcon5.sonar.plugins.mutationanalysis.testharness.TestSensorContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.profiles.RulesProfile;

public class PitestSensorTest {

   @Rule
   public TemporaryFolder folder = new TemporaryFolder();

   private SensorTestHarness harness;

   @Before
   public void setUp() throws Exception {
      this.harness = SensorTestHarness.builder().withTempFolder(folder).withResourceResolver(this.getClass()).build();
   }
   @Test
   public void describe() {
      final PitestSensor sensor = new PitestSensor(harness.createConfiguration(), harness.createEmptyRulesProfile(), harness.createSensorContext().fileSystem());

      final DefaultSensorDescriptor desc = new DefaultSensorDescriptor();

      sensor.describe(desc);

      assertEquals("Mutation Analysis", desc.name());

      assertTrue(desc.languages().contains("java"));
      assertTrue(desc.ruleRepositories().contains(REPOSITORY_KEY + ".java"));

      assertTrue(desc.languages().contains("kotlin"));
      assertTrue(desc.ruleRepositories().contains(REPOSITORY_KEY + ".kotlin"));
   }

   @Test
   public void testToString() throws Exception {

      final PitestSensor sensor = new PitestSensor(harness.createConfiguration(), harness.createEmptyRulesProfile(), harness.createSensorContext().fileSystem());

      assertEquals("PitestSensor", sensor.toString());
   }

   @Test
   public void execute_with_noFiles_and_SensorDisabled_noIssuesOrMeasuresCreated() {

      final TestSensorContext context = harness.createSensorContext();

      final PitestSensor sensor = new PitestSensor(disableSensor(context), harness.createEmptyRulesProfile(), context.fileSystem());

      sensor.execute(context);

      assertTrue(context.getStorage().getIssues().isEmpty());
      assertTrue(context.getStorage().getMeasures().isEmpty());
   }

   @Test
   public void execute_with_files_and_SensorDisabled_noIssuesAndMeasuresCreated() throws Exception {


      final TestSensorContext context = harness.createSensorContext();
      context.addTestFile("src/main/java/Test.java");

      final PitestSensor sensor = new PitestSensor(disableSensor(context), harness.createEmptyRulesProfile(), context.fileSystem());

      sensor.execute(context);

      assertTrue(context.getStorage().getIssues().isEmpty());
      assertTrue(context.getStorage().getMeasures().isEmpty());

   }

   @Test
   public void execute_with_files_and_SensorEnabled_noReport_noIssuesAndMeasuresCreated() throws Exception {

      final TestSensorContext context = harness.createSensorContext();
      context.addTestFile("src/main/java/Test.java");

      final RulesProfile profile = harness.createRulesProfile(RULE_SURVIVED_MUTANT);

      //sensor is configured by default
      final PitestSensor sensor = new PitestSensor(context.getTestConfiguration(), profile, context.fileSystem());

      sensor.execute(context);

      assertTrue(context.getStorage().getIssues().isEmpty());
      assertTrue(context.getStorage().getMeasures().isEmpty());

   }

   @Test
   public void execute_with_files_and_sensorEnabled_and_ReportExist_noActiveRule_noIssuesCreated_MetricsCreated() throws Exception {

      harness.resourceToFile("target/pit-reports/mutations.xml", "PitestSensorTest_Java_mutations.xml");

      final TestSensorContext context = harness.createSensorContext().scanFiles();
      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", md -> md.lines = 200);


      //sensor is configured by default
      final PitestSensor sensor = new PitestSensor(context.getTestConfiguration(), harness.createEmptyRulesProfile(), context.fileSystem());

      sensor.execute(context);

      assertTrue(context.getStorage().getIssues().isEmpty());
      assertEquals(11, context.getStorage().getMeasures().size());
   }


   @Test
   public void execute_mutatorSpecificRuleActive_issueCreated() throws Exception {

      harness.resourceToFile("target/pit-reports/mutations.xml", "PitestSensorTest_Java_mutations.xml");

      final TestSensorContext context = harness.createSensorContext().scanFiles();
      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", md -> md.lines = 200);

      final RulesProfile profile = harness.createRulesProfile("mutant.NEGATE_CONDITIONALS");

      //sensor is configured by default
      final PitestSensor sensor = new PitestSensor(context.getTestConfiguration(), profile, context.fileSystem());

      sensor.execute(context);

      final List<Issue> issues = context.getStorage().getIssues();
      assertContains(issues, i -> {
         assertEquals("mutant.NEGATE_CONDITIONALS", i.ruleKey().rule());
         assertEquals("test-module:src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", i.primaryLocation().inputComponent().key());
         assertEquals(1.0, i.gap(), 0.01);
         assertTextrangeOnLine(i.primaryLocation().textRange(), 172, 79);
         assertEquals("Alive Mutant: A conditional expression has been negated without being detected by a test.", i.primaryLocation().message());
      });
      assertContains(issues, i -> {
         assertEquals("mutant.NEGATE_CONDITIONALS", i.ruleKey().rule());
         assertEquals("test-module:src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", i.primaryLocation().inputComponent().key());
         assertEquals(1.0, i.gap(), 0.01);
         assertTextrangeOnLine(i.primaryLocation().textRange(), 175, 79);
         assertEquals("Alive Mutant: A conditional expression has been negated without being detected by a test. (WITH_SUFFIX)", i.primaryLocation().message());
      });
      assertEquals(11, context.getStorage().getMeasures().size());
   }

   @Test
   public void execute_survivedMutantRuleActive_issueCreated() throws Exception {

      harness.resourceToFile("target/pit-reports/mutations.xml", "PitestSensorTest_Java_mutations.xml");
      final TestSensorContext context = harness.createSensorContext().scanFiles();
      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", md -> md.lines = 200);
      final RulesProfile profile = harness.createRulesProfile("mutant.uncovered");
      final PitestSensor sensor = new PitestSensor(context.getTestConfiguration(), profile, context.fileSystem());

      sensor.execute(context);

      final List<Issue> issues = context.getStorage().getIssues();
      assertEquals(1, issues.size());
      assertContains(issues, i -> {
         assertEquals("mutant.uncovered", i.ruleKey().rule());
         assertEquals("test-module:src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", i.primaryLocation().inputComponent().key());
         assertEquals(1.0, i.gap(), 0.01);
         assertTextrangeOnLine(i.primaryLocation().textRange(), 175, 79);
         assertEquals("Alive Mutant: A conditional expression has been negated without being detected by a test. (WITH_SUFFIX)", i.primaryLocation().message());
      });
      assertEquals(11, context.getStorage().getMeasures().size());

   }

   @Test
   public void execute_unknownMutantStatusRuleActive_issueCreated() throws Exception {

      harness.resourceToFile("target/pit-reports/mutations.xml", "PitestSensorTest_Java_mutations.xml");
      final TestSensorContext context = harness.createSensorContext().scanFiles();
      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", md -> md.lines = 200);
      final RulesProfile profile = harness.createRulesProfile("mutant.unknownStatus");
      final PitestSensor sensor = new PitestSensor(context.getTestConfiguration(), profile, context.fileSystem());

      sensor.execute(context);

      final List<Issue> issues = context.getStorage().getIssues();
      assertEquals(1, issues.size());
      assertContains(issues, i -> {
         assertEquals("mutant.unknownStatus", i.ruleKey().rule());
         assertEquals("test-module:src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", i.primaryLocation().inputComponent().key());
         assertEquals(1.0, i.gap(), 0.01);
         assertTextrangeOnLine(i.primaryLocation().textRange(), 175, 79);
         assertEquals("Alive Mutant: A return value has been replaced by a method argument without being detected by a test.", i.primaryLocation().message());
      });
      assertEquals(11, context.getStorage().getMeasures().size());

   }

   @Test
   public void execute_coverageThresholdRuleActive_belowThreshold_twoMutantMissing() throws Exception {
      harness.resourceToFile("target/pit-reports/mutations.xml", "PitestSensorTest_Java_mutations.xml");
      final TestSensorContext context = harness.createSensorContext().scanFiles();
      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", md -> md.lines = 200);
      context.getTestConfiguration().set(EFFORT_FACTOR_MISSING_COVERAGE, 2.0);

      final RulesProfile profile = harness.createRulesProfile(harness.createRule("mutant.coverage", PARAM_MUTANT_COVERAGE_THRESHOLD, "66.6"));
      final PitestSensor sensor = new PitestSensor(context.getTestConfiguration(), profile, context.fileSystem());

      sensor.execute(context);

      final List<Issue> issues = context.getStorage().getIssues();
      assertEquals(1, issues.size());
      assertContains(issues, i -> {
         assertEquals("mutant.coverage", i.ruleKey().rule());
         assertEquals("test-module:src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", i.primaryLocation().inputComponent().key());
         assertEquals(2.0, i.gap(), 0.01);
         assertEquals("1 more mutants need to be killed to get the mutation coverage from 50.0% to 66.6%", i.primaryLocation().message());
      });

      assertCoverage(50.0, context.getStorage().getMeasures());
   }

   @Test
   public void execute_coverageThresholdRuleActive_belowThreshold_moreMutantsMissing() throws Exception {

      harness.resourceToFile("target/pit-reports/mutations.xml", "PitestSensorTest_Java_mutations.xml");
      final TestSensorContext context = harness.createSensorContext().scanFiles();
      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", md -> md.lines = 200);
      context.getTestConfiguration().set(EFFORT_FACTOR_MISSING_COVERAGE, 2.0);
      final RulesProfile profile = harness.createRulesProfile(harness.createRule("mutant.coverage", PARAM_MUTANT_COVERAGE_THRESHOLD, "80.0"));
      final PitestSensor sensor = new PitestSensor(context.getTestConfiguration(), profile, context.fileSystem());

      sensor.execute(context);

      final List<Issue> issues = context.getStorage().getIssues();
      assertEquals(1, issues.size());
      assertContains(issues, i -> {
         assertEquals("mutant.coverage", i.ruleKey().rule());
         assertEquals("test-module:src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", i.primaryLocation().inputComponent().key());
         assertEquals(3.6, i.gap(), 0.01);
         assertEquals("2 more mutants need to be killed to get the mutation coverage from 50.0% to 80.0%", i.primaryLocation().message());
      });

      assertCoverage(50.0, context.getStorage().getMeasures());
   }

   @Test
   public void execute_coverageThresholdRuleActive_aboveThreshold_noIssue() throws Exception {

      harness.resourceToFile("target/pit-reports/mutations.xml", "PitestSensorTest_Java_mutations.xml");
      final TestSensorContext context = harness.createSensorContext().scanFiles();
      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", md -> md.lines = 200);
      final RulesProfile profile = harness.createRulesProfile(harness.createRule("mutant.coverage", PARAM_MUTANT_COVERAGE_THRESHOLD, "40.0"));
      final PitestSensor sensor = new PitestSensor(context.getTestConfiguration(), profile, context.fileSystem());

      sensor.execute(context);

      assertTrue(context.getStorage().getIssues().isEmpty());

   }

   @Test
   public void testExecute_coverageThresholdRuleActive_onThreshold_noIssue() throws Exception {

      harness.resourceToFile("target/pit-reports/mutations.xml", "PitestSensorTest_Java_mutations.xml");
      final TestSensorContext context = harness.createSensorContext().scanFiles();
      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", md -> md.lines = 200);
      context.getTestConfiguration().set(EFFORT_FACTOR_MISSING_COVERAGE, 2.0);
      final RulesProfile profile = harness.createRulesProfile(harness.createRule("mutant.coverage", PARAM_MUTANT_COVERAGE_THRESHOLD, "50.0"));
      final PitestSensor sensor = new PitestSensor(context.getTestConfiguration(), profile, context.fileSystem());

      sensor.execute(context);

      assertTrue(context.getStorage().getIssues().isEmpty());
      assertCoverage(50.0, context.getStorage().getMeasures());
   }

   private void assertCoverage(final double percent, final List<Measure> measures) {
      final int total = (int) assertContains(measures, m -> assertEquals(MUTATIONS_TOTAL_KEY, m.metric().key())).value();
      final int killed = (int) assertContains(measures, m -> assertEquals(MUTATIONS_KILLED_KEY, m.metric().key())).value();
      assertEquals(percent, (double)killed * 100./(double)total, 0.01);
   }

   private void assertTextrangeOnLine(final TextRange textRange, final int expectedLineNumber, final int expectedLineLength) {
      assertEquals(expectedLineNumber, textRange.start().line());
      assertEquals(0, textRange.start().lineOffset());
      assertEquals(expectedLineNumber, textRange.end().line());
      assertEquals(expectedLineLength, textRange.end().lineOffset());
   }


   private TestConfiguration disableSensor(final TestSensorContext context) {
      return context.getTestConfiguration()
                    .set(MutationAnalysisPlugin.PITEST_JAVA_SENSOR_ENABLED, false)
                    .set(MutationAnalysisPlugin.PITEST_KOTLIN_SENSOR_ENABLED, false);
   }
}
