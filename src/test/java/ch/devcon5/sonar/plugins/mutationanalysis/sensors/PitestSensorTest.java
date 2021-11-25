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
import static ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin.EXPERIMENTAL_FEATURE_ENABLED;
import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.MUTATIONS_KILLED_KEY;
import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.MUTATIONS_TOTAL_KEY;
import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.TEST_KILLS_KEY;
import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.UTILITY_GLOBAL_MUTATIONS_KEY;
import static ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition.PARAM_MUTANT_COVERAGE_THRESHOLD;
import static ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition.REPOSITORY_KEY;
import static ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition.RULE_SURVIVED_MUTANT;
import static ch.devcon5.sonar.plugins.mutationanalysis.testharness.TestUtils.assertContains;
import static ch.devcon5.sonar.plugins.mutationanalysis.testharness.TestUtils.assertNotContains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin;
import ch.devcon5.sonar.plugins.mutationanalysis.testharness.SensorTestHarness;
import ch.devcon5.sonar.plugins.mutationanalysis.testharness.SystemLocale;
import ch.devcon5.sonar.plugins.mutationanalysis.testharness.TestSensorContext;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.internal.DefaultSensorDescriptor;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.measure.Measure;

//kills 68 mutants, 11 alive
public class PitestSensorTest {

   public static final int EXPECTED_QUANTITATIVE_METRICS = 12;
   @Rule
   public TemporaryFolder folder = new TemporaryFolder();

   private SensorTestHarness harness;

   @ClassRule
   public static SystemLocale systemLocale = SystemLocale.overrideDefault(Locale.ENGLISH);

   @Before
   public void setUp() throws Exception {
      this.harness = SensorTestHarness.builder().withTempFolder(folder).withResourceResolver(this.getClass()).build();
   }
   @Test
   public void describe() {
      final PitestSensor sensor = new PitestSensor(harness.createConfiguration(), harness.createEmptyActiveRules(),
                                                   harness.createSensorContext().fileSystem());

      final DefaultSensorDescriptor desc = new DefaultSensorDescriptor();

      sensor.describe(desc);

      assertEquals("Mutation Analysis", desc.name());

      assertTrue(desc.languages().contains("java"));
      assertTrue(desc.ruleRepositories().contains(REPOSITORY_KEY + ".java"));

      assertTrue(desc.languages().contains("kotlin"));
      assertTrue(desc.ruleRepositories().contains(REPOSITORY_KEY + ".kotlin"));

      //the following two assertion block target at killing mutants introduced
      //by the javac compiler for vargs.
      //these provide no extra value as the order is actually unimportant
      //and any reordering on the arguments done by pit is irrelevant
      Iterator<String> langIt = desc.languages().iterator();
      assertEquals("java", langIt.next());
      assertEquals("kotlin", langIt.next());

      Iterator<String> repoIt = desc.ruleRepositories().iterator();
      assertEquals(REPOSITORY_KEY + ".java", repoIt.next());
      assertEquals(REPOSITORY_KEY + ".kotlin", repoIt.next());
   }

   @Test
   public void testToString() throws Exception {

      final PitestSensor sensor = new PitestSensor(harness.createConfiguration(), harness.createEmptyActiveRules(), harness
          .createSensorContext().fileSystem());

      assertEquals("PitestSensor", sensor.toString());
   }

   @Test
   public void execute_AllSensorsDisabled_noIssuesOrMeasuresCreated() throws IOException {

      //arrange
      final TestSensorContext context = disableBothSensors(harness.createSensorContext());
      createReportFile("PitestSensorTest_KotlinJava_mutations.xml");
      final ActiveRules profile = harness.createActiveRules(
          harness.createRule("java",RULE_SURVIVED_MUTANT),
          harness.createRule("kotlin",RULE_SURVIVED_MUTANT)
          );
      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/JavaExample.java", md -> md.lines = 200);
      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/KotlinExample.kt", md -> md.lines = 200);

      //act
      final PitestSensor sensor = new PitestSensor(context.getConfiguration(), profile, context.fileSystem());
      sensor.execute(context);

      //assert
      assertTrue(context.getStorage().getIssues().isEmpty());
      assertTrue(context.getStorage().getMeasures().isEmpty());
   }

