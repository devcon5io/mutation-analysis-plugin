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
import org.sonar.api.Plugin;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.config.Configuration;

/**
 * This class is the entry point for the Mutation Analysis Plugin.
 * The properties define, which {@link org.sonar.api.config.Configuration} are configurable for the plugin.
 */
@Properties({
                @Property(key = MutationAnalysisPlugin.PITEST_JAVA_SENSOR_ENABLED,
                          name = "Active Pitest Java Sensor",
                          description = "Enables the Kotlin Sensor for PIT. Default is 'true'",
                          type = PropertyType.BOOLEAN,
                          defaultValue = "true",
                          project = true),
                @Property(key = MutationAnalysisPlugin.PITEST_KOTLIN_SENSOR_ENABLED,
                          name = "Active Pitest Kotlin Sensor",
                          description = "Enables the Kotlin Sensor for PIT. Default is 'true'",
                          type = PropertyType.BOOLEAN,
                          defaultValue = "true",
                          project = true),
                @Property(key = MutationAnalysisPlugin.EXPERIMENTAL_FEATURE_ENABLED,
                          name = "Enable experimental features",
                          description = "Enables features that are still under development and may cause existing behavior to break",
                          type = PropertyType.BOOLEAN,
                          defaultValue = "false",
                          project = true),
                @Property(key = MutationAnalysisPlugin.REPORT_DIRECTORY_KEY,
                          defaultValue = MutationAnalysisPlugin.REPORT_DIRECTORY_DEF,
                          name = "Output directory for the PIT reports",
                          description = "This property is needed when the reports are not located in the default directory (i.e. target/pit-reports)",
                          project = true),
                @Property(key = MutationAnalysisPlugin.EFFORT_MUTANT_KILL,
                          defaultValue = MutationAnalysisPlugin.DEFAULT_EFFORT_TO_KILL_MUTANT,
                          name = "Effort: Kill a mutant",
                          description = "Effort to kill a single mutant. Values may be hours, i.e. '1h' or minutes '30min'.",
                          type = PropertyType.STRING,
                          project = true),
                @Property(key = MutationAnalysisPlugin.EFFORT_FACTOR_MISSING_COVERAGE,
                          defaultValue = "1.0",
                          name = "Effort Factor: Missing Coverage",
                          description = "Factor that is multiplied by the minimally additional required mutants to be killed to get required mutation coverage.",
                          type = PropertyType.FLOAT,
                          project = true),
                @Property(key = MutationAnalysisPlugin.EFFORT_FACTOR_SURVIVED_MUTANT,
                          defaultValue = "1.0",
                          name = "Effort Factor: Survived Mutant ",
                          description = "Factor that is applied to any of the survived mutant rule to calculate effort to fix",
                          type = PropertyType.FLOAT,
                          project = true),
                @Property(key = MutationAnalysisPlugin.FORCE_MISSING_COVERAGE_TO_ZERO,
                          name = "Force missing coverage to zero",
                          description = "If a project has no mutation report, it's coverage is forced to zero. If disabled, no coverage metric is calculated",
                          type = PropertyType.BOOLEAN,
                          defaultValue = "false"
                          //the project=true setting doesn't seem to have an effect on compute engine side
                          // see https://community.sonarsource.com/t/plugin-development-project-level-settings-have-no-effect-in-ce/1528
                          //, project = true
                ),
            })
public final class MutationAnalysisPlugin implements Plugin {


  public static final String PITEST_JAVA_SENSOR_ENABLED = "dc5.mutationAnalysis.pitest.java.sensor.enabled";
  public static final String PITEST_KOTLIN_SENSOR_ENABLED = "dc5.mutationAnalysis.pitest.kotlin.sensor.enabled";
  public static final String EXPERIMENTAL_FEATURE_ENABLED = "dc5.mutationAnalysis.experimentalFeatures.enabled";
  public static final String EFFORT_MUTANT_KILL = "dc5.mutationAnalysis.effort.mutantKill";
  public static final String EFFORT_FACTOR_MISSING_COVERAGE = "dc5.mutationAnalysis.effort.missingCoverage";
  public static final String EFFORT_FACTOR_SURVIVED_MUTANT = "dc5.mutationAnalysis.effort.survivedMutant";
  public static final String FORCE_MISSING_COVERAGE_TO_ZERO = "dc5.mutationAnalysis.missingCoverage.force2zero";
  public static final String REPORT_DIRECTORY_KEY = "dc5.mutationAnalysis.pitest.sensor.reports.directory";
  public static final String REPORT_DIRECTORY_DEF = "target/pit-reports";
  public static final String DEFAULT_EFFORT_TO_KILL_MUTANT = "5min";

  @Override
  public void define(final Context context) {

    //we add each extension separately although there is a method (addExtensions) accepting varargs that would handle this in one call.
    //Background: varargs produce bytecode artifacts that cause mutations that can not be killed (i.e. reordering of varargs)
    //Because of this plugin deals with mutations, it's also kind of an example how to run mutation testing and what impact on code bases it might have.
    //This option was chosen because it allows to kill all mutants in this class + reduces the mutation density.
    //The equivalent option would be to use the varargs, which would have the same LoC but just a single statement and lots of re-ordering mutations, which
    //increased the mutation density + reduces the mutation coverage through unkillable mutations.
    context.addExtension(PitestReportParser.class);
    context.addExtension(ReportFinder.class);
    context.addExtension(JavaRulesDefinition.class);
    context.addExtension(KotlinRulesDefinition.class);
    context.addExtension(JavaProfileDefinition.class);
    context.addExtension(KotlinProfileDefinition.class);
    context.addExtension(PitestSensor.class);
    context.addExtension(MutationAnalysisMetrics.class);
    context.addExtension(MutationScoreComputer.class);
    context.addExtension(MutationDensityComputer.class);
    context.addExtension(TotalMutationsComputer.class);
    context.addExtension(TestKillRatioComputer.class);
    context.addExtension(QuantitativeMeasureComputer.class);
  }

  public static boolean isExperimentalFeaturesEnabled(Configuration config){
    return config.getBoolean(EXPERIMENTAL_FEATURE_ENABLED).orElse(false);
  }
}
