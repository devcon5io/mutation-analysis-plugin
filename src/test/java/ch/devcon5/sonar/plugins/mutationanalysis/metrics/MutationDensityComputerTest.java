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
