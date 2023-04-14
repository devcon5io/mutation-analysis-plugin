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

package ch.devcon5.sonar.plugins.mutationanalysis.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collection;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Mutation Operators Test
 */
class MutationOperatorsTest {

  @ParameterizedTest
  @ValueSource(strings = {
      "ARGUMENT_PROPAGATION",
      "org.pitest.mutationtest.engine.gregor.mutators.ArgumentPropagationMutator",
      "org.pitest.mutationtest.engine.gregor.mutators.experimental.ArgumentPropagationMutator",
      "org.pitest.mutationtest.engine.gregor.mutators.ArgumentPropagationMutator_WITH_SUFFIX",
      "org.pitest.mutationtest.engine.gregor.mutators.experimental.ArgumentPropagationMutator_WITH_SUFFIX"
  })
  void testFind_knownMutator_byID(String mutagenKey) {
    final MutationOperator mutationOperator = MutationOperators.find(mutagenKey);
    assertNotNull(mutationOperator);
    assertEquals("ARGUMENT_PROPAGATION", mutationOperator.getId());
    assertEquals(new HashSet<String>() {{
      add("org.pitest.mutationtest.engine.gregor.mutators.ArgumentPropagationMutator");
      add("org.pitest.mutationtest.engine.gregor.mutators.experimental.ArgumentPropagationMutator");
    }}, mutationOperator.getClassNames());
    assertNotNull(mutationOperator.getViolationDescription());
  }

  @Test
  void testAllMutators() {
    // act
    final Collection<MutationOperator> mutationOperators = MutationOperators.allMutationOperators();

    // assert
    assertNotNull(mutationOperators);
    assertFalse(mutationOperators.isEmpty());
    assertEquals(23, mutationOperators.size());
  }

}
