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

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.List;

import ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin;
import ch.devcon5.sonar.plugins.mutationanalysis.metrics.ResourceMutationMetrics;
import ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant;
import ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition;
import org.slf4j.Logger;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueLocation;
import org.sonar.api.config.Configuration;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;

/**
 *
 */
public class RulesProcessor {

  private static final Logger LOG = getLogger(RulesProcessor.class);

  private final Configuration settings;

  /**
   * the rules profile containing the currently active rule.
   */
  private final RulesProfile rulesProfile;

  public RulesProcessor(final Configuration configuration, final RulesProfile rulesProfile) {
    this.settings = configuration;
    this.rulesProfile = rulesProfile;

  }

  /**
   * Applies the active rules to the resources based on each resource's metrics.
   * @param metrics
   *  the metrics for each individual resource
   * @param context
   *  the current sensor context
   */
  public void processRules(final Collection<ResourceMutationMetrics> metrics, final SensorContext context) {
    final List<ActiveRule> activeRules = this.rulesProfile.getActiveRulesByRepository(MutationAnalysisRulesDefinition.REPOSITORY_KEY);

    if (activeRules.isEmpty()) {
      // ignore violations from report, if rule not activated in Sonar
      LOG.warn("/!\\ PIT rule needs to be activated in the \"{}\" profile.", rulesProfile.getName());
    }
    for (final ResourceMutationMetrics resourceMetrics : metrics) {
      applyRules(resourceMetrics, activeRules, context);
    }
  }

  /**
   * Applies the active rules on resource metrics for the {@link org.sonar.api.issue.Issuable} resource.
   *
   * @param resourceMetrics
   *     the mutants for found for the issuable
   * @param activeRules
   *     the active rules to apply
   * @param context
   *     the current sensor context
   */
  private void applyRules(final ResourceMutationMetrics resourceMetrics, final Collection<ActiveRule> activeRules, final SensorContext context) {

    for (final ActiveRule rule : activeRules) {
      applyRule(resourceMetrics, rule, context);
    }
  }


  /**
   * Applies the active rule on the issuable if any of the resource metrics for the issuable violates the rule
   *
   * @param resourceMetrics
   *     the metrics for the {@link org.sonar.api.resources.Resource} behind the {@link org.sonar.api.issue.Issuable}
   * @param rule
   *     the active rule to apply
   * @param context
   *     the current sensor context
   */
  private void applyRule(final ResourceMutationMetrics resourceMetrics, final ActiveRule rule, final SensorContext context) {

    applyThresholdRule(resourceMetrics, rule, context);

    applyMutantRule(resourceMetrics, rule, context);

  }

  /**
   * Creates a the mutation coverage threshold issue if the active rule is the Mutation Coverage rule.
   *
   * @param resourceMetrics
   *     the issuable on which to apply the rule
   * @param rule
   *     the metrics for the resource behind the issuable
   * @param context
   *     the rule to apply.
   *
   * @return <code>true</code> if the rule was the mutation coverage rule and the rule have been applied or
   * <code>false</code> if it was another rule
   */
  private void applyThresholdRule(final ResourceMutationMetrics resourceMetrics, final ActiveRule rule, final SensorContext context) {

    if (!MutationAnalysisRulesDefinition.RULE_MUTANT_COVERAGE.equals(rule.getRuleKey())) {
      return;
    }
    final double actualCoverage = resourceMetrics.getMutationCoverage();
    final double threshold = Double.parseDouble(rule.getParameter(MutationAnalysisRulesDefinition.PARAM_MUTANT_COVERAGE_THRESHOLD));

    if (resourceMetrics.getMutationCoverage() < threshold) {

      final double minimumKilledMutants = resourceMetrics.getMutationsTotal() * threshold / 100.0;
      final double additionalRequiredMutants = minimumKilledMutants - resourceMetrics.getMutationsKilled();

      // TODO ensure that additional + miniumum > threshold

      context.newIssue()
             .forRule(rule.getRule().ruleKey())
             .gap(settings.getDouble(MutationAnalysisPlugin.EFFORT_FACTOR_MISSING_COVERAGE).orElse(1.0) * additionalRequiredMutants)
             .at(newLocation().on(resourceMetrics.getResource())
                              .message(String.format("%.0f more mutants need to be killed to get the mutation coverage from %.1f%% to %.1f%%",
                                                     additionalRequiredMutants,
                                                     actualCoverage,
                                                     threshold)))
             .save();
    }
  }

