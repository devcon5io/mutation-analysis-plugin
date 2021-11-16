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

import static ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition.PARAM_MUTANT_COVERAGE_THRESHOLD;
import static org.slf4j.LoggerFactory.getLogger;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Optional;

import ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin;
import ch.devcon5.sonar.plugins.mutationanalysis.metrics.ResourceMutationMetrics;
import ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant;
import ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition;
import org.slf4j.Logger;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.config.Configuration;

/**
 *
 */
public class RulesProcessor {

   private static final Logger LOG = getLogger(RulesProcessor.class);

   private static final ThreadLocal<DecimalFormat> NO_DECIMAL_PLACE = ThreadLocal.withInitial(() -> new DecimalFormat("#"));
   private static final ThreadLocal<DecimalFormat> ONE_DECIMAL_PLACE = ThreadLocal.withInitial(() -> new DecimalFormat("#.0"));

   private final Configuration settings;

   /**
    * the rules profile containing the currently active rule.
    */
   private final ActiveRules rulesProfile;

   public RulesProcessor(final Configuration configuration, final ActiveRules rulesProfile) {
      this.settings = configuration;
      this.rulesProfile = rulesProfile;

   }

   /**
    * Applies the active rules to the resources based on each resource's metrics.
    *
    * @param metrics
    *         the metrics for each individual resource
    * @param context
    *         the current sensor context
    */
   public void processRules(final Collection<ResourceMutationMetrics> metrics, final SensorContext context, String language) {
      final Collection<ActiveRule> activeRules = this.rulesProfile.findByRepository(MutationAnalysisRulesDefinition.REPOSITORY_KEY + "." + language);

      if (activeRules.isEmpty()) {
         // ignore violations from report, if rule not activated in Sonar
         LOG.warn(
             "/!\\ At least one Mutation Analysis rule needs to be activated for the current profile and language: {}.",
             language);
      }

      metrics.stream()
             .filter(resourceMetrics -> language.equals(resourceMetrics.getResource().language()))
             .forEach(resourceMetrics -> applyRules(resourceMetrics, activeRules, context));
   }

   /**
    * Applies the active rules on resource metrics.
    *
    * @param resourceMetrics
    *         the mutants for found for the issuable
    * @param activeRules
    *         the active rules to apply
    * @param context
    *         the current sensor context
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
    *         the metrics for the Resource
    * @param rule
    *         the active rule to apply
    * @param context
    *         the current sensor context
    */
   private void applyRule(final ResourceMutationMetrics resourceMetrics, final ActiveRule rule, final SensorContext context) {

      applyThresholdRule(resourceMetrics, rule, context);

      applyMutantRule(resourceMetrics, rule, context);

   }

   /**
    * Creates a the mutation coverage threshold issue if the active rule is the Mutation Coverage rule.
    *
    * @param resourceMetrics
    *         the issuable on which to apply the rule
    * @param rule
    *         the metrics for the resource behind the issuable
    * @param context
    *         the rule to apply.
    *
    * @return <code>true</code> if the rule was the mutation coverage rule and the rule have been applied or
    * <code>false</code> if it was another rule
    */
   private void applyThresholdRule(final ResourceMutationMetrics resourceMetrics, final ActiveRule rule, final SensorContext context) {

      //we can skip the check whether the current rule is the coverage_threshold rule
      //because if it's not, then it won't have the coverage threshold parameter, defaulting to 0
      //an issue is only created if the actual coverage is less than the threshold, which is not possible with 0

      final double actualCoverage = resourceMetrics.getMutationCoverage();
      final double threshold = Double.parseDouble(Optional.ofNullable(rule.param(PARAM_MUTANT_COVERAGE_THRESHOLD))
                                                          .orElse("0.0"));

      if (resourceMetrics.getMutationCoverage() < threshold) {

         final double minimumKilledMutants = resourceMetrics.getMutationsTotal() * threshold / 100.0;
         final double additionalRequiredMutants = Math.ceil(minimumKilledMutants - resourceMetrics.getMutationsKilled());

         NewIssue newIssue = context.newIssue().forRule(rule.ruleKey());
         newIssue.gap(settings.getDouble(MutationAnalysisPlugin.EFFORT_FACTOR_MISSING_COVERAGE).orElse(1.0) * additionalRequiredMutants)
                 .at(newIssue.newLocation().on(resourceMetrics.getResource()).message(generateThresholdViolationMessage(actualCoverage, threshold, additionalRequiredMutants)))
                 .save();
      }
   }

   private String generateThresholdViolationMessage(final double actualCoverage, final double threshold, final double additionalRequiredMutants) {
      //
      // this method's implementation is a demonstration of how a 'killing spree' might affect your code.
      // a sane implementation would simply use String.format
      //
      // String.format("%.0f more mutants need to be killed to get the mutation coverage from %.1f%% to %.1f%%",new Object[]{additionalRequiredMutants,actualCoverage,threshold})
      //
      // But this creates an un-killable mutant through the varargs parameter it takes (something like Substituted 3 -> 4)
      // Secondly, despite best practices the StringBuilder is not initialized with an initial size hint as the value of
      // size hint is also an un-killable mutant as it only affects the resizing of the backing array but has no impact to the outcome
      //
      final DecimalFormat noDecimalPlace = NO_DECIMAL_PLACE.get();
      final DecimalFormat oneDecimalPlace = ONE_DECIMAL_PLACE.get();
      return new StringBuilder().append(noDecimalPlace.format(additionalRequiredMutants))
                                .append(" more mutants need to be killed to get the mutation coverage from ")
                                .append(oneDecimalPlace.format(actualCoverage))
                                .append('%')
                                .append(" to ")
                                .append(oneDecimalPlace.format(threshold))
                                .append('%')
                                .toString();
   }

