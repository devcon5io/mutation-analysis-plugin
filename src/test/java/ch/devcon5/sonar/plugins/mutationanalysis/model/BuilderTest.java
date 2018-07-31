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
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class BuilderTest {

  private Mutant.Builder subject;

  @Before
  public void setUp() throws Exception {

    subject = new Mutant.Builder();
  }

  @Test
  public void testBuild_withMinimalArguments() throws Exception {

    //@formatter:off
        final Mutant mutant = subject
                               .mutantStatus(Mutant.State.KILLED)
                               .inSourceFile("someSource.java")
                               .inClass("some.package.SomeClass")
                               .inMethod("aMethod")
                               .withMethodParameters("methodDescription")
                               .usingMutator(MutationOperators.find("ARGUMENT_PROPAGATION"))
                               .killedBy("aTest")
                               .build();
        // @formatter:on
    assertNotNull(mutant);
    assertEquals(Mutant.State.KILLED, mutant.getState());
    assertEquals("someSource.java", mutant.getSourceFile());
    assertEquals("some.package.SomeClass", mutant.getMutatedClass());
    assertEquals("aMethod", mutant.getMutatedMethod());
    assertEquals("methodDescription", mutant.getMethodDescription());
    // implicit default values
    assertEquals(0, mutant.getLineNumber());
    assertEquals(0, mutant.getIndex());
    assertTrue(mutant.isDetected());
    assertEquals("aTest", mutant.getKillingTest());

    final MutationOperator mutationOperator = MutationOperators.find("ARGUMENT_PROPAGATION");
    assertNotNull(mutationOperator);
    assertEquals(mutationOperator, mutant.getMutationOperator());
    assertEquals("", mutant.getMutatorSuffix());

  }

  @Test
  public void testBuild_detectedWithMinimalArguments() throws Exception {

    //@formatter:off
        final Mutant mutant = subject
                                .mutantStatus(Mutant.State.KILLED)
                                .inSourceFile("someSource.java")
                                .inClass("some.package.SomeClass")
                                .inMethod("aMethod")
                                .withMethodParameters("methodDescription")
                                .usingMutator(MutationOperators.find("ARGUMENT_PROPAGATION"))
                                .killedBy("aTest")
                                .build();
        // @formatter:on
    assertNotNull(mutant);
    assertTrue(mutant.isDetected());
    assertEquals(Mutant.State.KILLED, mutant.getState());
    assertEquals("someSource.java", mutant.getSourceFile());
    assertEquals("some.package.SomeClass", mutant.getMutatedClass());
    assertEquals("aMethod", mutant.getMutatedMethod());
    assertEquals("methodDescription", mutant.getMethodDescription());
    // implicit default values
    assertEquals(0, mutant.getLineNumber());
    assertEquals(0, mutant.getIndex());
    assertEquals("aTest", mutant.getKillingTest());

    final MutationOperator mutationOperator = MutationOperators.find("ARGUMENT_PROPAGATION");
    assertNotNull(mutationOperator);
    assertEquals(mutationOperator, mutant.getMutationOperator());
    assertEquals("", mutant.getMutatorSuffix());

  }

  @Test
  public void testBuild_StatusAsString_WithMinimalArguments() throws Exception {

    final Mutant mutant = subject.mutantStatus("KILLED")
                                 .inSourceFile("someSource.java")
                                 .inClass("some.package.SomeClass")
                                 .inMethod("aMethod")
                                 .withMethodParameters("methodDescription")
                                 .usingMutator(MutationOperators.find("ARGUMENT_PROPAGATION"))
                                 .killedBy("aTest")
                                 .build();
    assertNotNull(mutant);
    assertTrue(mutant.isDetected());
    assertEquals(Mutant.State.KILLED, mutant.getState());
    assertEquals("someSource.java", mutant.getSourceFile());
    assertEquals("some.package.SomeClass", mutant.getMutatedClass());
    assertEquals("aMethod", mutant.getMutatedMethod());
    assertEquals("methodDescription", mutant.getMethodDescription());
    // implicit default values
    assertEquals(0, mutant.getLineNumber());
    assertEquals(0, mutant.getIndex());
    assertEquals("aTest", mutant.getKillingTest());

    final MutationOperator mutationOperator = MutationOperators.find("ARGUMENT_PROPAGATION");
    assertNotNull(mutationOperator);
    assertEquals(mutationOperator, mutant.getMutationOperator());
    assertEquals("", mutant.getMutatorSuffix());

  }

  @Test
  public void testBuild_invalidStatus_WithMinimalArguments() throws Exception {

    final Mutant mutant = subject.mutantStatus("invalid")
                                 .inSourceFile("someSource.java")
                                 .inClass("some.package.SomeClass")
                                 .inMethod("aMethod")
                                 .withMethodParameters("methodDescription")
                                 .usingMutator(MutationOperators.find("ARGUMENT_PROPAGATION"))
                                 .build();
    assertNotNull(mutant);
    assertFalse(mutant.isDetected());
    assertEquals(Mutant.State.UNKNOWN, mutant.getState());
    assertEquals("someSource.java", mutant.getSourceFile());
    assertEquals("some.package.SomeClass", mutant.getMutatedClass());
    assertEquals("aMethod", mutant.getMutatedMethod());
    assertEquals("methodDescription", mutant.getMethodDescription());
    // implicit default values
    assertEquals(0, mutant.getLineNumber());
    assertEquals(0, mutant.getIndex());
    assertEquals("", mutant.getKillingTest());

    final MutationOperator mutationOperator = MutationOperators.find("ARGUMENT_PROPAGATION");
    assertNotNull(mutationOperator);
    assertEquals(mutationOperator, mutant.getMutationOperator());
    assertEquals("", mutant.getMutatorSuffix());

  }

  @Test
  public void testBuild_withLineAndIndexAndMinimalArguments() throws Exception {

    final Mutant mutant = subject.mutantStatus(Mutant.State.KILLED)
                                 .inSourceFile("someSource.java")
                                 .inClass("some.package.SomeClass")
                                 .inMethod("aMethod")
                                 .inLine(123)
                                 .atIndex(456)
                                 .withMethodParameters("methodDescription")
                                 .usingMutator(MutationOperators.find("ARGUMENT_PROPAGATION"))
                                 .killedBy("aTest")
                                 .build();
    assertNotNull(mutant);
    assertEquals(Mutant.State.KILLED, mutant.getState());
    assertEquals("someSource.java", mutant.getSourceFile());
    assertEquals("some.package.SomeClass", mutant.getMutatedClass());
    assertEquals("aMethod", mutant.getMutatedMethod());
    assertEquals("methodDescription", mutant.getMethodDescription());
    // implicit default values
    assertEquals(123, mutant.getLineNumber());
    assertEquals(456, mutant.getIndex());
    assertTrue(mutant.isDetected());
    assertEquals("aTest", mutant.getKillingTest());

    final MutationOperator mutationOperator = MutationOperators.find("ARGUMENT_PROPAGATION");
    assertNotNull(mutationOperator);
    assertEquals(mutationOperator, mutant.getMutationOperator());
    assertEquals("", mutant.getMutatorSuffix());

  }

  @Test
  public void testBuild_withMinimalArguments_MutatorAsStringId() throws Exception {

    final Mutant mutant = subject.mutantStatus(Mutant.State.KILLED)
                                 .inSourceFile("someSource.java")
                                 .inClass("some.package.SomeClass")
                                 .inMethod("aMethod")
                                 .withMethodParameters("methodDescription")
                                 .usingMutator("ARGUMENT_PROPAGATION")
                                 .killedBy("aTest")
                                 .build();
    assertNotNull(mutant);
    assertEquals(Mutant.State.KILLED, mutant.getState());
    assertEquals("someSource.java", mutant.getSourceFile());
    assertEquals("some.package.SomeClass", mutant.getMutatedClass());
    assertEquals("aMethod", mutant.getMutatedMethod());
    assertEquals("methodDescription", mutant.getMethodDescription());
    // implicit default values
    assertEquals(0, mutant.getLineNumber());
    assertEquals(0, mutant.getIndex());
    assertTrue(mutant.isDetected());
    assertEquals("aTest", mutant.getKillingTest());

    final MutationOperator mutationOperator = MutationOperators.find("ARGUMENT_PROPAGATION");
    assertNotNull(mutationOperator);
    assertEquals(mutationOperator, mutant.getMutationOperator());
    assertEquals("", mutant.getMutatorSuffix());

  }

  @Test
  public void testBuild_MutatorAsStringWithSuffix_withMinimalArguments() throws Exception {

    final Mutant mutant = subject.mutantStatus(Mutant.State.KILLED)
                                 .inSourceFile("someSource.java")
                                 .inClass("some.package.SomeClass")
                                 .inMethod("aMethod")
                                 .withMethodParameters("methodDescription")
                                 .usingMutator("org.pitest.mutationtest.engine.gregor.mutators.RemoveConditionalMutator_A_SUFFIX")
                                 .killedBy("aTest")
                                 .build();
    assertNotNull(mutant);
    assertEquals(Mutant.State.KILLED, mutant.getState());
    assertEquals("someSource.java", mutant.getSourceFile());
    assertEquals("some.package.SomeClass", mutant.getMutatedClass());
    assertEquals("aMethod", mutant.getMutatedMethod());
    assertEquals("methodDescription", mutant.getMethodDescription());
    // implicit default values
    assertEquals(0, mutant.getLineNumber());
    assertEquals(0, mutant.getIndex());
    assertTrue(mutant.isDetected());
    assertEquals("aTest", mutant.getKillingTest());

    final MutationOperator mutationOperator = MutationOperators.find("org.pitest.mutationtest.engine.gregor.mutators.RemoveConditionalMutator");
    assertNotNull(mutationOperator);
    assertEquals(mutationOperator, mutant.getMutationOperator());
    assertEquals("A_SUFFIX", mutant.getMutatorSuffix());

  }

  @Test
  public void testBuild_withKillingTestAndMinimalArguments() throws Exception {

    final Mutant mutant = subject.mutantStatus(Mutant.State.KILLED)
                                 .inSourceFile("someSource.java")
                                 .inClass("some.package.SomeClass")
                                 .inMethod("aMethod")
                                 .withMethodParameters("methodDescription")
                                 .usingMutator(MutationOperators.find("ARGUMENT_PROPAGATION"))
                                 .killedBy("killingTest")
                                 .build();
    assertNotNull(mutant);
    assertEquals(Mutant.State.KILLED, mutant.getState());
    assertEquals("someSource.java", mutant.getSourceFile());
    assertEquals("some.package.SomeClass", mutant.getMutatedClass());
    assertEquals("aMethod", mutant.getMutatedMethod());
    assertEquals("methodDescription", mutant.getMethodDescription());
    assertEquals("killingTest", mutant.getKillingTest());
    // implicit default values
    assertEquals(0, mutant.getLineNumber());
    assertEquals(0, mutant.getIndex());
    assertTrue(mutant.isDetected());

    final MutationOperator mutationOperator = MutationOperators.find("ARGUMENT_PROPAGATION");
    assertNotNull(mutationOperator);
    assertEquals(mutationOperator, mutant.getMutationOperator());
    assertEquals("", mutant.getMutatorSuffix());

  }

  @Test(expected = NullPointerException.class)
  public void test_null_state_NullPointerException() throws Exception {

    Mutant.builder()
          .inSourceFile("aFile.java")
          .inClass("AClass")
          .inMethod("aMethod")
          .withMethodParameters("desc")
          .inLine(1)
          .atIndex(0)
          .usingMutator(MutationOperators.UNKNOWN)
          .killedBy("aTest")
          .build();

  }
  @Test(expected = NullPointerException.class)
  public void test_null_sourceFile_NullPointerException() throws Exception {
    Mutant.builder()
          .mutantStatus(Mutant.State.KILLED)
          .inClass("AClass")
          .inMethod("aMethod")
          .withMethodParameters("desc")
          .inLine(1)
          .atIndex(0)
          .usingMutator(MutationOperators.UNKNOWN)
          .killedBy("aTest")
          .build();
  }

  @Test(expected = NullPointerException.class)
  public void test_null_mutatedClass_NullPointerException() throws Exception {
    Mutant.builder()
          .mutantStatus(Mutant.State.KILLED)
          .inSourceFile("aFile.java")
          .inMethod("aMethod")
          .withMethodParameters("desc")
          .inLine(1)
          .atIndex(0)
          .usingMutator(MutationOperators.UNKNOWN)
          .killedBy("aTest")
          .build();
  }
  @Test(expected = NullPointerException.class)
  public void test_null_mutatedMethod_NullPointerException() throws Exception {
    Mutant.builder()
          .mutantStatus(Mutant.State.KILLED)
          .inSourceFile("aFile.java")
          .inClass("AClass")
          .withMethodParameters("desc")
          .inLine(1)
          .atIndex(0)
          .usingMutator(MutationOperators.UNKNOWN)
          .killedBy("aTest")
          .build();
  }
  @Test(expected = NullPointerException.class)
  public void test_null_mutatedMethodDesc_NullPointerException() throws Exception {
    Mutant.builder()
          .mutantStatus(Mutant.State.KILLED)
          .inSourceFile("aFile.java")
          .inClass("AClass")
          .inMethod("aMethod")
          .inLine(1)
          .atIndex(0)
          .usingMutator(MutationOperators.UNKNOWN)
          .killedBy("aTest")
          .build();
  }
  @Test(expected = NullPointerException.class)
  public void test_null_mutationOperator_NullPointerException() throws Exception {
    Mutant.builder()
          .mutantStatus(Mutant.State.KILLED)
          .inSourceFile("aFile.java")
          .inClass("AClass")
          .inMethod("aMethod")
          .withMethodParameters("desc")
          .inLine(1)
          .atIndex(0)
          .killedBy("aTest")
          .build();
  }

  @Test(expected = NullPointerException.class)
  public void test_null_killingTest_NullPointerException() throws Exception {
    Mutant.builder()
          .mutantStatus(Mutant.State.KILLED)
          .inSourceFile("aFile.java")
          .inClass("AClass")
          .inMethod("aMethod")
          .withMethodParameters("desc")
          .inLine(1)
          .atIndex(0)
          .usingMutator(MutationOperators.UNKNOWN)
          .build();
  }
}
