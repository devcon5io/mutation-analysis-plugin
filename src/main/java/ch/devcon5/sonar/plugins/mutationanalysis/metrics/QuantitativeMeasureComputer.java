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

import static org.slf4j.LoggerFactory.getLogger;

import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.measures.Metric;

/**
 * Computer that processes the aggregated quantitative metric for a component from all the quantitative metrics of its
 * children.
 */
public class QuantitativeMeasureComputer implements MeasureComputer {

  private static final Logger LOG = getLogger(QuantitativeMeasureComputer.class);

  @Override
  public MeasureComputerDefinition define(final MeasureComputerDefinitionContext defContext) {

      return defContext.newDefinitionBuilder().setOutputMetrics(MutationMetrics.getQuantitativeMetrics()
                                                                               .stream()
                                                                               .map(Metric::getKey)
                                                                               .toArray(String[]::new)).build();
  }

  @Override
  public void compute(final MeasureComputerContext context) {

    LOG.info("Computing quantitative mutation metrics for {}", context.getComponent());
    MutationMetrics.getQuantitativeMetrics()
                   .stream()
                   .filter(m -> !m.isPercentageType())
                   .filter(m -> !m.isHidden()) //exclude hidden metrics, see below
                   .map(Metric::getKey)
                   .filter(metricKey -> context.getMeasure(metricKey) == null)
                   .forEach(metricKey -> {
                     int sum = StreamSupport.stream(context.getChildrenMeasures(metricKey).spliterator(), false)
                                            .map(Measure::getIntValue)
                                            .reduce(0, (s, i) -> s + i);
                     if (sum > 0) {
                       LOG.info("Computed {} {} for {}", sum, metricKey, context.getComponent());
                       context.addMeasure(metricKey, sum);
                     }
                   });
    //hidden utility metric are globally constant for all components
    MutationMetrics.getQuantitativeMetrics()
                   .stream()
                   .filter(Metric::isHidden)
                   .map(Metric::getKey)
                   .filter(metricKey -> context.getMeasure(metricKey) == null)
                   .forEach(metricKey -> {
                     int first = StreamSupport.stream(context.getChildrenMeasures(metricKey).spliterator(), false)
                                            .map(Measure::getIntValue)
                                            .findFirst().orElse(0);
                     if (first > 0) {
                       LOG.info("Computed {} {} for {}", first, metricKey, context.getComponent());
                       context.addMeasure(metricKey, first);
                     }
                   });
  }
}
