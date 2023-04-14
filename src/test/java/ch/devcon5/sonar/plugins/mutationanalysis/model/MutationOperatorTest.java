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

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

class MutationOperatorTest {

  @Test()
  void testMutator_nullId_exception() throws Exception {
    Set<String> empty = emptySet();
    URL url = new URL("file:///");
    assertThrows(NullPointerException.class, () ->
        new MutationOperator(null, "", empty, "", url));
  }

  @Test()
  void testMutator_nullName_exception() throws Exception {
    Set<String> empty = emptySet();
    URL url = new URL("file:///");
    assertThrows(NullPointerException.class, () ->
        new MutationOperator("", null, empty, "", url));
  }

  @Test()
  void testMutator_nullClassNames_exception() throws Exception {
    URL url = new URL("file:///");
    assertThrows(NullPointerException.class, () ->
        new MutationOperator("", "", null, "", url));
  }

  @Test()
  void testMutator_nullViolationDescription_exception() throws Exception {
    Set<String> empty = emptySet();
    URL url = new URL("file:///");
    assertThrows(NullPointerException.class, () ->
        new MutationOperator("", "", empty, null, url));
  }

  @Test
  void testMutator_nullArguments() {
    final MutationOperator mutationOperator = new MutationOperator("id", "name", emptySet(), "violationDescription",
        null);
    assertEquals("id", mutationOperator.getId());
    assertEquals("name", mutationOperator.getName());
    assertEquals("violationDescription", mutationOperator.getViolationDescription());
    assertEquals(emptySet(), mutationOperator.getClassNames());
    assertNotNull(mutationOperator.getMutagenDescriptionLocation());
    assertFalse(mutationOperator.getMutagenDescriptionLocation().isPresent());
    assertNotNull(mutationOperator.getMutagenDescriptionLocation());
    assertEquals(Optional.empty(), mutationOperator.getMutagenDescriptionLocation());
    assertNotNull(mutationOperator.getMutagenDescriptionLocation());

  }

  @Test
  void testGetId() {
    final MutationOperator mutationOperator = MutationOperators.find("ARGUMENT_PROPAGATION");
    assertEquals("ARGUMENT_PROPAGATION", mutationOperator.getId());
  }

  @Test
  void testGetMutatorDescriptionLocation() {
    final MutationOperator mutationOperator = MutationOperators.find("ARGUMENT_PROPAGATION");
    final Optional<URL> descriptorLocation = mutationOperator.getMutagenDescriptionLocation();
    assertNotNull(descriptorLocation);
    assertTrue(descriptorLocation.isPresent());
  }

  @Test
  void testGetMutatorDescription() {
    final MutationOperator mutationOperator = MutationOperators.find("ARGUMENT_PROPAGATION");
    final String desc = mutationOperator.getMutagenDescription();
    assertNotNull(desc);
  }

  @Test
  void testGetMutatorDescription_exceptionOccurred_noDescription() throws Exception {
    final MutationOperator mutationOperator = new MutationOperator("test", "aName", singleton("aClass"),
        "aViolationDescription", new URL("file://localhost:1"));
    final String desc = mutationOperator.getMutagenDescription();
    assertEquals("No description", desc);
  }

  @Test
  void testGetViolationDescription() {
    final MutationOperator mutationOperator = MutationOperators.find("ARGUMENT_PROPAGATION");
    final String violationDesc = mutationOperator.getViolationDescription();
    assertNotNull(violationDesc);
  }

  @Test
  void testGetName() {
    final MutationOperator mutationOperator = MutationOperators.find("ARGUMENT_PROPAGATION");
    final String name = mutationOperator.getName();
    assertEquals("Argument Propagation Mutator", name);
  }

  @Test
  void testGetClassNames() {
    final MutationOperator mutationOperator = MutationOperators.find("ARGUMENT_PROPAGATION");
    final Set<String> classNames = mutationOperator.getClassNames();
    assertEquals(
        new HashSet<>(
            Arrays.asList(
                "org.pitest.mutationtest.engine.gregor.mutators.ArgumentPropagationMutator",
                "org.pitest.mutationtest.engine.gregor.mutators.experimental.ArgumentPropagationMutator"
            )
        ),
        classNames);
  }

  @Test
  void testGetClassNames_fromCustomOperator() throws Exception {
    final MutationOperator mutationOperator = new MutationOperator("test", "aName", singleton("aClass"),
        "aViolationDescription", new URL("file://localhost:1"));
    final Set<String> classNames = mutationOperator.getClassNames();
    assertEquals(singleton("aClass"), classNames);
  }

  @Test
  void testEquals_null_false() {
    final MutationOperator argumentPropagation = MutationOperators.find("ARGUMENT_PROPAGATION");
    assertNotEquals(null, argumentPropagation);
  }

  @Test
  void testEquals_different_false() {
    final MutationOperator argumentPropagation = MutationOperators.find("ARGUMENT_PROPAGATION");
    final MutationOperator conditionalsBoundary = MutationOperators.find("CONDITIONALS_BOUNDARY");
    assertNotEquals(argumentPropagation, conditionalsBoundary);
  }

  @Test
  void testEquals_differentClass_false() {
    final MutationOperator argumentPropagation = MutationOperators.find("ARGUMENT_PROPAGATION");
    assertNotEquals(new Object(), argumentPropagation);
  }

  @Test
  void testEquals_same_true() {
    final MutationOperator argumentPropagation = MutationOperators.find("ARGUMENT_PROPAGATION");
    assertEquals(argumentPropagation, argumentPropagation);
  }

  @Test
  void testEquals_equalsId_true() throws Exception {
    final MutationOperator argumentPropagation = MutationOperators.find("ARGUMENT_PROPAGATION");
    final MutationOperator other = new MutationOperator("ARGUMENT_PROPAGATION", "someName", singleton("someClass"),
        "someDescription", new URL("file:///"));
    assertEquals(argumentPropagation, other);
  }

  @Test
  void testHashCode_reproducible() {
    final MutationOperator mutationOperator = MutationOperators.find("ARGUMENT_PROPAGATION");
    final int expectedHashCode = 31 + mutationOperator.getId().hashCode();
    assertEquals(expectedHashCode, mutationOperator.hashCode());
  }

  @Test
  void testHashCode_sameMutator() {
    final MutationOperator argumentPropagation = MutationOperators.find("ARGUMENT_PROPAGATION");
    assertEquals(argumentPropagation.hashCode(), argumentPropagation.hashCode());
  }

  @Test
  void testHashCode_otherMutatorObject() {
    final MutationOperator argumentPropagation = MutationOperators.find("ARGUMENT_PROPAGATION");
    final MutationOperator conditionalsBoundary = MutationOperators.find("CONDITIONALS_BOUNDARY");
    assertNotEquals(argumentPropagation.hashCode(), conditionalsBoundary.hashCode());
  }

  @Test
  void testGetMutagenDescription() throws Exception {
    String expected = IOUtils.toString(MutationOperator.class.getResourceAsStream("/ch/devcon5/sonar/plugins"
        + "/mutationanalysis/model/ARGUMENT_PROPAGATION.html"), StandardCharsets.UTF_8);
    final MutationOperator argumentPropagation = MutationOperators.find("ARGUMENT_PROPAGATION");
    String actual = argumentPropagation.getMutagenDescription();
    assertEquals(expected, actual);
  }

}
