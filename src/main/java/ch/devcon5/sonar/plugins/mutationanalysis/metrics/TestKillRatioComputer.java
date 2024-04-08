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

package ch.devcon5.sonar.plugins.mutationanalysis.metrics;

import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.TEST_KILLS_KEY;
import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.TEST_KILL_RATIO;
import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.TEST_KILL_RATIO_KEY;
import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.UTILITY_GLOBAL_MUTATIONS_KEY;
import static org.slf4j.LoggerFactory.getLogger;

import ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin;
import ch.devcon5.sonar.plugins.mutationanalysis.Streams;
import org.slf4j.Logger;
import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.config.Configuration;

public class TestKillRatioComputer implements MeasureComputer {

  private static final Logger LOG = getLogger(TestKillRatioComputer.class);

  private final Configuration config;

  public TestKillRatioComputer(final Configuration config) {
    this.config = config;
  }

  @Override
  public MeasureComputerDefinition define(final MeasureComputerDefinitionContext defContext) {
    return defContext.newDefinitionBuilder()
        .setInputMetrics(UTILITY_GLOBAL_MUTATIONS_KEY, TEST_KILLS_KEY)
        .setOutputMetrics(TEST_KILL_RATIO_KEY)
        .build();
  }

  @Override
  public void compute(final MeasureComputerContext context) {
    if (!MutationAnalysisPlugin.isExperimentalFeaturesEnabled(config)) {
      LOG.info("ExperimentalFeature disabled");
      return;
    }

    final Component comp = context.getComponent();
    if (isNotUnitTestOrDirectory(comp)) {
      LOG.info("Skipping {} because it's not a test resource", comp);
      return;
    }

    final double mutationsGlobal = getMutationsGlobal(context, UTILITY_GLOBAL_MUTATIONS_KEY);
    final double testKillsLocal = getTestKillsLocal(context);
    if (mutationsGlobal == 0.0) {
      return;
    }
    final double percentage = 100.0d * testKillsLocal / mutationsGlobal;
    LOG.info("Computed {} of {}% from ({} / {}) for {}", TEST_KILL_RATIO.getName(), percentage, testKillsLocal, mutationsGlobal, comp);
    context.addMeasure(TEST_KILL_RATIO_KEY, percentage);
  }

  boolean isNotUnitTestOrDirectory(final Component comp) {
    return comp.getType() == Component.Type.FILE && !comp.getFileAttributes().isUnitTest();
  }

  private double getMutationsGlobal(final MeasureComputerContext context, String key) {
    final double mutationsGlobal;
    final Measure globalMutationsMeasure = context.getMeasure(key);
    if (globalMutationsMeasure == null) {
      mutationsGlobal = Streams.parallelStream(context.getChildrenMeasures(key))
          .mapToInt(Measure::getIntValue)
          .findFirst()
          .orElse(0);
      LOG.info("Component {} has no global mutation information, using first child's: {}", context.getComponent(), mutationsGlobal);
    } else {
      mutationsGlobal = globalMutationsMeasure.getIntValue();
      LOG.info("Using global mutation count: {}", mutationsGlobal);
    }
    return mutationsGlobal;
  }

  private double getTestKillsLocal(final MeasureComputerContext context) {
    final Measure localTestKills = context.getMeasure(TEST_KILLS_KEY);
    if (localTestKills == null) {
      return Streams.parallelStream(context.getChildrenMeasures(TEST_KILLS_KEY))
          .mapToInt(Measure::getIntValue)
          .sum();
    } else {
      return localTestKills.getIntValue();
    }
  }

}
