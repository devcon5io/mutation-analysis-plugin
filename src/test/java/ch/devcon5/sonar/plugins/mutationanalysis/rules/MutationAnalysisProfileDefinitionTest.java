package ch.devcon5.sonar.plugins.mutationanalysis.rules;

import static ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition.REPOSITORY_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInQualityProfile;

@RunWith(MockitoJUnitRunner.class)
public class MutationAnalysisProfileDefinitionTest {

   public static final String SONAR_WAY_REPOKEY = "sonarWay";

   private BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();

   @Before
   public void setUp() throws Exception {
      BuiltInQualityProfilesDefinition.NewBuiltInQualityProfile sonarWay = context.createBuiltInQualityProfile("Sonar way", "java");
      sonarWay.activateRule(SONAR_WAY_REPOKEY, "exampleRule1");
      sonarWay.activateRule(SONAR_WAY_REPOKEY, "exampleRule2");
      sonarWay.done();
   }

   @Test
   public void define() {

      MutationAnalysisProfileDefinition def = new MutationAnalysisProfileDefinition();

      def.define(context);

      BuiltInQualityProfile result = context.profile("java", "Sonar Way + Mutation Analysis");

      assertNotNull(result);
      assertEquals(23, result.rules().stream().filter(r -> REPOSITORY_KEY.equals(r.repoKey())).count());
      assertEquals(2, result.rules().stream().filter(r -> SONAR_WAY_REPOKEY.equals(r.repoKey())).count());
   }
}
