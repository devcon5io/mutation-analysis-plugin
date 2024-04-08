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

import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.MUTATIONS_COVERAGE_KEY;
import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.MUTATIONS_DETECTED_KEY;
import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.MUTATIONS_SURVIVED_KEY;
import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.MUTATIONS_TEST_STRENGTH_KEY;
import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.MUTATIONS_TOTAL_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin;
import ch.devcon5.sonar.plugins.mutationanalysis.testharness.MeasureComputerTestHarness;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.ce.measure.test.TestMeasureComputerContext;
import org.sonar.api.ce.measure.test.TestMeasureComputerDefinitionContext;

class MutationScoreComputerTest {

  private MutationScoreComputer computer;
  private MeasureComputerTestHarness<MutationScoreComputer> harness;

  @BeforeEach
  public void setUp() {
    this.harness = MeasureComputerTestHarness.createFor(MutationScoreComputer.class);
    this.computer = harness.getComputer();
    setForceMissingCoverageToZero(null);

  }

  @Test
  void define() {
    final TestMeasureComputerDefinitionContext context = new TestMeasureComputerDefinitionContext();
    final MeasureComputer.MeasureComputerDefinition def = computer.define(context);

    assertEquals(def.getInputMetrics(),
        new HashSet<>(Arrays.asList(MUTATIONS_DETECTED_KEY, MUTATIONS_TOTAL_KEY, MUTATIONS_SURVIVED_KEY)));
    assertEquals(def.getOutputMetrics(),
        new HashSet<>(Arrays.asList(MUTATIONS_COVERAGE_KEY, MUTATIONS_TEST_STRENGTH_KEY)));
  }

  @Test
  void compute_noMutations_forceToZeroNotConfigured_defaultsToFalse_noMeasure() {
    final TestMeasureComputerContext measureContext = harness.createMeasureContextForSourceFile("compKey");

    computer.compute(measureContext);

    assertNull(measureContext.getMeasure(MUTATIONS_COVERAGE_KEY));
  }

  @Test
  void compute_noMutations_ForceTo0Disabled_noMeasure() {
    setForceMissingCoverageToZero(false);
    final TestMeasureComputerContext measureContext = harness.createMeasureContextForSourceFile("compKey");

    computer.compute(measureContext);

    assertNull(measureContext.getMeasure(MUTATIONS_COVERAGE_KEY));
  }

  @Test
  void compute_noMutations_forceTo0Enabled_noMeasure() {
    setForceMissingCoverageToZero(true);
    final TestMeasureComputerContext measureContext = harness.createMeasureContextForSourceFile("compKey");

    computer.compute(measureContext);

    assertEquals(0.0, measureContext.getMeasure(MUTATIONS_COVERAGE_KEY).getDoubleValue(), 0.05);
    assertEquals(0.0, measureContext.getMeasure(MUTATIONS_TEST_STRENGTH_KEY).getDoubleValue(), 0.05);
  }

  @Test
  void compute_0totalMutations_to_100percentCoverage() {
    final TestMeasureComputerContext measureContext = harness.createMeasureContextForSourceFile("compKey");

    measureContext.addInputMeasure(MUTATIONS_TOTAL_KEY, 0);

    computer.compute(measureContext);

    assertEquals(100.0, measureContext.getMeasure(MUTATIONS_COVERAGE_KEY).getDoubleValue(), 0.05);
    assertEquals(100.0, measureContext.getMeasure(MUTATIONS_TEST_STRENGTH_KEY).getDoubleValue(), 0.05);
  }

  @Test
  void compute_3of5Mutations_to_60percentCoverage() {
    final TestMeasureComputerContext measureContext = harness.createMeasureContextForSourceFile("compKey");

    measureContext.addInputMeasure(MUTATIONS_TOTAL_KEY, 5);
    measureContext.addInputMeasure(MUTATIONS_DETECTED_KEY, 3);

    computer.compute(measureContext);

    assertEquals(60.0, measureContext.getMeasure(MUTATIONS_COVERAGE_KEY).getDoubleValue(), 0.05);
  }

  @Test
  void compute_1of5MutationsKilled_to_20percentTestStrength() {
    final TestMeasureComputerContext measureContext = harness.createMeasureContextForSourceFile("compKey");

    measureContext.addInputMeasure(MUTATIONS_TOTAL_KEY, 5);
    measureContext.addInputMeasure(MUTATIONS_SURVIVED_KEY, 4);
    measureContext.addInputMeasure(MUTATIONS_DETECTED_KEY, 1);

    computer.compute(measureContext);

    assertEquals(20.0, measureContext.getMeasure(MUTATIONS_TEST_STRENGTH_KEY).getDoubleValue(), 0.05);
  }

  @Test
  void compute_SurvivedAndDetectedAreInconsitent_testStrengthDefaultsTo0() {
    final TestMeasureComputerContext measureContext = harness.createMeasureContextForSourceFile("compKey");

    measureContext.addInputMeasure(MUTATIONS_TOTAL_KEY, 1);
    measureContext.addInputMeasure(MUTATIONS_SURVIVED_KEY, 0);
    measureContext.addInputMeasure(MUTATIONS_DETECTED_KEY, 0);

    computer.compute(measureContext);

    assertEquals(0.0, measureContext.getMeasure(MUTATIONS_TEST_STRENGTH_KEY).getDoubleValue(), 0.05);
  }

  @Test
  void compute_5of5MutationsKilled_missingSurvivedMetric_to_100percentTestStrength() {
    final TestMeasureComputerContext measureContext = harness.createMeasureContextForSourceFile("compKey");

    //we dont set the MUTATIONS_SURVIVED_KEY metric (defaulting to 0)
    measureContext.addInputMeasure(MUTATIONS_TOTAL_KEY, 5);
    measureContext.addInputMeasure(MUTATIONS_DETECTED_KEY, 5);

    computer.compute(measureContext);

    assertEquals(100.0, measureContext.getMeasure(MUTATIONS_TEST_STRENGTH_KEY).getDoubleValue(), 0.05);
  }


  @Test
  void compute_NoCoveredElements_0percentCoverage() {
    final TestMeasureComputerContext measureContext = harness.createMeasureContextForSourceFile("compKey");

    measureContext.addInputMeasure(MUTATIONS_TOTAL_KEY, 5);

    computer.compute(measureContext);

    assertEquals(0.0, measureContext.getMeasure(MUTATIONS_COVERAGE_KEY).getDoubleValue(), 0.05);
    assertEquals(0.0, measureContext.getMeasure(MUTATIONS_TEST_STRENGTH_KEY).getDoubleValue(), 0.05);
  }

  private void setForceMissingCoverageToZero(Boolean enabled) {
    harness.getConfig().set(MutationAnalysisPlugin.FORCE_MISSING_COVERAGE_TO_ZERO, enabled);
  }

}
