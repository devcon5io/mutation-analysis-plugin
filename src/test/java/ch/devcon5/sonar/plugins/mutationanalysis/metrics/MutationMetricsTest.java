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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.Serializable;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.api.measures.Metric;

class MutationMetricsTest {

  @Test
  void testMetricConstants() {
    assertNotNull(MutationMetrics.MUTATIONS_COVERAGE);
    assertNotNull(MutationMetrics.MUTATIONS_TEST_STRENGTH);
    assertNotNull(MutationMetrics.MUTATIONS_DATA);
    assertNotNull(MutationMetrics.MUTATIONS_DETECTED);
    assertNotNull(MutationMetrics.MUTATIONS_KILLED);
    assertNotNull(MutationMetrics.MUTATIONS_MEMORY_ERROR);
    assertNotNull(MutationMetrics.MUTATIONS_NO_COVERAGE);
    assertNotNull(MutationMetrics.MUTATIONS_SURVIVED);
    assertNotNull(MutationMetrics.MUTATIONS_TIMED_OUT);
    assertNotNull(MutationMetrics.MUTATIONS_TOTAL);
    assertNotNull(MutationMetrics.MUTATIONS_UNKNOWN);
  }

  @Test
  void testGetQuantitativeMetrics() {
    final List<Metric<Serializable>> metrics = MutationMetrics.getQuantitativeMetrics();
    assertNotNull(metrics);
    assertFalse(metrics.isEmpty());
  }

  @Test
  void testGetSensorMetrics() {
    final List<Metric<Serializable>> metrics = MutationMetrics.getSensorMetrics();
    assertNotNull(metrics);
    assertFalse(metrics.isEmpty());
  }

}