   @Test
   public void execute_JavaSensorsDisabled_noJavaIssuesOrMeasuresCreated() throws IOException {

      //arrange
      final TestSensorContext context = disableJavaSensor(harness.createSensorContext());
      createReportFile("PitestSensorTest_KotlinJava_mutations.xml");
      final ActiveRules profile = harness.createActiveRules(
          harness.createRule("java","mutant.survived"),
          harness.createRule("kotlin","mutant.survived")
      );

      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/JavaExample.java", md -> md.lines = 200);
      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/KotlinExample.kt", md -> md.lines = 200);

      //act
      final PitestSensor sensor = new PitestSensor(context.getConfiguration(), profile, context.fileSystem());
      sensor.execute(context);

      //assert
      assertFalse(context.getStorage().getMeasures().isEmpty());
      final List<Issue> issues = context.getStorage().getIssues();
      assertContains(issues, i -> {
         assertEquals("mutant.survived", i.ruleKey().rule());
         assertEquals("test-module:src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/KotlinExample.kt", i.primaryLocation().inputComponent().key());
         assertEquals(1.0, i.gap(), 0.01);
         assertTextrangeOnLine(i.primaryLocation().textRange(), 28, 79);
         assertEquals("Alive Mutant: A conditional expression has been negated without being detected by a test. Mutation: negated conditional", i.primaryLocation().message());
      });
   }

   @Test
   public void execute_KotlinSensorDisabled_noJavaIssuesOrMeasuresCreated() throws IOException {

      //arrange
      final TestSensorContext context = disableKotlingSensor(harness.createSensorContext());
      createReportFile("PitestSensorTest_KotlinJava_mutations.xml");
      final ActiveRules profile = harness.createActiveRules(
          harness.createRule("java",RULE_SURVIVED_MUTANT),
          harness.createRule("kotlin",RULE_SURVIVED_MUTANT)
      );
      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/JavaExample.java", md -> md.lines = 200);
      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/KotlinExample.kt", md -> md.lines = 200);

      //act
      final PitestSensor sensor = new PitestSensor(context.getConfiguration(), profile, context.fileSystem());
      sensor.execute(context);

      //assert
      assertFalse(context.getStorage().getMeasures().isEmpty());
      final List<Issue> issues = context.getStorage().getIssues();
      assertContains(issues, i -> {
         assertEquals("mutant.survived", i.ruleKey().rule());
         assertEquals("test-module:src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/JavaExample.java", i.primaryLocation().inputComponent().key());
         assertEquals(1.0, i.gap(), 0.01);
         assertTextrangeOnLine(i.primaryLocation().textRange(), 162, 79);
         assertEquals("Alive Mutant: A conditional expression has been negated without being detected by a test.", i.primaryLocation().message());
      });
   }


   @Test
   public void execute_noTestFile_noIssuesAndMeasuresCreated() throws Exception {

      createReportFile("PitestSensorTest_Java_mutations.xml");
      final ActiveRules profile = harness.createActiveRules(RULE_SURVIVED_MUTANT);
      final TestSensorContext context = harness.createSensorContext();

      final PitestSensor sensor = new PitestSensor(context.getConfiguration(), profile, context.fileSystem());

      sensor.execute(context);

      assertTrue(context.getStorage().getIssues().isEmpty());
      assertTrue(context.getStorage().getMeasures().isEmpty());

   }

   @Test
   public void execute_noReport_noIssuesAndMeasuresCreated() throws Exception {

      final ActiveRules profile = harness.createActiveRules(RULE_SURVIVED_MUTANT);
      final TestSensorContext context = harness.createSensorContext();
      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", md -> md.lines = 200);

      final PitestSensor sensor = new PitestSensor(context.getConfiguration(), profile, context.fileSystem());

      sensor.execute(context);

      assertTrue(context.getStorage().getIssues().isEmpty());
      assertTrue(context.getStorage().getMeasures().isEmpty());

   }


   @Test
   public void execute_noRuleActivated_noIssuesCreated_but_MetricsCreated() throws Exception {

      createReportFile("PitestSensorTest_Java_mutations.xml");
      final TestSensorContext context = harness.createSensorContext().scanFiles();
      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", md -> md.lines = 200);
      final ActiveRules profile = harness.createEmptyActiveRules();

      //sensor is configured by default
      final PitestSensor sensor = new PitestSensor(context.getConfiguration(), profile, context.fileSystem());

      sensor.execute(context);

      assertTrue(context.getStorage().getIssues().isEmpty());
      assertEquals(12, context.getStorage().getMeasures().size());
   }


   @Test
   public void execute_mutatorSpecificRuleActive_issueCreated() throws Exception {

      createReportFile("PitestSensorTest_Java_mutations.xml");

      final TestSensorContext context = harness.createSensorContext().scanFiles();
      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", md -> md.lines = 200);

      final ActiveRules profile = harness.createActiveRules("mutant.NEGATE_CONDITIONALS");

      //sensor is configured by default
      final PitestSensor sensor = new PitestSensor(context.getConfiguration(), profile, context.fileSystem());

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
      assertEquals(12, context.getStorage().getMeasures().size());
   }

