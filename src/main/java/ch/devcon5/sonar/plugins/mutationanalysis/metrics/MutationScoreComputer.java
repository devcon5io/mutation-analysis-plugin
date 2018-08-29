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

import org.slf4j.Logger;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.config.Configuration;

/**
 * Computer for calculating the mutation coverage of a component based on the detected vs total mutations.
 */
public class MutationScoreComputer implements MeasureComputer {

    private static final Logger LOG = getLogger(MutationScoreComputer.class);

    private Configuration config;

    public MutationScoreComputer(final Configuration config) {

        this.config = config;
    }

    @Override
    public MeasureComputerDefinition define(final MeasureComputerDefinitionContext defContext) {

        return defContext.newDefinitionBuilder()
                         .setInputMetrics(MutationMetrics.MUTATIONS_DETECTED.key(), MutationMetrics.MUTATIONS_TOTAL.key())
                         .setOutputMetrics(MutationMetrics.MUTATIONS_COVERAGE.key())
                         .build();
    }

    @Override
    public void compute(final MeasureComputerContext context) {

        LOG.info("Computing Mutation Coverage for {}", context.getComponent());
        final Measure mutationsTotal = context.getMeasure(MutationMetrics.MUTATIONS_TOTAL.key());
        if(mutationsTotal != null)  {
            final Integer elements = mutationsTotal.getIntValue();
            if (elements > 0) {
                final Measure coveredElementsMeasure = context.getMeasure(MutationMetrics.MUTATIONS_DETECTED.key());
                final Integer coveredElements;
                if(coveredElementsMeasure != null){
                    coveredElements = coveredElementsMeasure.getIntValue();
                } else {
                    coveredElements = 0;
                }
                final Double coverage = 100.0 * coveredElements / elements;
                LOG.info("Computed Mutation Coverage of {}% for {}", coverage, context.getComponent());
                context.addMeasure(MutationMetrics.MUTATIONS_COVERAGE.key(), coverage);
            } else {
                //modules with no mutants (0 total) are always 100% covered (0 of 0 is 100%, right?)
                context.addMeasure(MutationMetrics.MUTATIONS_COVERAGE.key(), 100.0);
            }
        } else if(config.getBoolean(FORCE_MISSING_COVERAGE_TO_ZERO).orElse(Boolean.FALSE)){
            context.addMeasure(MutationMetrics.MUTATIONS_COVERAGE.key(), 0.0);
        }
    }
}
