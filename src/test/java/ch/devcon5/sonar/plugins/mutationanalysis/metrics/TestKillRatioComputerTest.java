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

import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.TEST_KILLS_KEY;
import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.TEST_KILL_RATIO_KEY;
import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.UTILITY_GLOBAL_MUTATIONS_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.devcon5.sonar.plugins.mutationanalysis.testharness.MeasureComputerTestHarness;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.ce.measure.test.TestMeasureComputerContext;
import org.sonar.api.ce.measure.test.TestMeasureComputerDefinitionContext;

/**
 * Test Kill Ratio Computer Tests
 */
class TestKillRatioComputerTest {

  private MeasureComputerTestHarness<TestKillRatioComputer> harness;

  private TestKillRatioComputer computer;

  @BeforeEach
  public void setUp() throws Exception {
    this.harness = MeasureComputerTestHarness.createFor(TestKillRatioComputer.class);
    this.computer = harness.getComputer();
  }

  @Test
  void define() {
    final TestMeasureComputerDefinitionContext context = new TestMeasureComputerDefinitionContext();
    final MeasureComputer.MeasureComputerDefinition def = computer.define(context);

    assertTrue(def.getInputMetrics().containsAll(Arrays.asList(UTILITY_GLOBAL_MUTATIONS_KEY, TEST_KILLS_KEY)));
    assertTrue(def.getOutputMetrics().containsAll(Collections.singletonList(TEST_KILL_RATIO_KEY)));
  }

  @Test
  void compute_noInputMeasures_noOutputMeasure() {
    final TestMeasureComputerContext measureContext = harness.createMeasureContextForUnitTest("compKey");

    computer.compute(measureContext);

    Measure total = measureContext.getMeasure(TEST_KILL_RATIO_KEY);

    //no measure is created if component contains no/0 input values
    assertNull(total);
  }

  @Test
  void compute_experimentalFeaturesDisable_noOutputMeasure() {
    harness.enableExperimentalFeatures(false);
    final TestMeasureComputerContext measureContext = harness.createMeasureContextForUnitTest("compKey");

    measureContext.addInputMeasure(UTILITY_GLOBAL_MUTATIONS_KEY, 10);
    measureContext.addInputMeasure(TEST_KILLS_KEY, 1);

    computer.compute(measureContext);

    Measure total = measureContext.getMeasure(TEST_KILL_RATIO_KEY);

    //no measure is created if component contains no/0 input values
    assertNull(total);
  }

  @Test
  void compute_directory_aggregatedOutputMeasure() {
    final TestMeasureComputerContext measureContext = harness.createMeasureContext("compKey", Component.Type.DIRECTORY);
    measureContext.addChildrenMeasures(UTILITY_GLOBAL_MUTATIONS_KEY, 10);
    measureContext.addChildrenMeasures(TEST_KILLS_KEY, 1);

    computer.compute(measureContext);

    Measure ratio = measureContext.getMeasure(TEST_KILL_RATIO_KEY);

    assertEquals(10.0, ratio.getDoubleValue(), 0.05);
  }

  @Test
  void compute_sourceFileIsNoUnitTest_noOutputMeasure() {
    final TestMeasureComputerContext measureContext = harness.createMeasureContext("compKey", Component.Type.FILE);
    measureContext.addInputMeasure(UTILITY_GLOBAL_MUTATIONS_KEY, 10);
    measureContext.addInputMeasure(TEST_KILLS_KEY, 1);

    computer.compute(measureContext);

    Measure total = measureContext.getMeasure(TEST_KILL_RATIO_KEY);

    //no measure is created if component contains no/0 input values
    assertNull(total);
  }

  @Test
  void compute_oneInputMeasure_computesOutputMeasure() {
    final TestMeasureComputerContext measureContext = harness.createMeasureContextForUnitTest("compKey");

    measureContext.addInputMeasure(UTILITY_GLOBAL_MUTATIONS_KEY, 10);
    measureContext.addInputMeasure(TEST_KILLS_KEY, 1);

    computer.compute(measureContext);

    Measure ratio = measureContext.getMeasure(TEST_KILL_RATIO_KEY);
    assertEquals(10.0, ratio.getDoubleValue(), 0.05);
  }

  @Test
  void compute_globalMutations0_noOutputMeasure() {
    final TestMeasureComputerContext measureContext = harness.createMeasureContextForUnitTest("compKey");

    measureContext.addInputMeasure(UTILITY_GLOBAL_MUTATIONS_KEY, 0);
    measureContext.addInputMeasure(TEST_KILLS_KEY, 0);

    computer.compute(measureContext);

    Measure ratio = measureContext.getMeasure(TEST_KILL_RATIO_KEY);
    assertNull(ratio);
  }

  @Test
  void compute_noGlobalMetrics_noOutputMeasure() {
    final TestMeasureComputerContext measureContext = harness.createMeasureContextForUnitTest("compKey");

    measureContext.addChildrenMeasures(TEST_KILLS_KEY, 3, 2, 1);

    computer.compute(measureContext);

    Measure ratio = measureContext.getMeasure(TEST_KILL_RATIO_KEY);
    assertNull(ratio);
  }

  @Test
  void compute_childInputMeasure_computesOutputMeasure() {
    final TestMeasureComputerContext measureContext = harness.createMeasureContextForUnitTest("compKey");

    measureContext.addChildrenMeasures(UTILITY_GLOBAL_MUTATIONS_KEY, 10, 10, 10, 10);
    measureContext.addChildrenMeasures(TEST_KILLS_KEY, 3, 2, 1);

    computer.compute(measureContext);

    Measure ratio = measureContext.getMeasure(TEST_KILL_RATIO_KEY);
    assertEquals(60.0, ratio.getDoubleValue(), 0.05);
  }

}
