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

import ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin;
import org.slf4j.Logger;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.CoreMetrics;

/**
 * Computer for calculating the mutation coverage of a component based on the detected vs total mutations.
 */
public class MutationDensityComputer implements MeasureComputer {

  private static final Logger LOG = getLogger(MutationDensityComputer.class);

  private Configuration config;

  public MutationDensityComputer(final Configuration config) {

    this.config = config;
  }

  @Override
  public MeasureComputerDefinition define(final MeasureComputerDefinitionContext defContext) {

    return defContext.newDefinitionBuilder()
                     .setInputMetrics(MutationMetrics.MUTATIONS_TOTAL.key(), CoreMetrics.LINES_TO_COVER_KEY)
                     .setOutputMetrics(MutationMetrics.MUTATIONS_DENSITY.key())
                     .build();
  }

  @Override
  public void compute(final MeasureComputerContext context) {

    if (!MutationAnalysisPlugin.isExperimentalFeaturesEnabled(config)) {
      LOG.info("Experimental Features disabled");
      return;
    }

    LOG.info("Computing Mutation Density for {}", context.getComponent());
    final Measure lines = context.getMeasure(CoreMetrics.LINES_TO_COVER_KEY);
    final Measure mutations = context.getMeasure(MutationMetrics.MUTATIONS_TOTAL.key());

    if (mutations != null) {
      final Double density;
      if (lines != null) {
        density = 100.0 * ((double) mutations.getIntValue() / (double) lines.getIntValue());
      } else {
        density = 0d;
      }
      LOG.info("Computed Mutation Density of {} for {}", density, context.getComponent());
      context.addMeasure(MutationMetrics.MUTATIONS_DENSITY.key(), density);
    }
  }
}
