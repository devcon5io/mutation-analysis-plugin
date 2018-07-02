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

import static org.junit.Assert.*;

import java.net.URL;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class MutationOperatorTest {

    @Test(expected = NullPointerException.class)
    public void testMutator_nullId_exception() throws Exception {

        new MutationOperator(null, "", "", "", new URL("file:///"));

    }

    @Test(expected = NullPointerException.class)
    public void testMutator_nullName_exception() throws Exception {

        new MutationOperator("", null, "", "", new URL("file:///"));

    }

    @Test(expected = NullPointerException.class)
    public void testMutator_nullViolationDescription_exception() throws Exception {

        new MutationOperator("", "", "", null, new URL("file:///"));

    }

    @Test
    public void testMutator_nullArgument() throws Exception {

        final MutationOperator mutationOperator = new MutationOperator("id", "name", null, "violationDescription", null);
        assertEquals("id", mutationOperator.getId());
        assertEquals("name", mutationOperator.getName());
        assertEquals("violationDescription", mutationOperator.getViolationDescription());
        assertNull(mutationOperator.getClassName());
        assertNotNull(mutationOperator.getMutagenDescriptionLocation());
        assertFalse(mutationOperator.getMutagenDescriptionLocation().isPresent());
        assertNotNull(mutationOperator.getMutagenDescriptionLocation());
        assertEquals(Optional.empty(), mutationOperator.getMutagenDescriptionLocation());
        assertNotNull(mutationOperator.getMutagenDescriptionLocation());

    }

    @Test
    public void testGetId() throws Exception {

        final MutationOperator mutationOperator = MutationOperators.find("ARGUMENT_PROPAGATION");
        assertEquals("ARGUMENT_PROPAGATION", mutationOperator.getId());
    }



    @Test
    public void testGetMutatorDescriptionLocation() throws Exception {

        final MutationOperator mutationOperator = MutationOperators.find("ARGUMENT_PROPAGATION");
        final Optional<URL> descriptorLocation = mutationOperator.getMutagenDescriptionLocation();
        assertNotNull(descriptorLocation);
        assertTrue(descriptorLocation.isPresent());
    }

    @Test
    public void testGetMutatorDescription() throws Exception {

        final MutationOperator mutationOperator = MutationOperators.find("ARGUMENT_PROPAGATION");
        final String desc = mutationOperator.getMutagenDescription();
        assertNotNull(desc);
    }

    @Test
    public void testGetViolationDescription() throws Exception {

        final MutationOperator mutationOperator = MutationOperators.find("ARGUMENT_PROPAGATION");
        final String violationDesc = mutationOperator.getViolationDescription();
        assertNotNull(violationDesc);
    }

    @Test
    public void testGetName() throws Exception {

        final MutationOperator mutationOperator = MutationOperators.find("ARGUMENT_PROPAGATION");
        final String name = mutationOperator.getName();
        assertEquals("Argument Propagation Mutator", name);
    }

    @Test
    public void testGetClassName() throws Exception {

        final MutationOperator mutationOperator = MutationOperators.find("ARGUMENT_PROPAGATION");
        final String className = mutationOperator.getClassName();
        assertEquals("org.pitest.mutationtest.engine.gregor.mutators.ArgumentPropagationMutator", className);
    }

    @Test
    public void testEquals_null_false() throws Exception {

        final MutationOperator argumentPropagation = MutationOperators.find("ARGUMENT_PROPAGATION");
        assertNotEquals(argumentPropagation, null);
    }

    @Test
    public void testEquals_different_false() throws Exception {

        final MutationOperator argumentPropagation = MutationOperators.find("ARGUMENT_PROPAGATION");
        final MutationOperator conditionalsBoundary = MutationOperators.find("CONDITIONALS_BOUNDARY");
        assertNotEquals(argumentPropagation, conditionalsBoundary);
    }

    @Test
    public void testEquals_differentClass_false() throws Exception {

        final MutationOperator argumentPropagation = MutationOperators.find("ARGUMENT_PROPAGATION");
        assertNotEquals(argumentPropagation, new Object());
    }

    @Test
    public void testEquals_same_true() throws Exception {

        final MutationOperator argumentPropagation = MutationOperators.find("ARGUMENT_PROPAGATION");
        assertEquals(argumentPropagation, argumentPropagation);
    }

    @Test
    public void testEquals_equalsId_true() throws Exception {

        final MutationOperator argumentPropagation = MutationOperators.find("ARGUMENT_PROPAGATION");
        final MutationOperator other = new MutationOperator("ARGUMENT_PROPAGATION", "someName", "someClass", "someDescription", new URL(
                "file:///"));
        assertEquals(argumentPropagation, other);
    }

    @Test
    public void testHashCode_reproducible() throws Exception {

        final MutationOperator mutationOperator = MutationOperators.find("ARGUMENT_PROPAGATION");
        final int expectedHashCode = 31 + mutationOperator.getId().hashCode();

        assertEquals(expectedHashCode, mutationOperator.hashCode());

    }

    @Test
    public void testHashCode_sameMutator() throws Exception {

        final MutationOperator argumentPropagation = MutationOperators.find("ARGUMENT_PROPAGATION");
        assertEquals(argumentPropagation.hashCode(), argumentPropagation.hashCode());
    }

    @Test
    public void testHashCode_otherMutatorObject() throws Exception {

        final MutationOperator argumentPropagation = MutationOperators.find("ARGUMENT_PROPAGATION");
        final MutationOperator conditionalsBoundary = MutationOperators.find("CONDITIONALS_BOUNDARY");

        assertNotEquals(argumentPropagation.hashCode(), conditionalsBoundary.hashCode());
    }

    @Test
    public void testGetMutagenDescription() throws Exception {
        String expected = IOUtils.toString(MutationOperator.class.getResourceAsStream("/ch/devcon5/sonar/plugins/mutationanalysis/model/ARGUMENT_PROPAGATION.html"));
        final MutationOperator argumentPropagation = MutationOperators.find("ARGUMENT_PROPAGATION");
        String actual = argumentPropagation.getMutagenDescription();
        assertEquals(expected, actual);
    }

}
