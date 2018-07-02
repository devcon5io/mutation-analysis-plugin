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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;

import org.junit.Test;

/**
 *
 */
public class MutationOperatorsTest {

    @Test
    public void testFind_knownMutator_byID() throws Exception {

        final MutationOperator mutationOperator = MutationOperators.find("ARGUMENT_PROPAGATION");
        assertNotNull(mutationOperator);
        assertEquals("ARGUMENT_PROPAGATION", mutationOperator.getId());
        assertEquals("org.pitest.mutationtest.engine.gregor.mutators.ArgumentPropagationMutator",
                     mutationOperator.getClassName());
        assertNotNull(mutationOperator.getViolationDescription());
    }

    @Test
    public void testFind_knownMutator_byClassName() throws Exception {

        final MutationOperator mutationOperator = MutationOperators
                .find("org.pitest.mutationtest.engine.gregor.mutators.ArgumentPropagationMutator");
        assertNotNull(mutationOperator);
        assertEquals("ARGUMENT_PROPAGATION", mutationOperator.getId());
        assertEquals("org.pitest.mutationtest.engine.gregor.mutators.ArgumentPropagationMutator",
                     mutationOperator.getClassName());
        assertNotNull(mutationOperator.getViolationDescription());
    }

    @Test
    public void testFind_knownMutator_byClassNameWithSuffix() throws Exception {

        final MutationOperator mutationOperator = MutationOperators
                .find("org.pitest.mutationtest.engine.gregor.mutators.ArgumentPropagationMutator_WITH_SUFFIX");
        assertNotNull(mutationOperator);
        assertEquals("ARGUMENT_PROPAGATION", mutationOperator.getId());
        assertEquals("org.pitest.mutationtest.engine.gregor.mutators.ArgumentPropagationMutator",
                     mutationOperator.getClassName());
        assertNotNull(mutationOperator.getViolationDescription());
    }

    @Test
    public void testAllMutators() throws Exception {

        // act
        final Collection<MutationOperator> mutationOperators = MutationOperators.allMutagens();

        // assert
        assertNotNull(mutationOperators);
        assertFalse(mutationOperators.isEmpty());
        assertEquals(23, mutationOperators.size());

    }
}
