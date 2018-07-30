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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.ce.measure.test.TestMeasureComputerContext;
import org.sonar.api.ce.measure.test.TestMeasureComputerDefinitionContext;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TotalMutationsComputerTest {

  public static final String GLOBAL_MUTATIONS = MutationMetrics.UTILITY_GLOBAL_MUTATIONS.key();
  public static final String GLOBAL_ALIVE = MutationMetrics.UTILITY_GLOBAL_ALIVE.key();
  public static final String MUTATIONS_TOTAL = MutationMetrics.MUTATIONS_TOTAL.key();
  public static final String MUTATIONS_ALIVE = MutationMetrics.MUTATIONS_ALIVE.key();

  public static final String TOTAL_PERCENT = MutationMetrics.MUTATIONS_TOTAL_PERCENT.key();
  public static final String ALIVE_PERCENT = MutationMetrics.MUTATIONS_ALIVE_PERCENT.key();

  private MeasureComputerTestHarness<TotalMutationsComputer> harness;

  private TotalMutationsComputer computer;

  @Before
  public void setUp() throws Exception {
    this.harness = MeasureComputerTestHarness.createFor(TotalMutationsComputer.class);
    this.computer = harness.getComputer();
  }

  @Test
  public void define() {

    final TestMeasureComputerDefinitionContext context = new TestMeasureComputerDefinitionContext();

    final MeasureComputer.MeasureComputerDefinition def = computer.define(context);

    assertTrue(def.getInputMetrics().containsAll(Arrays.asList(GLOBAL_MUTATIONS, GLOBAL_ALIVE, MUTATIONS_TOTAL, MUTATIONS_ALIVE)));
    assertTrue(def.getOutputMetrics().containsAll(Arrays.asList(TOTAL_PERCENT, ALIVE_PERCENT)));

  }

  @Test
  public void compute_experimentalFeaturesDisabled_noMeasure() {

    harness.enableExperimentalFeatures(false);

    final TestMeasureComputerContext measureContext = harness.createMeasureContextForSourceFile("compKey");

    computer.compute(measureContext);

    assertNull(measureContext.getMeasure(TOTAL_PERCENT));

  }

  @Test
  public void compute_moduleRootDirectory_noMeasure() {

    final TestMeasureComputerContext measureContext = harness.createMeasureContextForDirectory("module:/");

    computer.compute(measureContext);

    assertNull(measureContext.getMeasure(TOTAL_PERCENT));

  }
  @Test
  public void compute_pomXml_noMeasure() {

    final TestMeasureComputerContext measureContext = harness.createMeasureContextForSourceFile("module:/pom.xml");

    computer.compute(measureContext);

    assertNull(measureContext.getMeasure(TOTAL_PERCENT));

  }

  @Test
  public void compute_unitTest_noMeasure() {

    final TestMeasureComputerContext measureContext = harness.createMeasureContextForUnitTest("someTest");

    computer.compute(measureContext);

    assertNull(measureContext.getMeasure(TOTAL_PERCENT));

  }

  @Test
  public void compute_testSource_noMeasure() {

    final TestMeasureComputerContext measureContext = harness.createMeasureContextForDirectory("someTest:src/test/");

    computer.compute(measureContext);

    assertNull(measureContext.getMeasure(TOTAL_PERCENT));

  }

  @Test
  public void compute_noInputMeasures_outputValues_0() {

    final TestMeasureComputerContext measureContext = harness.createMeasureContextForSourceFile("compKey");

    computer.compute(measureContext);

    Measure total = measureContext.getMeasure(TOTAL_PERCENT);
    Measure alive = measureContext.getMeasure(ALIVE_PERCENT);

    assertEquals(0.0, total.getDoubleValue(), 0.05);
    assertEquals(0.0, alive.getDoubleValue(), 0.05);

  }

  @Test
  public void compute_noGlobalMutations_outputValues_0() {

    final TestMeasureComputerContext measureContext = harness.createMeasureContextForSourceFile("compKey");

    measureContext.addInputMeasure(GLOBAL_MUTATIONS, 0);
    measureContext.addInputMeasure(GLOBAL_ALIVE, 0);

    computer.compute(measureContext);

    Measure total = measureContext.getMeasure(TOTAL_PERCENT);
    Measure alive = measureContext.getMeasure(ALIVE_PERCENT);

    assertEquals(0.0, total.getDoubleValue(), 0.05);
    assertEquals(0.0, alive.getDoubleValue(), 0.05);

  }

  @Test
  public void compute_componentInputMeasure_correctOutputValues() {

    final TestMeasureComputerContext measureContext = harness.createMeasureContextForSourceFile("compKey");

    //80% of all mutations are in this component
    measureContext.addInputMeasure(GLOBAL_MUTATIONS, 10);
    measureContext.addInputMeasure(MUTATIONS_TOTAL, 8);

    //60% of all alive mutations are in this component
    measureContext.addInputMeasure(GLOBAL_ALIVE, 5);
    measureContext.addInputMeasure(MUTATIONS_ALIVE, 3);

    computer.compute(measureContext);

    Measure total = measureContext.getMeasure(TOTAL_PERCENT);
    Measure alive = measureContext.getMeasure(ALIVE_PERCENT);

    assertEquals(80.0, total.getDoubleValue(), 0.05);
    assertEquals(60.0, alive.getDoubleValue(), 0.05);

  }

  @Test
  public void compute_childInputMeasure_correctOutputValues() {

    final TestMeasureComputerContext measureContext = harness.createMeasureContextForModule("compKey");

    //80% of all mutations are in the child components
    measureContext.addChildrenMeasures(GLOBAL_MUTATIONS, 10,10,10,10);
    measureContext.addChildrenMeasures(MUTATIONS_TOTAL, 4,3,1);

    //60% of all alive mutations are in the child components
    measureContext.addChildrenMeasures(GLOBAL_ALIVE, 5, 5, 5, 5);
    measureContext.addChildrenMeasures(MUTATIONS_ALIVE, 2, 1);

    computer.compute(measureContext);

    Measure total = measureContext.getMeasure(TOTAL_PERCENT);
    Measure alive = measureContext.getMeasure(ALIVE_PERCENT);

    assertEquals(80.0, total.getDoubleValue(), 0.05);
    assertEquals(60.0, alive.getDoubleValue(), 0.05);

  }

}
