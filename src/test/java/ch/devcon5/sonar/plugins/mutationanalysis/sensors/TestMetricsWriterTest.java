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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import ch.devcon5.sonar.plugins.mutationanalysis.metrics.ResourceMutationMetrics;
import ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant;
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

  @Test
  public void writeMetrics_noMutants_noMetrics_noMeasure() {

    final TestSensorContext context = TestSensorContext.create(folder.getRoot().toPath(), "test-module");

    TestMetricsWriter smw = new TestMetricsWriter(context.fileSystem());

    final Collection<Mutant> globalMutants = Collections.emptyList();
    final Collection<ResourceMutationMetrics> metrics = Collections.emptyList();

    smw.writeMetrics(metrics, context, globalMutants);

    assertTrue(context.getStorage().getMeasures().isEmpty());
  }

  @Test
  public void writeMetrics_oneMutant_measureCreated() throws Exception {

    final TestSensorContext context = TestSensorContext.create(folder.getRoot().toPath(), "test-module");
    context.addTestFile("CustomTest.java");

    final TestMetricsWriter smw = new TestMetricsWriter(context.fileSystem());

    final Collection<Mutant> globalMutants = Collections.emptyList();
    final Collection<ResourceMutationMetrics> metrics = Arrays.asList(context.newResourceMutationMetrics("Product.java", md -> {
      md.lines = 100;
      md.mutants.survived = 2;
      md.mutants.killed = 5;
      md.test.name = "CustomTest";
    }));

    smw.writeMetrics(metrics, context, globalMutants);

    final List<Measure> measures = context.getStorage().getMeasures();

    assertContains(measures, m -> {
      assertEquals("test-module:CustomTest.java", m.inputComponent().key());
      assertEquals("dc5_mutationAnalysis_mutations_testkills", m.metric().key());
    });

    assertContains(measures, m -> {
      assertEquals("test-module:CustomTest.java", m.inputComponent().key());
      assertEquals("dc5_mutationAnalysis_mutations_global", m.metric().key());
    });
    assertEquals(2, measures.size());
  }

  @Test
  public void writeMetrics_moreMutants_measureCreated() throws Exception {

    final TestSensorContext context = TestSensorContext.create(folder.getRoot().toPath(), "test-module");
    context.addTestFile("CustomTest.java");
    context.addTestFile("OtherTest.java");

    final TestMetricsWriter smw = new TestMetricsWriter(context.fileSystem());

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

    smw.writeMetrics(metrics, context, globalMutants);

    final List<Measure> measures = context.getStorage().getMeasures();

    assertEquals(5, assertContains(measures, m -> {
      assertEquals("test-module:CustomTest.java", m.inputComponent().key());
      assertEquals("dc5_mutationAnalysis_mutations_testkills", m.metric().key());
    }).value());

    assertEquals(12, assertContains(measures, m -> {
      assertEquals("test-module:CustomTest.java", m.inputComponent().key());
      assertEquals("dc5_mutationAnalysis_mutations_global", m.metric().key());
    }).value());
    assertEquals(2,
    assertContains(measures, m -> {
      assertEquals("test-module:OtherTest.java", m.inputComponent().key());
      assertEquals("dc5_mutationAnalysis_mutations_testkills", m.metric().key());
    }).value());
    assertEquals(12, assertContains(measures, m -> {
      assertEquals("test-module:OtherTest.java", m.inputComponent().key());
      assertEquals("dc5_mutationAnalysis_mutations_global", m.metric().key());
    }).value());

    assertEquals(4, measures.size());
  }

  <G> G assertContains(List<G> container, Consumer<G> filter) {

    return container.stream().filter(m -> {
      try {
        filter.accept(m);
        return true;
      } catch (AssertionError e) {
        return false;
      }
    }).findFirst().orElseThrow(() -> new AssertionError("No element found matching assertions"));

  }
}
