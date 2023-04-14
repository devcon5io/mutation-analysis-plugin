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

import static ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin.FORCE_MISSING_COVERAGE_TO_ZERO;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Optional;
import org.slf4j.Logger;
import org.sonar.api.batch.measure.Metric;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.config.Configuration;

/**
 * Computer for calculating the mutation coverage of a component based on the detected vs total mutations.
 */
public class MutationScoreComputer implements MeasureComputer {

  private static final Logger LOG = getLogger(MutationScoreComputer.class);

  private final Configuration config;

  public MutationScoreComputer(final Configuration config) {
    this.config = config;
  }

  @Override
  public MeasureComputerDefinition define(final MeasureComputerDefinitionContext defContext) {
    return defContext.newDefinitionBuilder()
        .setInputMetrics(MutationMetrics.MUTATIONS_DETECTED.key(), MutationMetrics.MUTATIONS_TOTAL.key(), MutationMetrics.MUTATIONS_SURVIVED.key())
        .setOutputMetrics(MutationMetrics.MUTATIONS_COVERAGE.key(), MutationMetrics.MUTATIONS_TEST_STRENGTH.key())
        .build();
  }

  @Override
  public void compute(final MeasureComputerContext context) {
    LOG.info("Computing Mutation Coverage for {}", context.getComponent());
    final Measure mutationsTotalMeasure = context.getMeasure(MutationMetrics.MUTATIONS_TOTAL.key());
    if (mutationsTotalMeasure != null) {
      final int mutationsTotal = mutationsTotalMeasure.getIntValue();
      if (mutationsTotal > 0) {
        final int detectedMutants = getMetric(context, MutationMetrics.MUTATIONS_DETECTED);
        final int survivedMutants = getMetric(context, MutationMetrics.MUTATIONS_SURVIVED);
        computeMutationsCoverage(context, detectedMutants, mutationsTotal);
        computeTestStrength(context, survivedMutants, detectedMutants);
      } else {
        // modules with no mutants (0 total) are always 100% covered (0 of 0 is 100%, right?)
        context.addMeasure(MutationMetrics.MUTATIONS_COVERAGE.key(), 100.0);
        context.addMeasure(MutationMetrics.MUTATIONS_TEST_STRENGTH.key(), 100.0);
      }
    } else if (config.getBoolean(FORCE_MISSING_COVERAGE_TO_ZERO).orElse(Boolean.FALSE)) {
      context.addMeasure(MutationMetrics.MUTATIONS_COVERAGE.key(), 0.0);
      context.addMeasure(MutationMetrics.MUTATIONS_TEST_STRENGTH.key(), 0.0);
    }
  }

  private void computeMutationsCoverage(final MeasureComputerContext context, final int coveredMutants, final int totalMutants) {
    final double coverage = 100.0 * coveredMutants / totalMutants;
    LOG.info("Computed Mutation Coverage of {}% for {}", coverage, context.getComponent());
    context.addMeasure(MutationMetrics.MUTATIONS_COVERAGE.key(), coverage);
  }

  private void computeTestStrength(final MeasureComputerContext context, final int survivedMutants, final int detectedMutants) {
    final double testStrength;
    int allMutantsQuantity = survivedMutants + detectedMutants;
    if (allMutantsQuantity != 0) {
      testStrength = 100.0 * detectedMutants / allMutantsQuantity;
    } else {
      testStrength = 0;
    }
    LOG.info("Computed Test Strength of {}% for {}", testStrength, context.getComponent());
    context.addMeasure(MutationMetrics.MUTATIONS_TEST_STRENGTH.key(), testStrength);
  }

  private int getMetric(final MeasureComputerContext context, Metric<?> metric) {
    return Optional.ofNullable(context.getMeasure(metric.key())).map(Measure::getIntValue).orElse(0);
  }

}
