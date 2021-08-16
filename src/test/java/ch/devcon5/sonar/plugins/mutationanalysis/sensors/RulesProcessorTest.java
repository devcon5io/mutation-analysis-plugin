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
import static ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin.EFFORT_FACTOR_SURVIVED_MUTANT;
import static ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import ch.devcon5.sonar.plugins.mutationanalysis.metrics.ResourceMutationMetrics;
import ch.devcon5.sonar.plugins.mutationanalysis.model.MutationOperator;
import ch.devcon5.sonar.plugins.mutationanalysis.model.MutationOperators;
import ch.devcon5.sonar.plugins.mutationanalysis.rules.JavaRulesDefinition;
import ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition;
import ch.devcon5.sonar.plugins.mutationanalysis.testharness.LogRecordingAppender;
import ch.devcon5.sonar.plugins.mutationanalysis.testharness.SensorTestHarness;
import ch.devcon5.sonar.plugins.mutationanalysis.testharness.TestConfiguration;
import ch.devcon5.sonar.plugins.mutationanalysis.testharness.TestSensorContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.rules.Rule;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;

/**
 *
 */
public class RulesProcessorTest {

   private static final MutationOperator[] MUTATION_OPERATORS = MutationOperators.allMutationOperators().toArray(new MutationOperator[0]);

   @org.junit.Rule
   public TemporaryFolder folder = new TemporaryFolder();

   private TestConfiguration configuration;
   private MutationAnalysisRulesDefinition def;
   private RulesDefinition.Context rulesContext;

   private SensorTestHarness harness;
   private LogRecordingAppender appender;

   @Before
   public void setUp() throws Exception {

      this.harness = SensorTestHarness.builder().withTempFolder(folder).build();
      this.configuration = this.harness.createConfiguration();

      RulesDefinitionXmlLoader rulesLoader = new RulesDefinitionXmlLoader();
      this.def = new JavaRulesDefinition(configuration, rulesLoader);
      this.rulesContext = new RulesDefinition.Context();
      this.def.define(rulesContext);

      appender = new LogRecordingAppender();
   }

   @After
   public void tearDown() throws Exception {
      appender.close();
   }