  /**
   * Applies mutant specific rule on each mutant captured in the resource metric. For each mutant assigned to the
   * resource, it is checked if it violates: <ul> <li>the survived mutant rule</li> <li>the uncovered mutant rule</li>
   * <li>the unknown mutator status rule</li> <li>any of the mutator specific rules</li> </ul>
   *
   * @param resourceMetrics
   *     the resource metric containing the resource that might have an issue and all mutants found for that
   *     resource
   * @param rule
   *     the rule that might be violated
   * @param context
   *     the current sensor context
   */
  private void applyMutantRule(final ResourceMutationMetrics resourceMetrics, final ActiveRule rule, final SensorContext context) {

    for (final Mutant mutant : resourceMetrics.getMutants()) {
      if (violatesSurvivedMutantRule(rule, mutant)
          || violatesUncoveredMutantRule(rule, mutant)
          || violatesUnknownMutantStatusRule(rule, mutant)
          || violatesMutatorRule(rule, mutant)) {

        context.newIssue()
               .forRule(rule.getRule().ruleKey())
               .gap(settings.getDouble(MutationAnalysisPlugin.EFFORT_FACTOR_SURVIVED_MUTANT).orElse(1.0))
               .at(newLocation().on(resourceMetrics.getResource())
                                .at(resourceMetrics.getResource().selectLine(mutant.getLineNumber()))
                                .message(getViolationDescription(mutant)))
               .save();
      }
    }
  }

  /**
   * Checks if the rule if the Survived Mutant rule and if the mutant violates it
   *
   * @param rule
   *     the rule to verify
   * @param mutant
   *     the mutant that might violate the rule
   *
   * @return <code>true</code> if the rule is violated
   */
  private boolean violatesSurvivedMutantRule(final ActiveRule rule, final Mutant mutant) {

    return MutationAnalysisRulesDefinition.RULE_SURVIVED_MUTANT.equals(rule.getRuleKey()) && mutant.getState() == Mutant.State.SURVIVED;
  }

  /**
   * Checks if the rule is the Uncovered Mutant rule and if the mutant violates it
   *
   * @param rule
   *     the rule to verify
   * @param mutant
   *     the mutant that might violate the rule
   *
   * @return <code>true</code> if the rule is violated
   */
  private boolean violatesUncoveredMutantRule(final ActiveRule rule, final Mutant mutant) {

    return MutationAnalysisRulesDefinition.RULE_UNCOVERED_MUTANT.equals(rule.getRuleKey()) && mutant.getState() == Mutant.State.NO_COVERAGE;
  }

  /**
   * Checks if the rule is the Unknown MutationOperator Status rule and if the mutant violates it
   *
   * @param rule
   *     the rule to verify
   * @param mutant
   *     the mutant that might violate the rule
   *
   * @return <code>true</code> if the rule is violated
   */
  private boolean violatesUnknownMutantStatusRule(final ActiveRule rule, final Mutant mutant) {

    return MutationAnalysisRulesDefinition.RULE_UNKNOWN_MUTANT_STATUS.equals(rule.getRuleKey()) && mutant.getState() == Mutant.State.UNKNOWN;
  }

  /**
   * Checks if the active rule is a mutator-specific rule and if the mutant violates it.
   *
   * @param rule
   *     the rule to verify
   * @param mutant
   *     the mutant that might violate the rule
   *
   * @return <code>true</code> if the rule is violated
   */
  private boolean violatesMutatorRule(final ActiveRule rule, final Mutant mutant) {

    return rule.getRuleKey().equals(MutationAnalysisRulesDefinition.MUTANT_RULES_PREFIX + mutant.getMutationOperator().getId()) && mutant.getState().isAlive();
  }

  /**
   * Gets the mutant specific violation description of the mutator of the mutant
   *
   * @param mutant
   *     the mutant to receive the violation description
   *
   * @return the description as string
   */
  private String getViolationDescription(final Mutant mutant) {

    final StringBuilder message = new StringBuilder(mutant.getMutationOperator().getViolationDescription());
    if (!mutant.getMutatorSuffix().isEmpty()) {
      message.append(" (").append(mutant.getMutatorSuffix()).append(')');
    }
    return message.toString();
  }

  private static NewIssueLocation newLocation() {

    return new DefaultIssueLocation();
  }
}
