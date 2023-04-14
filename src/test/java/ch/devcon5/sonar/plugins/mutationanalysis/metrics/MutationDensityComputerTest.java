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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sonar.api.measures.CoreMetrics.LINES_TO_COVER_KEY;

import ch.devcon5.sonar.plugins.mutationanalysis.testharness.MeasureComputerTestHarness;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.ce.measure.test.TestMeasureComputerContext;
import org.sonar.api.ce.measure.test.TestMeasureComputerDefinitionContext;

/**
 * Mutation Density Computer Tests
 */
class MutationDensityComputerTest {

  private MeasureComputerTestHarness<MutationDensityComputer> harness;
  private MutationDensityComputer computer;

  @BeforeEach
  public void setUp() throws Exception {
    this.harness = MeasureComputerTestHarness.createFor(MutationDensityComputer.class);
    this.computer = harness.getComputer();
  }

  @Test
  void define() {
    final TestMeasureComputerDefinitionContext context = new TestMeasureComputerDefinitionContext();

    final MeasureComputer.MeasureComputerDefinition def = computer.define(context);

    assertTrue(def.getInputMetrics()
        .containsAll(Arrays.asList(MUTATIONS_TOTAL_KEY, LINES_TO_COVER_KEY)));
    assertTrue(def.getOutputMetrics().containsAll(Collections.singletonList(MUTATIONS_DENSITY_KEY)));
  }

  @Test
  void compute_experimentalFeaturesDisabled_noMeasure() {
    harness.enableExperimentalFeatures(false);

    final TestMeasureComputerContext measureContext = harness.createMeasureContextForSourceFile("compKey");

    measureContext.addInputMeasure(MUTATIONS_TOTAL_KEY, 30);
    measureContext.addInputMeasure(LINES_TO_COVER_KEY, 20);

    computer.compute(measureContext);

    assertNull(measureContext.getMeasure(MUTATIONS_DENSITY_KEY));
  }

  @Test
  void compute_noMutations_noMeasure() {
    final TestMeasureComputerContext measureContext = harness.createMeasureContextForSourceFile("compKey");

    computer.compute(measureContext);

    assertNull(measureContext.getMeasure(MUTATIONS_DENSITY_KEY));
  }

  @Test
  void compute_withInputMeausresMutations_correctOutputMeasure() {
    final TestMeasureComputerContext measureContext = harness.createMeasureContextForSourceFile("compKey");

    measureContext.addInputMeasure(MUTATIONS_TOTAL_KEY, 30);
    measureContext.addInputMeasure(LINES_TO_COVER_KEY, 20);

    computer.compute(measureContext);

    Measure density = measureContext.getMeasure(MUTATIONS_DENSITY_KEY);
    assertEquals(150.0, density.getDoubleValue(), 0.05);
  }

  @Test
  void compute_ZeroMutationsAndZeroLines_noDensityMetric() {
    final TestMeasureComputerContext measureContext = harness.createMeasureContextForSourceFile("compKey");

    measureContext.addInputMeasure(MUTATIONS_TOTAL_KEY, 0);
    measureContext.addInputMeasure(LINES_TO_COVER_KEY, 0);

    computer.compute(measureContext);

    assertNull(measureContext.getMeasure(MUTATIONS_DENSITY_KEY));
  }

  @Test
  void compute_noLineMetrics_densitySetToZero() {
    final TestMeasureComputerContext measureContext = harness.createMeasureContextForSourceFile("compKey");

    measureContext.addInputMeasure(MUTATIONS_TOTAL_KEY, 30);

    computer.compute(measureContext);

    Measure density = measureContext.getMeasure(MUTATIONS_DENSITY_KEY);
    assertEquals(0.0, density.getDoubleValue(), 0.05);
  }

}