   @Test
   public void processRules_noRules_withMetrics_noIssues() throws Exception {

      //arrange
      final TestSensorContext context = harness.createSensorContext();
      final ActiveRules profile = harness.createEmptyActiveRules();
      final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
         md.lines = 100;
         md.mutants.survived = 3;
         md.mutants.killed = 9;
      }));

      //act
      final RulesProcessor processor = new RulesProcessor(configuration, profile);
      processor.processRules(metrics, context, "java");

      //assert
      final List<Issue> issues = context.getStorage().getIssues();
      assertTrue(issues.isEmpty());

      final List<LogEvent> events = appender.getEvents();
      assertTrue(events.stream()
          .filter(e -> e.getLevel() == Level.WARN)
          .map(e -> e.getMessage().getFormattedMessage())
          .anyMatch(
              "/!\\ At least one Mutation Analysis rule needs to be activated for the current profile and language: java."::equals));

   }

   @Test
   public void processRules_survivorRuleActive_defaultEffortFactor_survivedMutant_issueCreated() {

      //arrange
      final TestSensorContext context = harness.createSensorContext();
      final ActiveRules profile = harness.createActiveRules(RULE_SURVIVED_MUTANT);

      final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
         md.lines = 100;
         md.mutants.survived = 3;
         md.mutants.killed = 9;
      }));

      //act
      final RulesProcessor processor = new RulesProcessor(configuration, profile);
      processor.processRules(metrics, context, "java");

      //assert
      final List<Issue> issues = context.getStorage().getIssues();
      assertIssueAtLine(issues.get(0), RULE_SURVIVED_MUTANT, "test-module:Test.java", 3, 1.0);
      assertIssueAtLine(issues.get(1), RULE_SURVIVED_MUTANT, "test-module:Test.java", 4, 1.0);
      assertIssueAtLine(issues.get(2), RULE_SURVIVED_MUTANT, "test-module:Test.java", 5, 1.0);
      assertEquals(3, issues.size());

      assertTrue(appender.getEvents().isEmpty());
   }

   @Test
   public void processRules_survivorRuleActive_defaultEffortFactor_survivedMutant_withDescription_issueCreated() {

      //arrange
      final TestSensorContext context = harness.createSensorContext();
      final ActiveRules profile = harness.createActiveRules(RULE_SURVIVED_MUTANT);
      final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
         md.lines = 100;
         md.mutants.survived = 3;
         md.mutants.killed = 9;
         md.mutants.description = "substituted foo with bar";
      }));

      //act
      final RulesProcessor processor = new RulesProcessor(configuration, profile);
      processor.processRules(metrics, context, "java");

      //assert
      final List<Issue> issues = context.getStorage().getIssues();
      assertIssueAtLine(issues.get(0), RULE_SURVIVED_MUTANT, "test-module:Test.java", 3, 1.0, " Mutation: substituted foo with bar");
      assertIssueAtLine(issues.get(1), RULE_SURVIVED_MUTANT, "test-module:Test.java", 4, 1.0, " Mutation: substituted foo with bar");
      assertIssueAtLine(issues.get(2), RULE_SURVIVED_MUTANT, "test-module:Test.java", 5, 1.0, " Mutation: substituted foo with bar");
      assertEquals(3, issues.size());
      assertTrue(appender.getEvents().isEmpty());
   }

   @Test
   public void processRules_survivorRuleActive_customEffortFactor_survivedMutant_issueCreated() {

      //arrange
      configuration.set(EFFORT_FACTOR_SURVIVED_MUTANT, 10.0);
      final TestSensorContext context = harness.createSensorContext();
      final ActiveRules profile = harness.createActiveRules(RULE_SURVIVED_MUTANT);
      final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
         md.lines = 100;
         md.mutants.survived = 3;
         md.mutants.killed = 9;
      }));

      //act
      final RulesProcessor processor = new RulesProcessor(configuration, profile);
      processor.processRules(metrics, context, "java");

      //assert
      final List<Issue> issues = context.getStorage().getIssues();
      assertEquals(3, issues.size());
      assertIssueAtLine(issues.get(0), RULE_SURVIVED_MUTANT, "test-module:Test.java", 3, 10.0);
      assertIssueAtLine(issues.get(1), RULE_SURVIVED_MUTANT, "test-module:Test.java", 4, 10.0);
      assertIssueAtLine(issues.get(2), RULE_SURVIVED_MUTANT, "test-module:Test.java", 5, 10.0);
      assertTrue(appender.getEvents().isEmpty());
   }

   @Test
   public void processRules_uncoveredRuleActive_defaultEffortFactor_uncoveredMutant_issueCreated() {

      //arrange
      final TestSensorContext context = harness.createSensorContext();
      final ActiveRules profile = harness.createActiveRules(RULE_UNCOVERED_MUTANT);
      final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
         md.lines = 100;
         md.mutants.noCoverage = 2;
         md.mutants.killed = 9;
      }));

      //act
      final RulesProcessor processor = new RulesProcessor(configuration, profile);
      processor.processRules(metrics, context, "java");

      //assert
      final List<Issue> issues = context.getStorage().getIssues();
      assertIssueAtLine(issues.get(0), RULE_UNCOVERED_MUTANT, "test-module:Test.java", 2, 1.0);
      assertIssueAtLine(issues.get(1), RULE_UNCOVERED_MUTANT, "test-module:Test.java", 3, 1.0);
      assertEquals(2, issues.size());
      assertTrue(appender.getEvents().isEmpty());
   }

   @Test
   public void processRules_uncoveredRuleActive_customEffortFactor_uncoveredMutant_issueCreated() {

      //arrange
      configuration.set(EFFORT_FACTOR_SURVIVED_MUTANT, 10.0);
      final TestSensorContext context = harness.createSensorContext();
      final ActiveRules profile = harness.createActiveRules(RULE_UNCOVERED_MUTANT);
      final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
         md.lines = 100;
         md.mutants.noCoverage = 2;
         md.mutants.killed = 9;
      }));

      //act
      final RulesProcessor processor = new RulesProcessor(configuration, profile);
      processor.processRules(metrics, context, "java");

      //assert
      final List<Issue> issues = context.getStorage().getIssues();
      assertIssueAtLine(issues.get(0), RULE_UNCOVERED_MUTANT, "test-module:Test.java", 2, 10.0);
      assertIssueAtLine(issues.get(1), RULE_UNCOVERED_MUTANT, "test-module:Test.java", 3, 10.0);
      assertEquals(2, issues.size());
      assertTrue(appender.getEvents().isEmpty());
   }

   @Test
   public void processRules_unknownMutationStatusRuleActive_defaultEffortFactor_unknownMutationState_issueCreated() {

      //arrange
      final TestSensorContext context = harness.createSensorContext();
      final ActiveRules profile = harness.createActiveRules(RULE_UNKNOWN_MUTANT_STATUS);
      final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
         md.lines = 100;
         md.mutants.unknown = 2;
         md.mutants.killed = 9;
      }));

      //act
      final RulesProcessor processor = new RulesProcessor(configuration, profile);
      processor.processRules(metrics, context, "java");

      //assert
      final List<Issue> issues = context.getStorage().getIssues();
      assertIssueAtLine(issues.get(0), RULE_UNKNOWN_MUTANT_STATUS, "test-module:Test.java", 2, 1.0);
      assertIssueAtLine(issues.get(1), RULE_UNKNOWN_MUTANT_STATUS, "test-module:Test.java", 3, 1.0);
      assertEquals(2, issues.size());
      assertTrue(appender.getEvents().isEmpty());
   }

   @Test
   public void processRules_unknownMutationStatusRuleActive_customEffortFactor_unknownMutationState_issueCreated() {

      //arrange
      configuration.set(EFFORT_FACTOR_SURVIVED_MUTANT, 10.0);
      final TestSensorContext context = harness.createSensorContext();
      final ActiveRules profile = harness.createActiveRules(RULE_UNKNOWN_MUTANT_STATUS);
      final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
         md.lines = 100;
         md.mutants.unknown = 2;
         md.mutants.killed = 9;
      }));

      //act
      final RulesProcessor processor = new RulesProcessor(configuration, profile);
      processor.processRules(metrics, context, "java");

      //assert
      final List<Issue> issues = context.getStorage().getIssues();
      assertIssueAtLine(issues.get(0), RULE_UNKNOWN_MUTANT_STATUS, "test-module:Test.java", 2, 10.0);
      assertIssueAtLine(issues.get(1), RULE_UNKNOWN_MUTANT_STATUS, "test-module:Test.java", 3, 10.0);
      assertEquals(2, issues.size());
      assertTrue(appender.getEvents().isEmpty());
   }

   @Test
   public void processRules_mutatorRuleActive_defaultEffortFactor_mutantSurvived_issueCreated() {

      //arrange
      final TestSensorContext context = harness.createSensorContext();
      final ActiveRules profile = harness.createActiveRules(createMutationOperatorRules());
      final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
         md.lines = 100;
         md.mutants.survived = MUTATION_OPERATORS.length;
         md.mutants.killed = 5;
      }));

      //act
      final RulesProcessor processor = new RulesProcessor(configuration, profile);
      processor.processRules(metrics, context, "java");

      //assert
      final List<Issue> issues = context.getStorage().getIssues();
      assertEquals(23, issues.size());
      for (int i = 0, len = MUTATION_OPERATORS.length; i < len; i++) {
         assertIssueAtLine(issues.get(i), "mutant." + MUTATION_OPERATORS[i].getId(), "test-module:Test.java", MUTATION_OPERATORS.length + i, 1.0);
      }
      assertTrue(appender.getEvents().isEmpty());
   }

   @Test
   public void processRules_coverageThresholdRuleActive_defaultEffortFactor_coverageTooLow_issueCreated() {

      //arrange
      final TestSensorContext context = harness.createSensorContext();
      final ActiveRules profile = harness.createActiveRules(harness.createRule(RULE_MUTANT_COVERAGE,
                                                                      PARAM_MUTANT_COVERAGE_THRESHOLD, "80"));
      final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
         md.mutants.survived = 3;
         md.mutants.killed = 9;
      }));

      //act
      final RulesProcessor processor = new RulesProcessor(configuration, profile);
      processor.processRules(metrics, context, "java");

      //assert
      final Issue issue = context.getStorage().getIssues().get(0);
      assertIssueAtLine(issue, RULE_MUTANT_COVERAGE, "test-module:Test.java", 1.0, "1 more mutants need to be killed to get the mutation coverage from 75.0% to 80.0%");
      assertTrue(appender.getEvents().isEmpty());
   }

   @Test
   public void processRules_coverageThresholdRuleActive_customEffortFactor_coverageTooLow_issueCreated() {

      //arrange
      configuration.set(EFFORT_FACTOR_MISSING_COVERAGE, 6.0);
      final TestSensorContext context = harness.createSensorContext();
      final ActiveRules profile = harness.createActiveRules(harness.createRule(RULE_MUTANT_COVERAGE,
                                                                      PARAM_MUTANT_COVERAGE_THRESHOLD, "80"));
      final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
         md.mutants.survived = 3;
         md.mutants.killed = 9;
      }));

      //act
      final RulesProcessor processor = new RulesProcessor(configuration, profile);
      processor.processRules(metrics, context, "java");

      //assert
      final Issue issue = context.getStorage().getIssues().get(0);
      assertIssueAtLine(issue, RULE_MUTANT_COVERAGE, "test-module:Test.java", 6.0, "1 more mutants need to be killed to get the mutation coverage from 75.0% to 80.0%");
      assertTrue(appender.getEvents().isEmpty());
   }

  @Test
  public void processRules_coverageThresholdRuleInactive_defaultEffortFactor_coverageThresholdMissed_noIssueCreated() {

    //arrange
    final TestSensorContext context = harness.createSensorContext();
    final ActiveRules profile = harness.createActiveRules("some.rule");
    final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
      md.mutants.survived = 2;
      md.mutants.killed = 8;
    }));

    //act
    final RulesProcessor processor = new RulesProcessor(configuration, profile);
    processor.processRules(metrics, context, "java");

    //assert
    assertTrue(context.getStorage().getIssues().isEmpty());
    assertTrue(appender.getEvents().isEmpty());
  }

   @Test
   public void processRules_coverageThresholdRuleActive_defaultEffortFactor_coverageThresholdHit_noIssueCreated() {

      //arrange
      final TestSensorContext context = harness.createSensorContext();
      final ActiveRules profile = harness.createActiveRules(harness.createRule(RULE_MUTANT_COVERAGE,
                                                                      PARAM_MUTANT_COVERAGE_THRESHOLD, "80"));
      final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
         md.mutants.survived = 2;
         md.mutants.killed = 8;
      }));

      //act
      final RulesProcessor processor = new RulesProcessor(configuration, profile);
      processor.processRules(metrics, context, "java");

      //assert
      assertTrue(context.getStorage().getIssues().isEmpty());
      assertTrue(appender.getEvents().isEmpty());
   }

  @Test
  public void processRules_coverageThresholdRuleActive_defaultThreshold_noIssueCreated() {

    //arrange
    final TestSensorContext context = harness.createSensorContext();
    final ActiveRules profile = harness.createActiveRules(harness.createRule(RULE_MUTANT_COVERAGE));
    final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
      md.mutants.survived = 2;
      md.mutants.killed = 8;
    }));

    //act
    final RulesProcessor processor = new RulesProcessor(configuration, profile);
    processor.processRules(metrics, context, "java");

    //assert
    assertTrue(context.getStorage().getIssues().isEmpty());
    assertTrue(appender.getEvents().isEmpty());
  }

   @Test
   public void processRules_coverageThresholdRuleActive_gapIsSamllerThan05_RoundedUp_issueCreated() {

      //arrange
      final TestSensorContext context = harness.createSensorContext();
      final ActiveRules profile = harness.createActiveRules(harness.createRule(RULE_MUTANT_COVERAGE,
                                                                      PARAM_MUTANT_COVERAGE_THRESHOLD, "80"));
      final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
         md.mutants.survived = 2;
         md.mutants.killed = 7;
      }));

      //act
      final RulesProcessor processor = new RulesProcessor(configuration, profile);
      processor.processRules(metrics, context, "java");

      //assert
      final Issue issue = context.getStorage().getIssues().get(0);
      assertIssueAtLine(issue, RULE_MUTANT_COVERAGE, "test-module:Test.java", 1.0, "1 more mutants need to be killed to get the mutation coverage from 77.8% to 80.0%");
      assertTrue(appender.getEvents().isEmpty());
   }

   private Rule[] createMutationOperatorRules() {

      return Arrays.stream(MUTATION_OPERATORS).map(o -> harness.createRule("mutant." + o.getId())).toArray(Rule[]::new);
   }

   private void assertIssueAtLine(final Issue issue, String ruleKey, String componentName, final double gap, String message) {

      assertIssueAtLine(issue, ruleKey, componentName, -1, gap, message);
   }

   private void assertIssueAtLine(final Issue issue, String ruleKey, String componentName, int line, final double gap) {
      assertIssueAtLine(issue, ruleKey, componentName, line, gap, null);
   }

   private void assertIssueAtLine(final Issue issue, String ruleKey, String componentName, int line, final double gap, String description) {

      assertEquals(ruleKey, issue.ruleKey().rule());
      assertEquals(componentName, issue.primaryLocation().inputComponent().key());
      assertEquals(gap, issue.gap(), 0.05);

      final String message;

      if (line != -1) {
         assertEquals(line, issue.primaryLocation().textRange().start().line());
         message = TestSensorContext.getMutationOperatorForLine(line).getViolationDescription() + optionalDescription(description);
      } else {
         assertNull("issue is not expected to have a textrange", issue.primaryLocation().textRange());
         message = optionalDescription(description);
      }

      assertEquals(message, issue.primaryLocation().message());
   }

   private String optionalDescription(final String description) {
      return description == null ? "" : description;
   }

}
