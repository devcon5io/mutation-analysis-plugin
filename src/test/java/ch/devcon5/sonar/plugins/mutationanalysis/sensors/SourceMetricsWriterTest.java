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
import java.util.Map;
import java.util.stream.Collectors;

import ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics;
import ch.devcon5.sonar.plugins.mutationanalysis.metrics.ResourceMutationMetrics;
import ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.sensor.coverage.internal.DefaultCoverage;
import org.sonar.api.batch.sensor.measure.Measure;

/**
 *
 */
public class SourceMetricsWriterTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void writeMetrics_noMutants_noMetric_nothingWritten() {

    SourceMetricsWriter smw = new SourceMetricsWriter();

    final TestSensorContext context = TestSensorContext.create(folder.getRoot().toPath(), "test-module");
    final Collection<Mutant> globalMutants = Collections.emptyList();
    final Collection<ResourceMutationMetrics> metrics = Collections.emptyList();

    smw.writeMetrics(metrics, context, globalMutants);

    assertTrue(context.getStorage().getMeasures().isEmpty());
  }

  @Test
  public void writeMetrics_singleResourceMetrics_metricsWritten() throws Exception {

    SourceMetricsWriter smw = new SourceMetricsWriter();

    final TestSensorContext context = TestSensorContext.create(folder.getRoot().toPath(), "test-module");
    final Collection<Mutant> globalMutants = Collections.emptyList();
    final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test.java", md -> {
      md.lines = 100;
      md.mutants.unknown = 0;
      md.mutants.noCoverage = 1;
      md.mutants.survived = 2;
      md.mutants.memoryError = 3;
      md.mutants.timedOut = 4;
      md.mutants.killed = 5;
    }));

    smw.writeMetrics(metrics, context, globalMutants);

    final Map<String, Serializable> measures = getMeasuresByKey("test-module:Test.java", context);
    assertEquals(11, measures.size());

    assertEquals(15, measures.get(MutationMetrics.MUTATIONS_TOTAL.key()));
    assertEquals(1, measures.get(MutationMetrics.MUTATIONS_NO_COVERAGE.key()));
    assertEquals(5, measures.get(MutationMetrics.MUTATIONS_KILLED.key()));
    assertEquals(2, measures.get(MutationMetrics.MUTATIONS_SURVIVED.key()));
    assertEquals(3, measures.get(MutationMetrics.MUTATIONS_ALIVE.key()));
    assertEquals(3, measures.get(MutationMetrics.MUTATIONS_MEMORY_ERROR.key()));
    assertEquals(4, measures.get(MutationMetrics.MUTATIONS_TIMED_OUT.key()));
    assertEquals(0, measures.get(MutationMetrics.MUTATIONS_UNKNOWN.key()));
    assertEquals(12, measures.get(MutationMetrics.MUTATIONS_DETECTED.key()));
    assertEquals(15, measures.get(MutationMetrics.UTILITY_GLOBAL_MUTATIONS.key()));
    assertEquals(3, measures.get(MutationMetrics.UTILITY_GLOBAL_ALIVE.key()));

    //the coverage should be equal the number of killed mutations (so it's implemented in the testcontext)
    final DefaultCoverage coverages = getCoveragesByKey("Test.java", context);
    assertEquals(5, coverages.coveredLines());
  }

  private Map<String, Serializable> getMeasuresByKey(String expectedComponentKey, final TestSensorContext context) {

    return context.getStorage()
                  .getMeasures()
                  .stream()
                  .filter(m -> expectedComponentKey.equals(m.inputComponent().key()))
                  .collect(Collectors.toMap(m -> m.metric().key(), Measure::value));
  }

  private DefaultCoverage getCoveragesByKey(final String expectedInputFile, final TestSensorContext context) {

    return context.getStorage()
                  .getCoverages()
                  .stream()
                  .filter(c -> expectedInputFile.equals(c.inputFile().filename()))
                  .findFirst()
                  .orElseThrow(() -> new AssertionError("no input file found with filename=" + expectedInputFile));
  }

  @Test
  public void writeMetrics_multiResourceMetrics_metricsWritten() throws Exception {

    SourceMetricsWriter smw = new SourceMetricsWriter();

    final TestSensorContext context = TestSensorContext.create(folder.getRoot().toPath(), "test-module");

    final Collection<Mutant> globalMutants = Collections.emptyList();
    final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Test1.java", md -> {
      md.lines = 200;
      md.mutants.unknown = 0;
      md.mutants.noCoverage = 1;
      md.mutants.survived = 2;
      md.mutants.memoryError = 3;
      md.mutants.timedOut = 4;
      md.mutants.killed = 5;
    }), context.newResourceMutationMetrics("Test2.java", md -> {
      md.lines = 100;
      md.mutants.unknown = 1;
      md.mutants.noCoverage = 2;
      md.mutants.survived = 2;
      md.mutants.memoryError = 0;
      md.mutants.timedOut = 1;
      md.mutants.killed = 3;
    }));

    smw.writeMetrics(metrics, context, globalMutants);

    final Map<String, Serializable> values1 = getMeasuresByKey("test-module:Test1.java", context);
    assertEquals(11, values1.size());
    assertEquals(15, values1.get(MutationMetrics.MUTATIONS_TOTAL.key()));
    assertEquals(1, values1.get(MutationMetrics.MUTATIONS_NO_COVERAGE.key()));
    assertEquals(5, values1.get(MutationMetrics.MUTATIONS_KILLED.key()));
    assertEquals(2, values1.get(MutationMetrics.MUTATIONS_SURVIVED.key()));
    assertEquals(3, values1.get(MutationMetrics.MUTATIONS_ALIVE.key()));
    assertEquals(3, values1.get(MutationMetrics.MUTATIONS_MEMORY_ERROR.key()));
    assertEquals(4, values1.get(MutationMetrics.MUTATIONS_TIMED_OUT.key()));
    assertEquals(0, values1.get(MutationMetrics.MUTATIONS_UNKNOWN.key()));
    assertEquals(12, values1.get(MutationMetrics.MUTATIONS_DETECTED.key()));
    assertEquals(24, values1.get(MutationMetrics.UTILITY_GLOBAL_MUTATIONS.key()));
    assertEquals(8, values1.get(MutationMetrics.UTILITY_GLOBAL_ALIVE.key()));
    final Map<String, Serializable> values2 = getMeasuresByKey("test-module:Test2.java", context);
    assertEquals(11, values2.size());
    assertEquals(9, values2.get(MutationMetrics.MUTATIONS_TOTAL.key()));
    assertEquals(2, values2.get(MutationMetrics.MUTATIONS_NO_COVERAGE.key()));
    assertEquals(3, values2.get(MutationMetrics.MUTATIONS_KILLED.key()));
    assertEquals(2, values2.get(MutationMetrics.MUTATIONS_SURVIVED.key()));
    assertEquals(5, values2.get(MutationMetrics.MUTATIONS_ALIVE.key()));
    assertEquals(0, values2.get(MutationMetrics.MUTATIONS_MEMORY_ERROR.key()));
    assertEquals(1, values2.get(MutationMetrics.MUTATIONS_TIMED_OUT.key()));
    assertEquals(1, values2.get(MutationMetrics.MUTATIONS_UNKNOWN.key()));
    assertEquals(4, values2.get(MutationMetrics.MUTATIONS_DETECTED.key()));
    assertEquals(24, values2.get(MutationMetrics.UTILITY_GLOBAL_MUTATIONS.key()));
    assertEquals(8, values2.get(MutationMetrics.UTILITY_GLOBAL_ALIVE.key()));

    final DefaultCoverage coverage1 = getCoveragesByKey("Test1.java", context);
    assertEquals(5, coverage1.coveredLines());

    final DefaultCoverage coverage2 = getCoveragesByKey("Test2.java", context);
    assertEquals(3, coverage2.coveredLines());
  }
}
