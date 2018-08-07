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

package ch.devcon5.sonar.plugins.mutationanalysis.standalone;

import ch.devcon5.sonar.plugins.mutationanalysis.metrics.ResourceMutationMetrics;
import ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant;
import ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition;
import org.sonar.api.rules.ActiveRule;

import java.nio.file.Path;
import java.util.*;

import static ch.devcon5.sonar.plugins.mutationanalysis.standalone.Json.*;


/**
 *
 */
public class StandaloneRulesProcessor {

    /**
     * Applies the active rules to the resources based on each resource's metrics.
     *
     * @param metrics the metrics for each individual resource
     */
    public List<String> processRules(final Collection<ResourceMutationMetrics> metrics, final Path baseDir) {

        List<String> issues = new ArrayList<>();
        for (final ResourceMutationMetrics resourceMetrics : metrics) {
            issues.addAll(applyRules(resourceMetrics, baseDir));
        }
        return issues;
    }

    /**
     * Applies the active rules on resource metrics for the {@link org.sonar.api.issue.Issuable} resource.
     *
     * @param resourceMetrics the mutants for found for the issuable
     */
    private List<String> applyRules(final ResourceMutationMetrics resourceMetrics, Path baseDir) {
        List<String> issues = applyMutantRule(resourceMetrics, baseDir);
        applyThresholdRule(resourceMetrics, baseDir).ifPresent(issues::add);
        return issues;
    }

    /**
     * Creates a the mutation coverage threshold issue if the active rule is the Mutation Coverage rule.
     *
     * @param resourceMetrics the issuable on which to apply the rule
     * @return <code>true</code> if the rule was the mutation coverage rule and the rule have been applied or
     * <code>false</code> if it was another rule
     */
    private Optional<String> applyThresholdRule(final ResourceMutationMetrics resourceMetrics, final Path baseDir) {


        final double actualCoverage = resourceMetrics.getMutationCoverage();
//    final double threshold = Double.parseDouble(rule.getParameter(MutationAnalysisRulesDefinition.PARAM_MUTANT_COVERAGE_THRESHOLD));
        final double threshold = 80.0;

        if (resourceMetrics.getMutationCoverage() < threshold) {

            final double minimumKilledMutants = resourceMetrics.getMutationsTotal() * threshold / 100.0;
            final double additionalRequiredMutants = minimumKilledMutants - resourceMetrics.getMutationsKilled();

            // TODO ensure that additional + miniumum > threshold

            //TODO add .gap(settings.getDouble(MutationAnalysisPlugin.EFFORT_FACTOR_MISSING_COVERAGE).orElse(1.0) * additionalRequiredMutants)
            return Optional.of(
                    //TODO use correct rule
                    newIssue("mutationCoverageThreshold",
                    atLocation(resourceMetrics.getFile(),
                    String.format("%.0f more mutants need to be killed to get the mutation coverage from %.1f%% to %.1f%%",
                    additionalRequiredMutants,
                    actualCoverage,
                    threshold))));

        }
        return Optional.empty();
    }

    private String atLocation(Path filePath, String message, int lineNumber) {


        String loc = obj(prop("filePath",filePath.toString().replaceAll("\\\\","/")), prop("message", message));


        if(lineNumber != -1){
            loc = append(loc, propObj("textRange", obj(prop("startLine",lineNumber), prop("endLine", lineNumber))));
        }
        return loc;

    }
    private String atLocation(Path filePath, String message) {
        return atLocation(filePath, message, -1);
    }

    private String newIssue(String rule, String location) {
        return obj( prop("engineId", "test"),
                    prop("ruleId", rule),
                    prop("severity", "MAJOR"),
                    prop("type", "BUG"),
                    propObj("primaryLocation", location));
    }

    /**
     * Applies mutant specific rule on each mutant captured in the resource metric. For each mutant assigned to the
     * resource, it is checked if it violates: <ul> <li>the survived mutant rule</li> <li>the uncovered mutant rule</li>
     * <li>the unknown mutator status rule</li> <li>any of the mutator specific rules</li> </ul>
     *
     * @param resourceMetrics the resource metric containing the resource that might have an issue and all mutants found for that
     *                        resource
     */
    private List<String> applyMutantRule(final ResourceMutationMetrics resourceMetrics, Path baseDir) {

        List<String> issues = new ArrayList<>();

        for (final Mutant mutant : resourceMetrics.getMutants()) {

            if(!mutant.isDetected()){

                issues.add(
                newIssue(mutant.getMutationOperator().getId(),
                        //TODO use correct urle
                        //TODO .gap(settings.getDouble(MutationAnalysisPlugin.EFFORT_FACTOR_SURVIVED_MUTANT).orElse(1.0))
                        atLocation(resourceMetrics.getFile(), getViolationDescription(mutant), mutant.getLineNumber())));
            }
        }
        return issues;
    }

    /**
     * Checks if the rule if the Survived Mutant rule and if the mutant violates it
     *
     * @param rule   the rule to verify
     * @param mutant the mutant that might violate the rule
     * @return <code>true</code> if the rule is violated
     */
    private boolean violatesSurvivedMutantRule(final ActiveRule rule, final Mutant mutant) {

        return MutationAnalysisRulesDefinition.RULE_SURVIVED_MUTANT.equals(rule.getRuleKey()) && mutant.getState() == Mutant.State.SURVIVED;
    }

    /**
     * Checks if the rule is the Uncovered Mutant rule and if the mutant violates it
     *
     * @param rule   the rule to verify
     * @param mutant the mutant that might violate the rule
     * @return <code>true</code> if the rule is violated
     */
    private boolean violatesUncoveredMutantRule(final ActiveRule rule, final Mutant mutant) {

        return MutationAnalysisRulesDefinition.RULE_UNCOVERED_MUTANT.equals(rule.getRuleKey()) && mutant.getState() == Mutant.State.NO_COVERAGE;
    }

    /**
     * Checks if the rule is the Unknown MutationOperator Status rule and if the mutant violates it
     *
     * @param rule   the rule to verify
     * @param mutant the mutant that might violate the rule
     * @return <code>true</code> if the rule is violated
     */
    private boolean violatesUnknownMutantStatusRule(final ActiveRule rule, final Mutant mutant) {

        return MutationAnalysisRulesDefinition.RULE_UNKNOWN_MUTANT_STATUS.equals(rule.getRuleKey()) && mutant.getState() == Mutant.State.UNKNOWN;
    }

    /**
     * Checks if the active rule is a mutator-specific rule and if the mutant violates it.
     *
     * @param rule   the rule to verify
     * @param mutant the mutant that might violate the rule
     * @return <code>true</code> if the rule is violated
     */
    private boolean violatesMutatorRule(final ActiveRule rule, final Mutant mutant) {

        return rule.getRuleKey()
                   .equals(MutationAnalysisRulesDefinition.MUTANT_RULES_PREFIX + mutant.getMutationOperator()
                                                                                       .getId()) && mutant.getState()
                                                                                                          .isAlive();
    }

    /**
     * Gets the mutant specific violation description of the mutator of the mutant
     *
     * @param mutant the mutant to receive the violation description
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
