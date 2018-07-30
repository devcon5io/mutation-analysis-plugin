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

import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.MUTATIONS_DENSITY_KEY;
import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.MUTATIONS_TOTAL_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.sonar.api.measures.CoreMetrics.LINES_TO_COVER_KEY;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.ce.measure.test.TestMeasureComputerContext;
import org.sonar.api.ce.measure.test.TestMeasureComputerDefinitionContext;

/**
 *
 */
public class MutationDensityComputerTest {

  public static final String MUTATION_DENSITY = MutationMetrics.MUTATIONS_DENSITY.key();
  private MeasureComputerTestHarness<MutationDensityComputer> harness;
  private MutationDensityComputer computer;

  @Before
  public void setUp() throws Exception {
    this.harness = MeasureComputerTestHarness.createFor(MutationDensityComputer.class);
    this.computer = harness.getComputer();
  }

  @Test
  public void define() {

    final TestMeasureComputerDefinitionContext context = new TestMeasureComputerDefinitionContext();

    final MeasureComputer.MeasureComputerDefinition def = computer.define(context);

    assertTrue(def.getInputMetrics()
                  .containsAll(Arrays.asList(MUTATIONS_TOTAL_KEY, LINES_TO_COVER_KEY)));
    assertTrue(def.getOutputMetrics().containsAll(Arrays.asList(MUTATIONS_DENSITY_KEY)));
  }

  @Test
  public void compute_experimentalFeaturesDisabled_noMeasure() {
    harness.enableExperimentalFeatures(false);

    final TestMeasureComputerContext measureContext = harness.createMeasureContextForSourceFile("compKey");

    computer.compute(measureContext);

    assertNull(measureContext.getMeasure(MUTATIONS_DENSITY_KEY));
  }

  @Test
  public void compute_noMutations_noMeasure() {
    final TestMeasureComputerContext measureContext = harness.createMeasureContextForSourceFile("compKey");

    computer.compute(measureContext);

    assertNull(measureContext.getMeasure(MUTATIONS_DENSITY_KEY));
  }

  @Test
  public void compute_withInputMeausresMutations_correctOutputMeasure() {
    final TestMeasureComputerContext measureContext = harness.createMeasureContextForSourceFile("compKey");

    measureContext.addInputMeasure(MUTATIONS_TOTAL_KEY, 30);
    measureContext.addInputMeasure(LINES_TO_COVER_KEY, 20);

    computer.compute(measureContext);

    Measure density = measureContext.getMeasure(MUTATIONS_DENSITY_KEY);
    assertEquals(150.0, density.getDoubleValue(), 0.05);
  }
}