   @Test
   public void execute_survivedMutantRuleActive_issueCreated() throws Exception {

      createReportFile("PitestSensorTest_Java_mutations.xml");
      final TestSensorContext context = harness.createSensorContext().scanFiles();
      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", md -> md.lines = 200);

      final ActiveRules profile = harness.createActiveRules("mutant.uncovered");
      final PitestSensor sensor = new PitestSensor(context.getConfiguration(), profile, context.fileSystem());

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
      assertEquals(12, context.getStorage().getMeasures().size());

   }

   @Test
   public void execute_unknownMutantStatusRuleActive_issueCreated() throws Exception {

      createReportFile("PitestSensorTest_Java_mutations.xml");
      final TestSensorContext context = harness.createSensorContext().scanFiles();
      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", md -> md.lines = 200);
      final ActiveRules profile = harness.createActiveRules("mutant.unknownStatus");
      final PitestSensor sensor = new PitestSensor(context.getConfiguration(), profile, context.fileSystem());

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
      assertEquals(EXPECTED_QUANTITATIVE_METRICS, context.getStorage().getMeasures().size());

   }

   @Test
   public void execute_coverageThresholdRuleActive_belowThreshold_twoMutantMissing() throws Exception {

      createReportFile("PitestSensorTest_Java_mutations.xml");
      final TestSensorContext context = harness.createSensorContext().scanFiles();
      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", md -> md.lines = 200);
      context.setConfiguration(EFFORT_FACTOR_MISSING_COVERAGE, 2.0);

      final ActiveRules profile = harness.createActiveRules(harness.createRule("mutant.coverage", PARAM_MUTANT_COVERAGE_THRESHOLD, "66.6"));
      final PitestSensor sensor = new PitestSensor(context.getConfiguration(), profile, context.fileSystem());

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

      createReportFile("PitestSensorTest_Java_mutations.xml");
      final TestSensorContext context = harness.createSensorContext().scanFiles();
      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", md -> md.lines = 200);
      context.setConfiguration(EFFORT_FACTOR_MISSING_COVERAGE, 2.0);
      final ActiveRules profile = harness.createActiveRules(harness.createRule("mutant.coverage", PARAM_MUTANT_COVERAGE_THRESHOLD, "80.0"));
      final PitestSensor sensor = new PitestSensor(context.getConfiguration(), profile, context.fileSystem());

      sensor.execute(context);

      final List<Issue> issues = context.getStorage().getIssues();
      assertEquals(1, issues.size());
      assertContains(issues, i -> {
         assertEquals("mutant.coverage", i.ruleKey().rule());
         assertEquals("test-module:src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", i.primaryLocation().inputComponent().key());
         assertEquals(4.0, i.gap(), 0.01);
         assertEquals("2 more mutants need to be killed to get the mutation coverage from 50.0% to 80.0%", i.primaryLocation().message());
      });

      assertCoverage(50.0, context.getStorage().getMeasures());
   }

   @Test
   public void execute_coverageThresholdRuleActive_aboveThreshold_noIssue() throws Exception {

      createReportFile("PitestSensorTest_Java_mutations.xml");
      final TestSensorContext context = harness.createSensorContext().scanFiles();
      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", md -> md.lines = 200);
      final ActiveRules profile = harness.createActiveRules(harness.createRule("mutant.coverage", PARAM_MUTANT_COVERAGE_THRESHOLD, "40.0"));
      final PitestSensor sensor = new PitestSensor(context.getConfiguration(), profile, context.fileSystem());

      sensor.execute(context);

      assertTrue(context.getStorage().getIssues().isEmpty());

   }

   @Test
   public void execute_coverageThresholdRuleActive_onThreshold_noIssue() throws Exception {

      createReportFile("PitestSensorTest_Java_mutations.xml");
      final TestSensorContext context = harness.createSensorContext().scanFiles();
      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", md -> md.lines = 200);
      context.setConfiguration(EFFORT_FACTOR_MISSING_COVERAGE, 2.0);
      final ActiveRules profile = harness.createActiveRules(harness.createRule("mutant.coverage", PARAM_MUTANT_COVERAGE_THRESHOLD, "50.0"));
      final PitestSensor sensor = new PitestSensor(context.getConfiguration(), profile, context.fileSystem());

      sensor.execute(context);

      assertTrue(context.getStorage().getIssues().isEmpty());
      assertCoverage(50.0, context.getStorage().getMeasures());
   }

