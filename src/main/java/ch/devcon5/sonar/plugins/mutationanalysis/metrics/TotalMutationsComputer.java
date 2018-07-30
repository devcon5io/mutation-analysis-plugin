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

import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.*;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.stream.StreamSupport;

import ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin;
import org.slf4j.Logger;
import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.Metric;

public class TotalMutationsComputer implements MeasureComputer {

  private static final Logger LOG = getLogger(TotalMutationsComputer.class);

  private Configuration config;

  public TotalMutationsComputer(final Configuration config) {

    this.config = config;
  }

  @Override
  public MeasureComputerDefinition define(final MeasureComputerDefinitionContext defContext) {

    return defContext.newDefinitionBuilder()
                     .setInputMetrics(UTILITY_GLOBAL_MUTATIONS_KEY, UTILITY_GLOBAL_ALIVE_KEY, MUTATIONS_TOTAL_KEY, MUTATIONS_ALIVE_KEY)
                     .setOutputMetrics(MUTATIONS_TOTAL_PERCENT_KEY, MUTATIONS_ALIVE_PERCENT_KEY)
                     .build();
  }

  @Override
  public void compute(final MeasureComputerContext context) {

    if (!MutationAnalysisPlugin.isExperimentalFeaturesEnabled(config)) {
      LOG.info("Experimental features disabled");
      return;
    }
    final Component comp = context.getComponent();
    if (comp.getType() == Component.Type.DIRECTORY && comp.getKey().endsWith(":/") || comp.getType() == Component.Type.FILE && comp.getKey()
                                                                                                                                   .endsWith("pom.xml")) {
      LOG.info("Skipping module-root from analysis key={}", comp.getKey());
      return;
    }

    if ((comp.getType() == Component.Type.FILE && comp.getFileAttributes().isUnitTest()) || (comp.getType() == Component.Type.DIRECTORY && comp.getKey()
                                                                                                                                               .contains(
                                                                                                                                                   ":src/test/"))) {
      LOG.debug("Skipping test unit {} from processing", comp.getKey());
      return;
    }
    LOG.info("Calculating total mutation % for comp={} key={}", comp.getType(), comp.getKey());

    computePercentage(context, UTILITY_GLOBAL_MUTATIONS, MUTATIONS_TOTAL, MUTATIONS_TOTAL_PERCENT);
    computePercentage(context, UTILITY_GLOBAL_ALIVE, MUTATIONS_ALIVE, MUTATIONS_ALIVE_PERCENT);
  }

  private void computePercentage(final MeasureComputerContext context, final Metric globalMetric, final Metric localMetric, final Metric resultMetric) {

    final double mutationsGlobal = getMutationsGlobal(context, globalMetric);
    final double mutationsLocal = getMutationsLocal(context, localMetric);

    if (mutationsGlobal > 0.0) {
      final double percentage = 100.0 * mutationsLocal / mutationsGlobal;
      LOG.info("Computed {} of {}% from ({} / {}) for {}", resultMetric.getName(), percentage, mutationsLocal, mutationsGlobal, context.getComponent());
      context.addMeasure(resultMetric.key(), percentage);
    } else {
      LOG.info("No mutations found in project");
      context.addMeasure(resultMetric.key(), 0.0);
    }
  }

  private double getMutationsGlobal(final MeasureComputerContext context, Metric metric) {

    final double mutationsGlobal;
    final Measure globalMutationsMeasure = context.getMeasure(metric.key());

    if (globalMutationsMeasure == null) {
      mutationsGlobal = StreamSupport.stream(context.getChildrenMeasures(metric.key()).spliterator(), false)
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

  private double getMutationsLocal(final MeasureComputerContext context, Metric metric) {

    final double mutationsLocal;
    final Measure localMutationsMeasure = context.getMeasure(metric.key());

    if (localMutationsMeasure == null) {

      mutationsLocal = (double) StreamSupport.stream(context.getChildrenMeasures(metric.key()).spliterator(), false).mapToInt(Measure::getIntValue).sum();
      LOG.info("Component {} children have {} mutations ", context.getComponent(), mutationsLocal);

    } else {
      mutationsLocal = localMutationsMeasure.getIntValue();
      LOG.info("Component {} has no children, using local mutation count of {}", context.getComponent(), mutationsLocal);

    }
    return mutationsLocal;
  }

}