   /**
    * Applies mutant specific rule on each mutant captured in the resource metric. For each mutant assigned to the
    * resource, it is checked if it violates: <ul> <li>the survived mutant rule</li> <li>the uncovered mutant rule</li>
    * <li>the unknown mutator status rule</li> <li>any of the mutator specific rules</li> </ul>
    *
    * @param resourceMetrics
    *         the resource metric containing the resource that might have an issue and all mutants found for that
    *         resource
    * @param rule
    *         the rule that might be violated
    * @param context
    *         the current sensor context
    */
   private void applyMutantRule(final ResourceMutationMetrics resourceMetrics, final ActiveRule rule, final SensorContext context) {

      for (final Mutant mutant : resourceMetrics.getMutants()) {
         if (violatesSurvivedMutantRule(rule, mutant)
             || violatesUncoveredMutantRule(rule, mutant)
             || violatesUnknownMutantStatusRule(rule, mutant)
             || violatesMutatorRule(rule, mutant)) {

            NewIssue newIssue = context.newIssue().forRule(rule.ruleKey());

            NewIssueLocation newLocation = newIssue.newLocation().on(resourceMetrics.getResource())
                    .at(resourceMetrics.getResource().selectLine(mutant.getLineNumber()))
                    .message(getViolationDescription(mutant));

            newIssue.gap(settings.getDouble(MutationAnalysisPlugin.EFFORT_FACTOR_SURVIVED_MUTANT).orElse(1.0))
                    .at(newLocation)
                    .save();
         }
      }
   }

   /**
    * Checks if the rule if the Survived Mutant rule and if the mutant violates it
    *
    * @param rule
    *         the rule to verify
    * @param mutant
    *         the mutant that might violate the rule
    *
    * @return <code>true</code> if the rule is violated
    */
   private boolean violatesSurvivedMutantRule(final ActiveRule rule, final Mutant mutant) {

      return MutationAnalysisRulesDefinition.RULE_SURVIVED_MUTANT.equals(rule.ruleKey().rule())
          && (mutant.getState() == Mutant.State.SURVIVED
          || mutant.getState() == Mutant.State.NO_COVERAGE);
   }

   /**
    * Checks if the rule is the Uncovered Mutant rule and if the mutant violates it
    *
    * @param rule
    *         the rule to verify
    * @param mutant
    *         the mutant that might violate the rule
    *
    * @return <code>true</code> if the rule is violated
    */
   private boolean violatesUncoveredMutantRule(final ActiveRule rule, final Mutant mutant) {

      return MutationAnalysisRulesDefinition.RULE_UNCOVERED_MUTANT.equals(rule.ruleKey().rule())
          && mutant.getState() == Mutant.State.NO_COVERAGE;
   }

   /**
    * Checks if the rule is the Unknown MutationOperator Status rule and if the mutant violates it
    *
    * @param rule
    *         the rule to verify
    * @param mutant
    *         the mutant that might violate the rule
    *
    * @return <code>true</code> if the rule is violated
    */
   private boolean violatesUnknownMutantStatusRule(final ActiveRule rule, final Mutant mutant) {

      return MutationAnalysisRulesDefinition.RULE_UNKNOWN_MUTANT_STATUS.equals(rule.ruleKey().rule())
          && mutant.getState() == Mutant.State.UNKNOWN;
   }

   /**
    * Checks if the active rule is a mutator-specific rule and if the mutant violates it.
    *
    * @param rule
    *         the rule to verify
    * @param mutant
    *         the mutant that might violate the rule
    *
    * @return <code>true</code> if the rule is violated
    */
   private boolean violatesMutatorRule(final ActiveRule rule, final Mutant mutant) {

      return rule.ruleKey()
                 .rule()
                 .startsWith(MutationAnalysisRulesDefinition.MUTANT_RULES_PREFIX + mutant.getMutationOperator().getId())
          && mutant.getState().isAlive();
   }

   /**
    * Gets the mutant specific violation description of the mutator of the mutant
    *
    * @param mutant
    *         the mutant to receive the violation description
    *
    * @return the description as string
    */
   private String getViolationDescription(final Mutant mutant) {

      final StringBuilder message = new StringBuilder(mutant.getMutationOperator().getViolationDescription());

      mutant.getDescription().ifPresent(desc -> message.append(" Mutation: ").append(desc));

      if (!mutant.getMutatorSuffix().isEmpty()) {
         message.append(" (").append(mutant.getMutatorSuffix()).append(')');
      }
      return message.toString();
   }

}