   @Test
   public void execute_withExperimentalFeaturesEnable_TestMetricsCreated() throws Exception {

      createReportFile("PitestSensorTest_Java_mutations.xml");

      final TestSensorContext context = harness.createSensorContext().scanFiles();
      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", md -> md.lines = 200);
      context.addTestFile("src/test/java/ch/devcon5/sonar/plugins/mutationanalysis/model/MutantTest.java");
      context.setConfiguration(EXPERIMENTAL_FEATURE_ENABLED, "true");

      final PitestSensor sensor = new PitestSensor(context.getConfiguration(), harness.createEmptyActiveRules(), context
          .fileSystem());

      sensor.execute(context);

      final List<Measure> measures = context.getStorage().getMeasures();
      assertEquals(EXPECTED_QUANTITATIVE_METRICS +2, measures.size());
      assertEquals(3, assertContains(measures, m -> assertEquals(TEST_KILLS_KEY, m.metric().key())).value());
      assertEquals(6, assertContains(measures, m -> assertEquals(UTILITY_GLOBAL_MUTATIONS_KEY, m.metric().key())).value());
   }

   @Test
   public void execute_withoutExperimentalFeaturesEnable_noTestMetricsCreated() throws Exception {

      createReportFile("PitestSensorTest_Java_mutations.xml");

      final TestSensorContext context = harness.createSensorContext().scanFiles();
      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/model/Mutant.java", md -> md.lines = 200);
      context.addTestFile("src/test/java/ch/devcon5/sonar/plugins/mutationanalysis/model/MutantTest.java");
      context.setConfiguration(EXPERIMENTAL_FEATURE_ENABLED, "false");

      //sensor is configured by default
      final PitestSensor sensor = new PitestSensor(context.getConfiguration(), harness.createEmptyActiveRules(), context.fileSystem());

      sensor.execute(context);

      final List<Measure> measures = context.getStorage().getMeasures();
      assertEquals(12, measures.size());
      assertNotContains(measures, m -> assertEquals(TEST_KILLS_KEY, m.metric().key()));
      assertNotContains(measures, m -> assertEquals(UTILITY_GLOBAL_MUTATIONS_KEY, m.metric().key()));
   }

   @Test
   public void execute_onlyKotlinSensor_issueCreated() throws Exception {

      this.harness = harness.changeLanguage("kotlin");
      createReportFile("PitestSensorTest_Kotlin_mutations.xml");
      final TestSensorContext context = disableJavaSensor(harness.createSensorContext().scanFiles());
      context.addTestFile("src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/KotlinExample.kt", md -> md.lines = 200);

      final ActiveRules profile = harness.createActiveRules("mutant.uncovered");
      final PitestSensor sensor = new PitestSensor(context.getConfiguration(), profile, context.fileSystem());

      sensor.execute(context);

      final List<Issue> issues = context.getStorage().getIssues();
      assertFalse(issues.isEmpty());
      assertContains(issues, i -> {
         assertEquals("mutant.uncovered", i.ruleKey().rule());
         assertEquals("test-module:src/main/java/ch/devcon5/sonar/plugins/mutationanalysis/KotlinExample.kt", i.primaryLocation().inputComponent().key());
         assertEquals(1.0, i.gap(), 0.01);
         assertTextrangeOnLine(i.primaryLocation().textRange(), 28, 79);
         assertEquals("Alive Mutant: A conditional expression has been negated without being detected by a test. Mutation: negated conditional", i.primaryLocation().message());
      });
      assertEquals(12, context.getStorage().getMeasures().size());

   }

   private void createReportFile(String reportFile) throws IOException {

      harness.resourceToFile("target/pit-reports/mutations.xml", reportFile);
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


   private TestSensorContext disableBothSensors(final TestSensorContext context) {
      return context.setConfiguration(MutationAnalysisPlugin.PITEST_JAVA_SENSOR_ENABLED, false)
                    .setConfiguration(MutationAnalysisPlugin.PITEST_KOTLIN_SENSOR_ENABLED, false);
   }
   private TestSensorContext disableJavaSensor(final TestSensorContext context) {
      return context.setConfiguration(MutationAnalysisPlugin.PITEST_JAVA_SENSOR_ENABLED, false);
   }
   private TestSensorContext disableKotlingSensor(final TestSensorContext context) {
      return context.setConfiguration(MutationAnalysisPlugin.PITEST_KOTLIN_SENSOR_ENABLED, false);
   }
}
