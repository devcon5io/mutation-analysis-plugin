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

package ch.devcon5.sonar.plugins.mutationanalysis.sensors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics;
import ch.devcon5.sonar.plugins.mutationanalysis.metrics.ResourceMutationMetrics;
import ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant;
import ch.devcon5.sonar.plugins.mutationanalysis.testharness.SensorTestHarness;
import ch.devcon5.sonar.plugins.mutationanalysis.testharness.TestSensorContext;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.sensor.measure.Measure;

/**
 *
 */
public class TestMetricsWriterTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private SensorTestHarness harness;

  @Before
  public void setUp() throws Exception {
    this.harness = SensorTestHarness.builder().withTempFolder(folder).build();
  }

  @Test
  public void writeMetrics_noMutants_noMetrics_noMeasure() {

    //arrange
    final Collection<ResourceMutationMetrics> metrics = Collections.emptyList();
    final TestSensorContext context = harness.createSensorContext();
    final Collection<Mutant> globalMutants = Collections.emptyList();

    //act
    final TestMetricsWriter smw = new TestMetricsWriter(context.fileSystem());
    smw.writeMetrics(metrics, context, globalMutants);

    //assert
    assertTrue(context.getStorage().getMeasures().isEmpty());
  }

  @Test
  public void writeMetrics_oneMutant_withKillingTest_measureCreated() throws Exception {

    //arrange
    final TestSensorContext context = harness.createSensorContext();
    context.addTestFile("CustomTest.java");
    final Collection<Mutant> globalMutants = Collections.emptyList();
    final Collection<ResourceMutationMetrics> metrics = generateMutantMetrics(context, "CustomTest");

    //act
    final TestMetricsWriter smw = new TestMetricsWriter(context.fileSystem());
    smw.writeMetrics(metrics, context, globalMutants);

    //assert
    final Map<String, Serializable> measures = getMeasuresByKey("test-module:CustomTest.java", context);
    assertEquals(2, measures.size());
    assertEquals(5, measures.get(MutationMetrics.TEST_KILLS_KEY));
    assertEquals(7, measures.get(MutationMetrics.UTILITY_GLOBAL_MUTATIONS_KEY));
  }

  @Test
  public void writeMetrics_oneMutant_withGlobalMutants_measureCreated() throws Exception {

    //arrange
    final TestSensorContext context = harness.createSensorContext();
    context.addTestFile("CustomTest.java");
    final Collection<Mutant> globalMutants = generateMutants(20);
    //generates mutant metrics that killed 5 tests
    final Collection<ResourceMutationMetrics> metrics = generateMutantMetrics(context, "CustomTest");

    //act
    final TestMetricsWriter smw = new TestMetricsWriter(context.fileSystem());
    smw.writeMetrics(metrics, context, globalMutants);

    //assert
    final Map<String, Serializable> measures = getMeasuresByKey("test-module:CustomTest.java", context);
    assertEquals(2, measures.size());
    assertEquals(5, measures.get(MutationMetrics.TEST_KILLS_KEY));
    assertEquals(20, measures.get(MutationMetrics.UTILITY_GLOBAL_MUTATIONS_KEY));
  }

  @Test
  public void writeMetrics_oneMutant_killingTestEmpty_noMeasureCreated() throws Exception {

    //arrange
    final TestSensorContext context = harness.createSensorContext();
    context.addTestFile("CustomTest.java");
    final Collection<Mutant> globalMutants = Collections.emptyList();
    final Collection<ResourceMutationMetrics> metrics = generateMutantMetrics(context, "");

    //act
    final TestMetricsWriter smw = new TestMetricsWriter(context.fileSystem());
    smw.writeMetrics(metrics, context, globalMutants);

    //assert
    final Map<String, Serializable> measures = getMeasuresByKey("test-module:CustomTest.java", context);
    assertTrue(measures.isEmpty());
  }

  @NotNull
  private List<ResourceMutationMetrics> generateMutantMetrics(final TestSensorContext context, String testname) {

    return Arrays.asList(context.newResourceMutationMetrics("Product.java", md -> {
      md.lines = 100;
      md.mutants.survived = 2;
      md.mutants.killed = 5;
      md.test.name = testname;
    }));
  }

  @Test
  public void writeMetrics_moreMutants_measureCreated() throws Exception {

    //arrange
    final TestSensorContext context = harness.createSensorContext();
    context.addTestFile("CustomTest.java");
    context.addTestFile("OtherTest.java");
    final Collection<Mutant> globalMutants = Collections.emptyList();
    final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Product.java", md -> {
      md.lines = 100;
      md.mutants.survived = 2;
      md.mutants.killed = 5;
      md.test.name = "CustomTest";
    }), context.newResourceMutationMetrics("Other.java", md -> {
      md.lines = 50;
      md.mutants.survived = 3;
      md.mutants.killed = 2;
      md.test.name = "OtherTest";
    }));

    //act
    final TestMetricsWriter smw = new TestMetricsWriter(context.fileSystem());
    smw.writeMetrics(metrics, context, globalMutants);

    //assert
    final Map<String, Serializable> measures1 = getMeasuresByKey("test-module:CustomTest.java", context);
    assertEquals(2, measures1.size());
    assertEquals(5, measures1.get(MutationMetrics.TEST_KILLS_KEY));
    assertEquals(12, measures1.get(MutationMetrics.UTILITY_GLOBAL_MUTATIONS_KEY));

    final Map<String, Serializable> measures2 = getMeasuresByKey("test-module:OtherTest.java", context);
    assertEquals(2, measures2.size());
    assertEquals(2, measures2.get(MutationMetrics.TEST_KILLS_KEY));
    assertEquals(12, measures2.get(MutationMetrics.UTILITY_GLOBAL_MUTATIONS_KEY));
  }
  private Map<String, Serializable> getMeasuresByKey(String expectedComponentKey, final TestSensorContext context) {

    return context.getStorage()
                  .getMeasures()
                  .stream()
                  .filter(m -> expectedComponentKey.equals(m.inputComponent().key()))
                  .collect(Collectors.toMap(m -> m.metric().key(), Measure::value));
  }
  private List<Mutant> generateMutants(final int count) {

    return IntStream.range(0, count).mapToObj(this::newMutant).collect(Collectors.toList());
  }

  private Mutant newMutant(int i) {

    return Mutant.builder()
                 .mutantStatus(Mutant.State.KILLED)
                 .inSourceFile("Example.java")
                 .inClass("Example")
                 .inMethod("aMethod")
                 .withMethodParameters("()")
                 .usingMutator("BOOLEAN_FALSE_RETURN")
                 .killedBy("Test")
                 .inLine(i)
                 .build();
  }

}
