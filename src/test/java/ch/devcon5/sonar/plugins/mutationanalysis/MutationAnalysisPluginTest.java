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

package ch.devcon5.sonar.plugins.mutationanalysis;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationAnalysisMetrics;
import ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationDensityComputer;
import ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationScoreComputer;
import ch.devcon5.sonar.plugins.mutationanalysis.metrics.QuantitativeMeasureComputer;
import ch.devcon5.sonar.plugins.mutationanalysis.metrics.TestKillRatioComputer;
import ch.devcon5.sonar.plugins.mutationanalysis.metrics.TotalMutationsComputer;
import ch.devcon5.sonar.plugins.mutationanalysis.report.PitestReportParser;
import ch.devcon5.sonar.plugins.mutationanalysis.report.ReportFinder;
import ch.devcon5.sonar.plugins.mutationanalysis.rules.JavaProfileDefinition;
import ch.devcon5.sonar.plugins.mutationanalysis.rules.JavaRulesDefinition;
import ch.devcon5.sonar.plugins.mutationanalysis.rules.KotlinProfileDefinition;
import ch.devcon5.sonar.plugins.mutationanalysis.rules.KotlinRulesDefinition;
import ch.devcon5.sonar.plugins.mutationanalysis.sensors.PitestSensor;
import ch.devcon5.sonar.plugins.mutationanalysis.testharness.TestConfiguration;
import org.junit.Test;
import org.sonar.api.Plugin;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.Version;

public class MutationAnalysisPluginTest {


   private SonarRuntime sonarRuntime = SonarRuntimeImpl.forSonarQube(Version.create(7, 3), SonarQubeSide.SCANNER, SonarEdition.COMMUNITY);


   @Test
   public void testDefine() throws Exception {

      MutationAnalysisPlugin plugin = new MutationAnalysisPlugin();
      Plugin.Context context = new Plugin.Context(sonarRuntime);

      plugin.define(context);

      assertTrue(context.getExtensions().contains(PitestReportParser.class));
      assertTrue(context.getExtensions().contains(PitestReportParser.class));
      assertTrue(context.getExtensions().contains(ReportFinder.class));
      assertTrue(context.getExtensions().contains(JavaRulesDefinition.class));
      assertTrue(context.getExtensions().contains(KotlinRulesDefinition.class));
      assertTrue(context.getExtensions().contains(JavaProfileDefinition.class));
      assertTrue(context.getExtensions().contains(KotlinProfileDefinition.class));
      assertTrue(context.getExtensions().contains(PitestSensor.class));
      assertTrue(context.getExtensions().contains(MutationAnalysisMetrics.class));
      assertTrue(context.getExtensions().contains(MutationScoreComputer.class));
      assertTrue(context.getExtensions().contains(MutationDensityComputer.class));
      assertTrue(context.getExtensions().contains(TotalMutationsComputer.class));
      assertTrue(context.getExtensions().contains(TestKillRatioComputer.class));
      assertTrue(context.getExtensions().contains(QuantitativeMeasureComputer.class));

   }

   @Test
   public void experimentalFeaturesEnabled_default_false() throws Exception {

      TestConfiguration configuration = new TestConfiguration();

      assertFalse(MutationAnalysisPlugin.isExperimentalFeaturesEnabled(configuration));

   }

   @Test
   public void experimentalFeaturesEnabled_configured_true() throws Exception {

      TestConfiguration configuration = new TestConfiguration();
      configuration.set("dc5.mutationAnalysis.experimentalFeatures.enabled", true);

      assertTrue(MutationAnalysisPlugin.isExperimentalFeaturesEnabled(configuration));

   }

}
