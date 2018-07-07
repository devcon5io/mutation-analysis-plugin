package ch.devcon5.sonar.plugins.mutationanalysis.rules;

import static ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition.MUTANT_RULES_PREFIX;
import static ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition.REPOSITORY_KEY;

import ch.devcon5.sonar.plugins.mutationanalysis.model.MutationOperators;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

public class MutationAnalysisProfileDefinition implements BuiltInQualityProfilesDefinition {

   @Override
   public void define(final Context context) {

      final NewBuiltInQualityProfile mutationAnalysis = context.createBuiltInQualityProfile("Mutation Analysis","java");

      MutationOperators.allMutagens().forEach(m -> mutationAnalysis.activateRule(REPOSITORY_KEY, MUTANT_RULES_PREFIX + m.getId()));

      mutationAnalysis.done();
   }
}
