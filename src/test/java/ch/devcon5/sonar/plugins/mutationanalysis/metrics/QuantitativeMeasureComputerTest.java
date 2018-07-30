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

import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.ce.measure.test.TestMeasureComputerContext;
import org.sonar.api.ce.measure.test.TestMeasureComputerDefinitionContext;

/**
 *
 */
public class QuantitativeMeasureComputerTest {


  private MeasureComputerTestHarness<QuantitativeMeasureComputer> harness;
  private QuantitativeMeasureComputer computer;

  @Before
  public void setUp() throws Exception {
    this.harness = MeasureComputerTestHarness.createFor(QuantitativeMeasureComputer.class);
    this.computer = harness.getComputer();
  }

  @Test
  public void define() {

    final TestMeasureComputerDefinitionContext context = new TestMeasureComputerDefinitionContext();

    final MeasureComputer.MeasureComputerDefinition def = computer.define(context);

    assertTrue(def.getOutputMetrics().containsAll(Arrays.asList(MUTATIONS_TOTAL_KEY,
                                                                MUTATIONS_NO_COVERAGE_KEY,
                                                                MUTATIONS_DETECTED_KEY,
                                                                MUTATIONS_ALIVE_KEY,
                                                                MUTATIONS_KILLED_KEY,
                                                                MUTATIONS_UNKNOWN_KEY,
                                                                MUTATIONS_TIMED_OUT_KEY,
                                                                MUTATIONS_MEMORY_ERROR_KEY,
                                                                MUTATIONS_SURVIVED_KEY,
                                                                TEST_KILLS_KEY,
                                                                UTILITY_GLOBAL_MUTATIONS_KEY,
                                                                UTILITY_GLOBAL_ALIVE_KEY)));
  }

  @Test
  public void compute_sumChildMeasures() {
    final TestMeasureComputerContext measureContext = harness.createMeasureContextForSourceFile("compKey");

    measureContext.addChildrenMeasures(MUTATIONS_TOTAL_KEY, 1,2,3);
    measureContext.addChildrenMeasures(MUTATIONS_NO_COVERAGE_KEY, 4,5,6);
    measureContext.addChildrenMeasures(MUTATIONS_DETECTED_KEY, 7,8,9);
    measureContext.addChildrenMeasures(MUTATIONS_KILLED_KEY, 10,11,12);
    measureContext.addChildrenMeasures(MUTATIONS_TIMED_OUT_KEY, 13,14,15);
    measureContext.addChildrenMeasures(MUTATIONS_MEMORY_ERROR_KEY, 16,17,18);
    measureContext.addChildrenMeasures(MUTATIONS_SURVIVED_KEY, 19,20,21);
    measureContext.addChildrenMeasures(TEST_KILLS_KEY, 22,23,24);
    measureContext.addChildrenMeasures(UTILITY_GLOBAL_MUTATIONS_KEY, 10,10,10);
    measureContext.addChildrenMeasures(UTILITY_GLOBAL_ALIVE_KEY, 20,20,20);

    computer.compute(measureContext);

    assertEquals(6, measureContext.getMeasure(MUTATIONS_TOTAL_KEY).getIntValue());
    assertEquals(15, measureContext.getMeasure(MUTATIONS_NO_COVERAGE_KEY).getIntValue());
    assertEquals(24, measureContext.getMeasure(MUTATIONS_DETECTED_KEY).getIntValue());
    assertEquals(33, measureContext.getMeasure(MUTATIONS_KILLED_KEY).getIntValue());
    assertEquals(42, measureContext.getMeasure(MUTATIONS_TIMED_OUT_KEY).getIntValue());
    assertEquals(51, measureContext.getMeasure(MUTATIONS_MEMORY_ERROR_KEY).getIntValue());
    assertEquals(60, measureContext.getMeasure(MUTATIONS_SURVIVED_KEY).getIntValue());
    assertEquals(69, measureContext.getMeasure(TEST_KILLS_KEY).getIntValue());
    assertEquals(10, measureContext.getMeasure(UTILITY_GLOBAL_MUTATIONS_KEY).getIntValue());
    assertEquals(20, measureContext.getMeasure(UTILITY_GLOBAL_ALIVE_KEY).getIntValue());
  }

  @Test
  public void compute_childMeasuresAre0_noValuePropagated() {
    final TestMeasureComputerContext measureContext = harness.createMeasureContextForSourceFile("compKey");

    measureContext.addChildrenMeasures(MUTATIONS_TOTAL_KEY, 0,0,0);
    measureContext.addChildrenMeasures(MUTATIONS_NO_COVERAGE_KEY, 0,0,0);
    measureContext.addChildrenMeasures(MUTATIONS_DETECTED_KEY, 0,0,0);
    measureContext.addChildrenMeasures(MUTATIONS_KILLED_KEY, 0,0,0);
    measureContext.addChildrenMeasures(MUTATIONS_TIMED_OUT_KEY, 0,0,0);
    measureContext.addChildrenMeasures(MUTATIONS_MEMORY_ERROR_KEY, 0,0,0);
    measureContext.addChildrenMeasures(MUTATIONS_SURVIVED_KEY, 0,0,0);
    measureContext.addChildrenMeasures(TEST_KILLS_KEY, 0,0,0);
    measureContext.addChildrenMeasures(UTILITY_GLOBAL_MUTATIONS_KEY, 0,0,0);
    measureContext.addChildrenMeasures(UTILITY_GLOBAL_ALIVE_KEY, 0,0,0);

    computer.compute(measureContext);

    assertNull(measureContext.getMeasure(MUTATIONS_TOTAL_KEY));
    assertNull(measureContext.getMeasure(MUTATIONS_NO_COVERAGE_KEY));
    assertNull(measureContext.getMeasure(MUTATIONS_DETECTED_KEY));
    assertNull(measureContext.getMeasure(MUTATIONS_KILLED_KEY));
    assertNull(measureContext.getMeasure(MUTATIONS_TIMED_OUT_KEY));
    assertNull(measureContext.getMeasure(MUTATIONS_MEMORY_ERROR_KEY));
    assertNull(measureContext.getMeasure(MUTATIONS_SURVIVED_KEY));
    assertNull(measureContext.getMeasure(TEST_KILLS_KEY));
    assertNull(measureContext.getMeasure(UTILITY_GLOBAL_MUTATIONS_KEY));
    assertNull(measureContext.getMeasure(UTILITY_GLOBAL_ALIVE_KEY));
  }


  @Test
  public void compute_childMeasuresAreNull_noValuePropagated() {
    final TestMeasureComputerContext measureContext = harness.createMeasureContextForSourceFile("compKey");

    computer.compute(measureContext);

    assertNull(measureContext.getMeasure(MUTATIONS_TOTAL_KEY));
    assertNull(measureContext.getMeasure(MUTATIONS_NO_COVERAGE_KEY));
    assertNull(measureContext.getMeasure(MUTATIONS_DETECTED_KEY));
    assertNull(measureContext.getMeasure(MUTATIONS_KILLED_KEY));
    assertNull(measureContext.getMeasure(MUTATIONS_TIMED_OUT_KEY));
    assertNull(measureContext.getMeasure(MUTATIONS_MEMORY_ERROR_KEY));
    assertNull(measureContext.getMeasure(MUTATIONS_SURVIVED_KEY));
    assertNull(measureContext.getMeasure(TEST_KILLS_KEY));
    assertNull(measureContext.getMeasure(UTILITY_GLOBAL_MUTATIONS_KEY));
    assertNull(measureContext.getMeasure(UTILITY_GLOBAL_ALIVE_KEY));
  }
}
