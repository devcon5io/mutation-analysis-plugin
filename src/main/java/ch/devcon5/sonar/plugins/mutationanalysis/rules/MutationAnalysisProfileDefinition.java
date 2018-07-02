package ch.devcon5.sonar.plugins.mutationanalysis.rules;

import static ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition.MUTANT_RULES_PREFIX;
import static ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition.REPOSITORY_KEY;
import static org.slf4j.LoggerFactory.getLogger;

import ch.devcon5.sonar.plugins.mutationanalysis.model.MutationOperators;
import org.slf4j.Logger;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

public class MutationAnalysisProfileDefinition implements BuiltInQualityProfilesDefinition {

   private static final Logger LOGGER = getLogger(MutationAnalysisProfileDefinition.class);

   @Override
   public void define(final Context context) {

      final NewBuiltInQualityProfile mutationAnalysis = context.createBuiltInQualityProfile("Sonar Way + Mutation Analysis","java");
      MutationOperators.allMutagens().forEach(m -> mutationAnalysis.activateRule(REPOSITORY_KEY, MUTANT_RULES_PREFIX + m.getId()));

      //take all the rules active in the java built-in profile
      final BuiltInQualityProfile sonarWay = context.profile("java","Sonar way");
      if(sonarWay != null) {
         sonarWay.rules().forEach(rule -> mutationAnalysis.activateRule(rule.repoKey(), rule.ruleKey()));
      } else {
         LOGGER.warn("No 'Sonar Way'/java profile found");
      }

      //this will make this rule a new default for java
      mutationAnalysis.setDefault(true);
      mutationAnalysis.done();
   }
}
