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

package ch.devcon5.sonar.plugins.mutationanalysis.rules;

import static ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin.DEFAULT_EFFORT_TO_KILL_MUTANT;
import static ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin.EFFORT_MUTANT_KILL;

import ch.devcon5.sonar.plugins.mutationanalysis.model.MutationOperator;
import ch.devcon5.sonar.plugins.mutationanalysis.model.MutationOperators;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;

/**
 * The definition of pitest rules. A new repository is created for the Pitest plugin and Java language. The rules are
 * defined in the rules.xml file in the classpath. The rule keys are accessible as constants.
 */
public abstract class MutationAnalysisRulesDefinition implements org.sonar.api.server.rule.RulesDefinition {

  /**
   * The key for the PITest repository
   */
  public static final String REPOSITORY_KEY = "mutation.analysis";
  /**
   * The name for the PITest repository
   */
  public static final String REPOSITORY_NAME = "MutationAnalysis";
  /**
   * Rule key for the survived mutants rule.
   */
  public static final String RULE_SURVIVED_MUTANT = "mutant.survived";
  /**
   * Rule key for the uncovered mutants rule.
   */
  public static final String RULE_UNCOVERED_MUTANT = "mutant.uncovered";
  /**
   * Rule key for the coverage of mutants not killed by a test
   */
  public static final String RULE_MUTANT_COVERAGE = "mutant.coverage";
  /**
   * The parameter for the Mutant Coverage rule defining the threshold when an issue is created
   */
  public static final String PARAM_MUTANT_COVERAGE_THRESHOLD = "mutant.coverage.threshold";
  /**
   * Rule key for the mutants with unknown status rule.
   */
  public static final String RULE_UNKNOWN_MUTANT_STATUS = "mutant.unknownStatus";
  /**
   * Prefix for mutator rules
   */
  public static final String MUTANT_RULES_PREFIX = "mutant.";
  /**
   * SLF4J Logger for this class
   */
  private static final Logger LOG = LoggerFactory.getLogger(MutationAnalysisRulesDefinition.class);
  /**
   * Loader used to load the rules from an xml file
   */
  private final RulesDefinitionXmlLoader xmlLoader;

  /**
   * The plugin settings
   */
  private final Configuration settings;

  /**
   * Constructor to create the pitest rules definitions and repository. The constructor is invoked by Sonar.
   *
   * @param settings the settings of the Pitest-Sensor plugin
   * @param xmlLoader an XML loader to load the rules definitions from the rules def.
   */
  protected MutationAnalysisRulesDefinition(final Configuration settings, final RulesDefinitionXmlLoader xmlLoader) {
    this.xmlLoader = xmlLoader;
    this.settings = settings;
  }

  /**
   * Defines the rules for the pitest rules repository. In addition to the rules defined in the rules.xml the method
   * created a rule for every mutator.
   */
  @Override
  public void define(final Context context) {
    final NewRepository repository = context.createRepository(REPOSITORY_KEY + "." + getLanguageKey(), getLanguageKey())
        .setName(REPOSITORY_NAME);
    this.xmlLoader.load(repository,
        getClass().getResourceAsStream("/ch/devcon5/sonar/plugins/mutationanalysis/rules.xml"), StandardCharsets.UTF_8);
    addMutatorRules(repository);

    for (final NewRule rule : repository.rules()) {
      rule.setDebtRemediationFunction(rule.debtRemediationFunctions()
          .linearWithOffset(settings.get(EFFORT_MUTANT_KILL)
              .orElse(DEFAULT_EFFORT_TO_KILL_MUTANT), "7min"));
      rule.setGapDescription("Effort to kill the mutant(s)");
    }
    repository.done();
    LOG.info("Defining Mutation Analysis rule repository {} done", repository);
  }

  protected abstract String getLanguageKey();

  /**
   * Enriches the mutator rules with the descriptions from the mutators
   *
   * @param repository the repository containing the mutator rules
   */
  private void addMutatorRules(final NewRepository repository) {
    for (final MutationOperator mutationOperator : MutationOperators.allMutationOperators()) {
      // create for each mutation operator two rules, one is a "bug" rule, the other is
      // a "code smell" rule. As there are project which prefer to treat them as bugs (or potential bugs)
      // and other projects might prefer to treat them as code smell. This way
      // the projects can decide themselves how to set up their quality profile
      createBugRule(repository, mutationOperator);
      createCodeSmellRule(repository, mutationOperator);
    }
  }

  private void createBugRule(final NewRepository repository, final MutationOperator mutationOperator) {
    final String id = MUTANT_RULES_PREFIX + mutationOperator.getId();
    final String name = mutationOperator.getName();
    createRule(repository, mutationOperator, RuleType.BUG, id, name);
  }

  private void createCodeSmellRule(final NewRepository repository, final MutationOperator mutationOperator) {
    final String id = MUTANT_RULES_PREFIX + mutationOperator.getId() + "_" + RuleType.CODE_SMELL;
    final String name = mutationOperator.getName() + " (Code Smell)";
    createRule(repository, mutationOperator, RuleType.CODE_SMELL, id, name);
  }

  private void createRule(final NewRepository repository, final MutationOperator mutationOperator, final RuleType type, final String id, final String name) {
    final NewRule rule = repository.createRule(id)
        .setType(type)
        .setName(name)
        .setTags("pitest", "test", "test-quality", "mutator", "mutation-operator");
    mutationOperator.getMutagenDescriptionLocation().ifPresent(rule::setHtmlDescription);
    if (mutationOperator.getId().startsWith("EXPERIMENTAL")) {
      rule.setStatus(RuleStatus.BETA);
    } else {
      rule.setActivatedByDefault(true);
    }
  }

}
