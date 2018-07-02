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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.ce.measure.MeasureComputer.MeasureComputerDefinition.Builder;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class MutationScoreComputerTest {

    /**
     * The class under test
     */
    @InjectMocks
    private MutationScoreComputer subject;

    @Mock
    private MeasureComputer.MeasureComputerDefinitionContext definitionContext;

    @Mock
    private MeasureComputer.MeasureComputerContext computerContext;

    @Mock
    private MeasureComputer.MeasureComputerDefinition computerDefinition;

    @Mock
    private Builder builder;

    @Mock
    private Measure mutationTotal;

    @Mock
    private Measure mutationsCovered;

    @Test
    public void define() throws Exception {

        when(definitionContext.newDefinitionBuilder()).thenReturn(builder);
        when(builder.setInputMetrics(anyVararg())).thenReturn(builder);
        when(builder.setOutputMetrics(anyVararg())).thenReturn(builder);
        when(builder.build()).thenReturn(computerDefinition);

        MeasureComputer.MeasureComputerDefinition def = subject.define(definitionContext);

        verify(builder).setInputMetrics("dc5_mutationAnalysis_mutations_detected", "dc5_mutationAnalysis_mutations_total");
        verify(builder).setOutputMetrics("dc5_mutationAnalysis_mutations_coverage");
        assertEquals(computerDefinition, def);
    }

    @Test
    public void compute_noMutations() throws Exception {

        subject.compute(computerContext);

        verify(computerContext, times(0)).addMeasure(anyString(), anyDouble());
    }

    @Test
    public void compute_0totalMutations_to_100percentCoverage() throws Exception {
        when(computerContext.getMeasure("dc5_mutationAnalysis_mutations_total")).thenReturn(mutationTotal);

        subject.compute(computerContext);

        final ArgumentCaptor<Double> captor = forClass(double.class);
        verify(computerContext, times(1)).addMeasure(eq("dc5_mutationAnalysis_mutations_coverage"), captor.capture());
        final Double passedParam = captor.getValue();
        assertEquals(100.0, passedParam, 0.05);
    }

    @Test
    public void compute_3of5Mutations_to_60percentCoverage() throws Exception {
        when(mutationTotal.getIntValue()).thenReturn(5);
        when(mutationsCovered.getIntValue()).thenReturn(3);
        when(computerContext.getMeasure("dc5_mutationAnalysis_mutations_total")).thenReturn(mutationTotal);
        when(computerContext.getMeasure("dc5_mutationAnalysis_mutations_detected")).thenReturn(mutationsCovered);

        subject.compute(computerContext);

        final ArgumentCaptor<Double> captor = forClass(double.class);
        verify(computerContext, times(1)).addMeasure(eq("dc5_mutationAnalysis_mutations_coverage"), captor.capture());
        final Double passedParam = captor.getValue();
        assertEquals(60.0, passedParam, 0.05);
    }

}
