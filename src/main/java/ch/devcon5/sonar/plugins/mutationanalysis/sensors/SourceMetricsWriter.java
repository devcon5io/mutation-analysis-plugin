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

package ch.devcon5.sonar.plugins.mutationanalysis.sensors;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;

import ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics;
import ch.devcon5.sonar.plugins.mutationanalysis.metrics.ResourceMutationMetrics;
import ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant;
import org.slf4j.Logger;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.coverage.NewCoverage;

/**
 *
 */
public class SourceMetricsWriter {

  private static final Logger LOG = getLogger(SourceMetricsWriter.class);

  /**
   * Saves the information of the mutants the sensors context.
   *
   * @param metrics
   *     the mutant information parsed from the PIT report
   * @param context
   *     the current {@link org.sonar.api.batch.sensor.SensorContext}
   * @param globalMutants
   */
  public void writeMetrics(final Collection<ResourceMutationMetrics> metrics, final SensorContext context, final Collection<Mutant> globalMutants) {

    final int total = globalMutants.isEmpty() ? sumTotal(metrics) : globalMutants.size();
    final int alive = total - (globalMutants.isEmpty()
                               ? metrics.stream().mapToInt(rmm -> countDetected(rmm.getMutants())).sum()
                               : countDetected(globalMutants));

    for (final ResourceMutationMetrics resourceMetrics : metrics) {
      saveResourceMetrics(resourceMetrics, context);

      context.newMeasure().forMetric(MutationMetrics.UTILITY_GLOBAL_MUTATIONS).on(resourceMetrics.getResource()).withValue(total).save();
      context.newMeasure().forMetric(MutationMetrics.UTILITY_GLOBAL_ALIVE).on(resourceMetrics.getResource()).withValue(alive).save();
    }
  }

  private int countDetected(final Collection<Mutant> c) {

    return (int) c.stream().filter(Mutant::isDetected).count();
  }

  private int sumTotal(final Collection<ResourceMutationMetrics> metrics) {

    return (int) metrics.stream().mapToLong(ResourceMutationMetrics::getMutationsTotal).sum();
  }

  /**
   * Saves the {@link Mutant} metrics for the given resource in the SonarContext
   *
   * @param resourceMetrics
   *     the actual metrics for the resource to persist
   * @param context
   *     the context to register the metrics
   */
  private void saveResourceMetrics(final ResourceMutationMetrics resourceMetrics, final SensorContext context) {

    final InputFile resource = resourceMetrics.getResource();

    LOG.debug("Saving resource metrics for {}", resource);

    if (resourceMetrics.getMutationsKilled() > 0) {
      NewCoverage newCov = context.newCoverage().onFile(resource);
      for (Mutant m : resourceMetrics.getMutants()) {
        if (Mutant.State.KILLED == m.getState()) {
          newCov.lineHits(m.getLineNumber(), 1);
        }
      }
      newCov.save();
    }
    if (resource.type() == InputFile.Type.MAIN) {

      context.newMeasure().on(resource).forMetric(MutationMetrics.MUTATIONS_TOTAL).withValue(resourceMetrics.getMutationsTotal()).save();
      context.newMeasure().on(resource).forMetric(MutationMetrics.MUTATIONS_NO_COVERAGE).withValue(resourceMetrics.getMutationsNoCoverage()).save();
      context.newMeasure().on(resource).forMetric(MutationMetrics.MUTATIONS_KILLED).withValue(resourceMetrics.getMutationsKilled()).save();
      context.newMeasure().on(resource).forMetric(MutationMetrics.MUTATIONS_SURVIVED).withValue(resourceMetrics.getMutationsSurvived()).save();
      context.newMeasure()
             .on(resource)
             .forMetric(MutationMetrics.MUTATIONS_ALIVE)
             .withValue(resourceMetrics.getMutationsTotal() - resourceMetrics.getMutationsDetected())
             .save();
      context.newMeasure().on(resource).forMetric(MutationMetrics.MUTATIONS_MEMORY_ERROR).withValue(resourceMetrics.getMutationsMemoryError()).save();
      context.newMeasure().on(resource).forMetric(MutationMetrics.MUTATIONS_TIMED_OUT).withValue(resourceMetrics.getMutationsTimedOut()).save();
      context.newMeasure().on(resource).forMetric(MutationMetrics.MUTATIONS_UNKNOWN).withValue(resourceMetrics.getMutationsUnknown()).save();
      context.newMeasure().on(resource).forMetric(MutationMetrics.MUTATIONS_DETECTED).withValue(resourceMetrics.getMutationsDetected()).save();
    }
  }
}
