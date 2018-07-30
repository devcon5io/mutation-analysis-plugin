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
import static ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin.EFFORT_MUTANT_KILL;
import static ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.sonar.api.rules.RulePriority.MAJOR;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import ch.devcon5.sonar.plugins.mutationanalysis.metrics.ResourceMutationMetrics;
import ch.devcon5.sonar.plugins.mutationanalysis.model.MutationOperator;
import ch.devcon5.sonar.plugins.mutationanalysis.model.MutationOperators;
import ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.config.Configuration;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class RulesProcessorTest {

  private static final MutationOperator[] MUTATION_OPERATORS = MutationOperators.allMutagens().toArray(new MutationOperator[0]);
  @org.junit.Rule
  public TemporaryFolder folder = new TemporaryFolder();
  @Mock
  private Configuration configuration;
  private MutationAnalysisRulesDefinition def;
  private RulesDefinition.Context rulesContext;

  @Before
  public void setUp() throws Exception {

    when(configuration.get(EFFORT_MUTANT_KILL)).thenReturn(Optional.empty());

    RulesDefinitionXmlLoader rulesLoader = new RulesDefinitionXmlLoader();
    this.def = new MutationAnalysisRulesDefinition(configuration, rulesLoader);
    this.rulesContext = new RulesDefinition.Context();
    this.def.define(rulesContext);
  }

  @Test
  public void processRules_noRules_noMetrics_noIssues() {

    final TestSensorContext context = TestSensorContext.create(folder.getRoot().toPath(), "test-module");
    final RulesProfile profile = RulesProfile.create("test.profile", "java");
    final Collection<ResourceMutationMetrics> metrics = Collections.emptyList();

    final RulesProcessor processor = new RulesProcessor(configuration, profile);

    processor.processRules(metrics, context);

    final List<Issue> issues = context.getStorage().getIssues();
    assertTrue(issues.isEmpty());
  }

  @Test
  public void processRules_survivorRuleActive_defaultEffortFactor_survivedMutant_issueCreated() {

    when(configuration.getDouble(EFFORT_FACTOR_SURVIVED_MUTANT)).thenReturn(Optional.empty());

    final TestSensorContext context = TestSensorContext.create(folder.getRoot().toPath(), "test-module");
    final RulesProfile profile = createRulesProfile(createRule(RULE_SURVIVED_MUTANT));
    final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
      md.lines = 100;
      md.mutants.survived = 3;
      md.mutants.killed = 9;
    }));

    final RulesProcessor processor = new RulesProcessor(configuration, profile);

    processor.processRules(metrics, context);

    final List<Issue> issues = context.getStorage().getIssues();
    assertIssueAtLine(issues.get(0), RULE_SURVIVED_MUTANT, "test-module:Test.java", 3, 1.0);
    assertIssueAtLine(issues.get(1), RULE_SURVIVED_MUTANT, "test-module:Test.java", 4, 1.0);
    assertIssueAtLine(issues.get(2), RULE_SURVIVED_MUTANT, "test-module:Test.java", 5, 1.0);
    assertEquals(3, issues.size());
  }

  @Test
  public void processRules_survivorRuleActive_customEffortFactor_survivedMutant_issueCreated() {

    when(configuration.getDouble(EFFORT_FACTOR_SURVIVED_MUTANT)).thenReturn(Optional.of(10.0));

    final TestSensorContext context = TestSensorContext.create(folder.getRoot().toPath(), "test-module");
    final RulesProfile profile = createRulesProfile(createRule(RULE_SURVIVED_MUTANT));
    final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
      md.lines = 100;
      md.mutants.survived = 3;
      md.mutants.killed = 9;
    }));

    final RulesProcessor processor = new RulesProcessor(configuration, profile);

    processor.processRules(metrics, context);

    final List<Issue> issues = context.getStorage().getIssues();
    assertEquals(3, issues.size());
    assertIssueAtLine(issues.get(0), RULE_SURVIVED_MUTANT, "test-module:Test.java", 3, 10.0);
    assertIssueAtLine(issues.get(1), RULE_SURVIVED_MUTANT, "test-module:Test.java", 4, 10.0);
    assertIssueAtLine(issues.get(2), RULE_SURVIVED_MUTANT, "test-module:Test.java", 5, 10.0);
  }

  @Test
  public void processRules_uncoveredRuleActive_defaultEffortFactor_uncoveredMutant_issueCreated() {

    when(configuration.getDouble(EFFORT_FACTOR_SURVIVED_MUTANT)).thenReturn(Optional.empty());

    final TestSensorContext context = TestSensorContext.create(folder.getRoot().toPath(), "test-module");
    final RulesProfile profile = createRulesProfile(createRule(RULE_UNCOVERED_MUTANT));
    final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
      md.lines = 100;
      md.mutants.noCoverage = 2;
      md.mutants.killed = 9;
    }));

    final RulesProcessor processor = new RulesProcessor(configuration, profile);

    processor.processRules(metrics, context);

    final List<Issue> issues = context.getStorage().getIssues();
    assertIssueAtLine(issues.get(0), RULE_UNCOVERED_MUTANT, "test-module:Test.java", 2, 1.0);
    assertIssueAtLine(issues.get(1), RULE_UNCOVERED_MUTANT, "test-module:Test.java", 3, 1.0);
    assertEquals(2, issues.size());
  }

  @Test
  public void processRules_uncoveredRuleActive_customEffortFactor_uncoveredMutant_issueCreated() {

    when(configuration.getDouble(EFFORT_FACTOR_SURVIVED_MUTANT)).thenReturn(Optional.of(10.0));

    final TestSensorContext context = TestSensorContext.create(folder.getRoot().toPath(), "test-module");
    final RulesProfile profile = createRulesProfile(createRule(RULE_UNCOVERED_MUTANT));
    final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
      md.lines = 100;
      md.mutants.noCoverage = 2;
      md.mutants.killed = 9;
    }));

    final RulesProcessor processor = new RulesProcessor(configuration, profile);

    processor.processRules(metrics, context);

    final List<Issue> issues = context.getStorage().getIssues();
    assertIssueAtLine(issues.get(0), RULE_UNCOVERED_MUTANT, "test-module:Test.java", 2, 10.0);
    assertIssueAtLine(issues.get(1), RULE_UNCOVERED_MUTANT, "test-module:Test.java", 3, 10.0);
    assertEquals(2, issues.size());
  }

  @Test
  public void processRules_unknownMutationStatusRuleActive_defaultEffortFactor_unknownMutationState_issueCreated() {

    when(configuration.getDouble(EFFORT_FACTOR_SURVIVED_MUTANT)).thenReturn(Optional.empty());

    final TestSensorContext context = TestSensorContext.create(folder.getRoot().toPath(), "test-module");
    final RulesProfile profile = createRulesProfile(createRule(RULE_UNKNOWN_MUTANT_STATUS));
    final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
      md.lines = 100;
      md.mutants.unknown = 2;
      md.mutants.killed = 9;
    }));

    final RulesProcessor processor = new RulesProcessor(configuration, profile);

    processor.processRules(metrics, context);

    final List<Issue> issues = context.getStorage().getIssues();
    assertIssueAtLine(issues.get(0), RULE_UNKNOWN_MUTANT_STATUS, "test-module:Test.java", 2, 1.0);
    assertIssueAtLine(issues.get(1), RULE_UNKNOWN_MUTANT_STATUS, "test-module:Test.java", 3, 1.0);
    assertEquals(2, issues.size());
  }

  @Test
  public void processRules_unknownMutationStatusRuleActive_customEffortFactor_unknownMutationState_issueCreated() {

    when(configuration.getDouble(EFFORT_FACTOR_SURVIVED_MUTANT)).thenReturn(Optional.of(10.0));

    final TestSensorContext context = TestSensorContext.create(folder.getRoot().toPath(), "test-module");
    final RulesProfile profile = createRulesProfile(createRule(RULE_UNKNOWN_MUTANT_STATUS));
    final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
      md.lines = 100;
      md.mutants.unknown = 2;
      md.mutants.killed = 9;
    }));

    final RulesProcessor processor = new RulesProcessor(configuration, profile);

    processor.processRules(metrics, context);

    final List<Issue> issues = context.getStorage().getIssues();
    assertIssueAtLine(issues.get(0), RULE_UNKNOWN_MUTANT_STATUS, "test-module:Test.java", 2, 10.0);
    assertIssueAtLine(issues.get(1), RULE_UNKNOWN_MUTANT_STATUS, "test-module:Test.java", 3, 10.0);
    assertEquals(2, issues.size());
  }

  @Test
  public void processRules_mutatorRuleActive_defaultEffortFactor_mutantSurvived_issueCreated() {

    when(configuration.getDouble(EFFORT_FACTOR_SURVIVED_MUTANT)).thenReturn(Optional.empty());

    final TestSensorContext context = TestSensorContext.create(folder.getRoot().toPath(), "test-module");
    final RulesProfile profile = createRulesProfile(createMutationOperatorRules());
    final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
      md.lines = 100;
      md.mutants.survived = MUTATION_OPERATORS.length;
      md.mutants.killed = 5;
    }));

    final RulesProcessor processor = new RulesProcessor(configuration, profile);

    processor.processRules(metrics, context);

    final List<Issue> issues = context.getStorage().getIssues();
    assertEquals(23, issues.size());
    for(int i = 0, len = MUTATION_OPERATORS.length; i < len; i++){

      assertIssueAtLine(issues.get(i), "mutant." +MUTATION_OPERATORS[i].getId(), "test-module:Test.java", MUTATION_OPERATORS.length + i, 1.0);
    }
  }

  @Test
  public void processRules_coverageThresholdRuleActive_defaultEffortFactor_coverageTooLow_issueCreated() {

    when(configuration.getDouble(EFFORT_FACTOR_MISSING_COVERAGE)).thenReturn(Optional.empty());
    final TestSensorContext context = TestSensorContext.create(folder.getRoot().toPath(), "test-module");
    final RulesProfile profile = createRulesProfile(createRule(RULE_MUTANT_COVERAGE, PARAM_MUTANT_COVERAGE_THRESHOLD, "80"));
    final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
      md.mutants.survived = 3;
      md.mutants.killed = 9;
    }));

    final RulesProcessor processor = new RulesProcessor(configuration, profile);

    processor.processRules(metrics, context);

    final Issue issue = context.getStorage().getIssues().get(0);
    assertIssueAtLine(issue, RULE_MUTANT_COVERAGE, "test-module:Test.java", 0.6);
  }

  @Test
  public void processRules_coverageThresholdRuleActive_customEffortFactor_coverageTooLow_issueCreated() {

    when(configuration.getDouble(EFFORT_FACTOR_MISSING_COVERAGE)).thenReturn(Optional.of(10.0));

    final TestSensorContext context = TestSensorContext.create(folder.getRoot().toPath(), "test-module");
    final RulesProfile profile = createRulesProfile(createRule(RULE_MUTANT_COVERAGE, PARAM_MUTANT_COVERAGE_THRESHOLD, "80"));
    final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
      md.mutants.survived = 3;
      md.mutants.killed = 9;
    }));

    final RulesProcessor processor = new RulesProcessor(configuration, profile);

    processor.processRules(metrics, context);

    final Issue issue = context.getStorage().getIssues().get(0);
    assertIssueAtLine(issue, RULE_MUTANT_COVERAGE, "test-module:Test.java", 6.0);
  }

  @Test
  public void processRules_coverageThresholdRuleActive_defaultEffortFactor_coverageThresholdHit_noIssueCreated() {

    when(configuration.getDouble(EFFORT_FACTOR_MISSING_COVERAGE)).thenReturn(Optional.empty());
    final TestSensorContext context = TestSensorContext.create(folder.getRoot().toPath(), "test-module");
    final RulesProfile profile = createRulesProfile(createRule(RULE_MUTANT_COVERAGE, PARAM_MUTANT_COVERAGE_THRESHOLD, "80"));
    final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
      md.mutants.survived = 2;
      md.mutants.killed = 8;
    }));

    final RulesProcessor processor = new RulesProcessor(configuration, profile);

    processor.processRules(metrics, context);

    assertTrue(context.getStorage().getIssues().isEmpty());
  }

  private Rule[] createMutationOperatorRules() {

    return Arrays.stream(MUTATION_OPERATORS).map(o -> createRule("mutant." + o.getId())).toArray(Rule[]::new);
  }

  private void assertIssueAtLine(final Issue issue, String ruleKey, String componentName, final double gap) {

    assertIssueAtLine(issue, ruleKey, componentName, -1, gap);
  }

  private void assertIssueAtLine(final Issue issue, String ruleKey, String componentName, int line, final double gap) {

    assertEquals(ruleKey, issue.ruleKey().rule());
    assertEquals(componentName, issue.primaryLocation().inputComponent().key());
    assertEquals(gap, issue.gap(), 0.05);
    if (line != -1) {
      assertEquals(line, issue.primaryLocation().textRange().start().line());
    } else {
      assertNull("issue is not expected to have a textrange", issue.primaryLocation().textRange());
    }
  }

  private RulesProfile createRulesProfile(Rule... rules) {

    final RulesProfile profile = RulesProfile.create("test.profile", "java");
    profile.setActiveRules(Arrays.stream(rules).map(r -> {
      final ActiveRule ar = new ActiveRule(profile, r, MAJOR);
      r.getParams().forEach(p -> ar.setParameter(p.getKey(), p.getDefaultValue()));
      return ar;
    }).collect(Collectors.toList()));
    return profile;
  }

  private Rule createRule(final String ruleKey) {

    return Rule.create(REPOSITORY_KEY, ruleKey);
  }

  private Rule createRule(String ruleKey, String key, String value) {

    final Rule r = Rule.create(REPOSITORY_KEY, ruleKey);
    r.createParameter(key).setDefaultValue(value);
    return r;
  }
}
