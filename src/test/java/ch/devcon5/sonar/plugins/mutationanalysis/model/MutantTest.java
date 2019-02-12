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

import org.junit.Test;

public class MutantTest {

  public static Mutant newUndetectedMutant() {

    return Mutant.builder()
                 .mutantStatus(Mutant.State.NO_COVERAGE)
                 .inSourceFile("SomeClass.java")
                 .inClass("com.foo.bar.SomeClass")
                 .inMethod("anyMethod")
                 .withMethodParameters("anyMethodDesc")
                 .inLine(8)
                 .usingMutator(MutationOperators.find("INVERT_NEGS"))
                 .atIndex(10)
                 .numberOfTestsRun(123)
                 .killedBy("com.foo.bar.SomeClassKillingTest")
                 .build();
  }

  public static Mutant newDetectedMutant() {

    return Mutant.builder()
                 .mutantStatus(Mutant.State.KILLED)
                 .inSourceFile("SomeClass.java")
                 .inClass("com.foo.bar.SomeClass")
                 .inMethod("anyMethod")
                 .withMethodParameters("anyMethodDesc")
                 .inLine(17)
                 .usingMutator(MutationOperators.find("INVERT_NEGS"))
                 .atIndex(5)
                 .numberOfTestsRun(123)
                 .killedBy("com.foo.bar.SomeClassKillingTest")
                 .build();
  }

  public static Mutant newSurvivedMutantWithSuffix(){
    return Mutant.builder()
                 .mutantStatus(Mutant.State.SURVIVED)
                 .inSourceFile("SomeClass.java")
                 .inClass("com.foo.bar.SomeClass")
                 .inMethod("anyMethod")
                 .withMethodParameters("anyMethodDesc")
                 .inLine(8)
                 .usingMutator("org.pitest.mutationtest.engine.gregor.mutators.RemoveConditionalMutator_EQUAL_ELSE")
                 .atIndex(10)
                 .killedBy("com.foo.bar.SomeClassKillingTest")
                 .numberOfTestsRun(123)
                 .withDescription("removed conditional - replaced equality check with false")
                 .build();
  }


  @Test
  public void testIsDetected_true() throws Exception {

    assertTrue(newDetectedMutant().isDetected());
  }

  @Test
  public void testIsDetected_false() throws Exception {

    assertFalse(newUndetectedMutant().isDetected());
  }

  @Test
  public void testGetMutantStatus_killed() throws Exception {

    assertEquals(Mutant.State.KILLED, newDetectedMutant().getState());
  }

  @Test
  public void testGetMutantStatus_noCoverage() throws Exception {

    assertEquals(Mutant.State.NO_COVERAGE, newUndetectedMutant().getState());
  }

  @Test
  public void testGetSourceFile() throws Exception {

    assertEquals("SomeClass.java", newDetectedMutant().getSourceFile());
  }


  @Test
  public void testGetState_killed() throws Exception {

    assertEquals(Mutant.State.KILLED, newDetectedMutant().getState());
  }

  @Test
  public void testGetMutatedClass() throws Exception {

    assertEquals("com.foo.bar.SomeClass", newDetectedMutant().getMutatedClass());
  }

  @Test
  public void testGetMutatedMethod() throws Exception {

    assertEquals("anyMethod", newDetectedMutant().getMutatedMethod());
  }

  @Test
  public void testGetMethodDescription() throws Exception {

    assertEquals("anyMethodDesc", newDetectedMutant().getMethodDescription());
  }

  @Test
  public void testGetDescription_defaultEmpty() throws Exception {

    assertFalse(newDetectedMutant().getDescription().isPresent());
  }

  @Test
  public void testGetDescription() throws Exception {

    assertEquals("removed conditional - replaced equality check with false", newSurvivedMutantWithSuffix().getDescription().get());
  }

  @Test
  public void testGetLineNumber() throws Exception {

    assertEquals(17, newDetectedMutant().getLineNumber());
    assertEquals(8, newUndetectedMutant().getLineNumber());
  }

  @Test
  public void testGetNumberOfTestsRun() throws Exception {

    assertEquals(123, newDetectedMutant().getNumberOfTestsRun());
  }


  @Test
  public void testGetMutator() throws Exception {

    final MutationOperator mutationOperator = MutationOperators.find("org.pitest.mutationtest.engine.gregor.mutators.InvertNegsMutator");
    assertNotNull(mutationOperator);
    assertEquals(mutationOperator, newDetectedMutant().getMutationOperator());
  }

  @Test
  public void testGetMutatorSuffix_emptySuffix() throws Exception {

    assertEquals("", newDetectedMutant().getMutatorSuffix());

  }

  @Test
  public void testGetMutatorSuffix_nonEmptySuffix() throws Exception {

    Mutant mutant = newSurvivedMutantWithSuffix();

    assertEquals("EQUAL_ELSE", mutant.getMutatorSuffix());

  }

  @Test
  public void testGetIndex() throws Exception {

    assertEquals(5, newDetectedMutant().getIndex());
    assertEquals(10, newUndetectedMutant().getIndex());
  }

  @Test
  public void testGetKillingTest() throws Exception {

    assertEquals("com.foo.bar.SomeClassKillingTest", newDetectedMutant().getKillingTest());
  }

  @Test
  public void testToString() throws Exception {

    assertEquals("Mutant [sourceFile=SomeClass.java, "
                     + "mutatedClass=com.foo.bar.SomeClass, "
                     + "mutatedMethod=anyMethod, "
                     + "methodDescription=anyMethodDesc, "
                     + "lineNumber=17, "
                     + "state=KILLED, "
                     + "mutationOperator=Invert Negs Mutator, "
                     + "numberOfTestsRun=123, "
                     + "killingTest=com.foo.bar.SomeClassKillingTest]",
                 newDetectedMutant().toString());

  }
  @Test
  public void testToString_withDescription() throws Exception {
    assertEquals("Mutant [sourceFile=SomeClass.java, "
                     + "mutatedClass=com.foo.bar.SomeClass, "
                     + "mutatedMethod=anyMethod, "
                     + "methodDescription=anyMethodDesc, "
                     + "lineNumber=8, "
                     + "state=SURVIVED, "
                     + "mutationOperator=Remove Conditional Mutator, "
                     + "numberOfTestsRun=123, "
                     + "killingTest=, "
                     + "description=removed conditional - replaced equality check with false]",
                 newSurvivedMutantWithSuffix().toString());

  }

  @Test
  public void testEquals_same_true() throws Exception {

    Mutant mutant = newDetectedMutant();
    assertEquals(mutant, mutant);
  }

  @Test
  public void testEquals_clone_true() throws Exception {

    final Mutant expected = newDetectedMutant();
    final Mutant twin = newDetectedMutant();
    assertEquals(expected, twin);
  }

  @Test
  public void testEquals_differentDetected_false() throws Exception {

    final Mutant expected = newDetectedMutant();
    final Mutant other = newUndetectedMutant();

    assertNotEquals(expected, other);
  }

  @Test
  public void testEquals_differentStatus_false() throws Exception {

    final Mutant expected = newDetectedMutant();
    final Mutant other = Mutant.builder()
                               .mutantStatus(Mutant.State.MEMORY_ERROR)
                               .inSourceFile(expected.getSourceFile())
                               .inClass(expected.getMutatedClass())
                               .inMethod(expected.getMutatedMethod())
                               .inLine(expected.getLineNumber())
                               .withMethodParameters(expected.getMethodDescription())
                               .usingMutator(expected.getMutationOperator())
                               .atIndex(expected.getIndex())
                               .numberOfTestsRun(expected.getNumberOfTestsRun())
                               .killedBy(expected.getKillingTest())
                               .build();

    assertNotEquals(expected, other);
  }

  @Test
  public void testEquals_differentSourceFile_false() throws Exception {

    final Mutant expected = newDetectedMutant();
    final Mutant other = Mutant.builder()
                               .mutantStatus(expected.getState())
                               .inSourceFile("other")
                               .inClass(expected.getMutatedClass())
                               .inMethod(expected.getMutatedMethod())
                               .inLine(expected.getLineNumber())
                               .withMethodParameters(expected.getMethodDescription())
                               .usingMutator(expected.getMutationOperator())
                               .atIndex(expected.getIndex())
                               .numberOfTestsRun(expected.getNumberOfTestsRun())
                               .killedBy(expected.getKillingTest())
                               .build();
    assertNotEquals(expected, other);
  }

  @Test
  public void testEquals_differentMutatedClass_false() throws Exception {

    final Mutant expected = newDetectedMutant();
    final Mutant other = Mutant.builder()
                               .mutantStatus(expected.getState())
                               .inSourceFile(expected.getSourceFile())
                               .inClass("com.otherClass")
                               .inMethod(expected.getMutatedMethod())
                               .inLine(expected.getLineNumber())
                               .withMethodParameters(expected.getMethodDescription())
                               .usingMutator(expected.getMutationOperator())
                               .atIndex(expected.getIndex())
                               .numberOfTestsRun(expected.getNumberOfTestsRun())
                               .killedBy(expected.getKillingTest())
                               .build();

    assertNotEquals(expected, other);
  }

  @Test
  public void testEquals_differentMutatedMethod_false() throws Exception {

    final Mutant expected = newDetectedMutant();
    final Mutant other = Mutant.builder()
                               .mutantStatus(expected.getState())
                               .inSourceFile(expected.getSourceFile())
                               .inClass(expected.getMutatedClass())
                               .inMethod("otherMethod")
                               .inLine(expected.getLineNumber())
                               .withMethodParameters(expected.getMethodDescription())
                               .usingMutator(expected.getMutationOperator())
                               .atIndex(expected.getIndex())
                               .numberOfTestsRun(expected.getNumberOfTestsRun())
                               .killedBy(expected.getKillingTest())
                               .build();
    assertNotEquals(expected, other);
  }

  @Test
  public void testEquals_differentMethodDescription_false() throws Exception {

    final Mutant expected = newDetectedMutant();
    final Mutant other = Mutant.builder()
                               .mutantStatus(expected.getState())
                               .inSourceFile(expected.getSourceFile())
                               .inClass(expected.getMutatedClass())
                               .inMethod(expected.getMutatedMethod())
                               .inLine(expected.getLineNumber())
                               .withMethodParameters("()")
                               .usingMutator(expected.getMutationOperator())
                               .atIndex(expected.getIndex())
                               .numberOfTestsRun(expected.getNumberOfTestsRun())
                               .killedBy(expected.getKillingTest())
                               .build();

    assertNotEquals(expected, other);
  }

  @Test
  public void testEquals_differentLineNumber_false() throws Exception {

    final Mutant expected = newDetectedMutant();
    final Mutant other = Mutant.builder()
                               .mutantStatus(expected.getState())
                               .inSourceFile(expected.getSourceFile())
                               .inClass(expected.getMutatedClass())
                               .inMethod(expected.getMutatedMethod())
                               .inLine(127)
                               .withMethodParameters(expected.getMethodDescription())
                               .usingMutator(expected.getMutationOperator())
                               .atIndex(expected.getIndex())
                               .numberOfTestsRun(expected.getNumberOfTestsRun())
                               .killedBy(expected.getKillingTest())
                               .build();

    assertNotEquals(expected, other);
  }

  @Test
  public void testEquals_differentMutator_false() throws Exception {

    final Mutant expected = newDetectedMutant();
    final Mutant other = Mutant.builder()
                               .mutantStatus(expected.getState())
                               .inSourceFile(expected.getSourceFile())
                               .inClass(expected.getMutatedClass())
                               .inMethod(expected.getMutatedMethod())
                               .inLine(expected.getLineNumber())
                               .withMethodParameters(expected.getMethodDescription())
                               .usingMutator(MutationOperators.find("ARGUMENT_PROPAGATION"))
                               .atIndex(expected.getIndex())
                               .numberOfTestsRun(expected.getNumberOfTestsRun())
                               .killedBy(expected.getKillingTest())
                               .build();

    assertNotEquals(expected, other);
  }

  @Test
  public void testEquals_differentSuffix_false() throws Exception {

    final Mutant expected = newSurvivedMutantWithSuffix();
    final Mutant other = Mutant.builder()
                               .mutantStatus(expected.getState())
                               .inSourceFile(expected.getSourceFile())
                               .inClass(expected.getMutatedClass())
                               .inMethod(expected.getMutatedMethod())
                               .inLine(expected.getLineNumber())
                               .withMethodParameters(expected.getMethodDescription())
                               .usingMutator("org.pitest.mutationtest.engine.gregor.mutators.RemoveConditionalMutator_EQUAL_IF")
                               .withDescription(expected.getDescription().get())
                               .atIndex(expected.getIndex())
                               .numberOfTestsRun(expected.getNumberOfTestsRun())
                               .killedBy(expected.getKillingTest())
                               .build();

    assertNotEquals(expected, other);
  }

  @Test
  public void testEquals_differentNumberOfTestsRun_false() throws Exception {

    final Mutant expected = newSurvivedMutantWithSuffix();
    final Mutant other = Mutant.builder()
                               .mutantStatus(expected.getState())
                               .inSourceFile(expected.getSourceFile())
                               .inClass(expected.getMutatedClass())
                               .inMethod(expected.getMutatedMethod())
                               .inLine(expected.getLineNumber())
                               .withMethodParameters(expected.getMethodDescription())
                               .usingMutator(expected.getMutationOperator())
                               .withDescription(expected.getDescription().get())
                               .atIndex(expected.getIndex())
                               .numberOfTestsRun(256)
                               .killedBy(expected.getKillingTest())
                               .build();

    assertNotEquals(expected, other);
  }

  @Test
  public void testEquals_differentIndex_false() throws Exception {

    final Mutant expected = newDetectedMutant();
    final Mutant other = Mutant.builder()
                               .mutantStatus(expected.getState())
                               .inSourceFile(expected.getSourceFile())
                               .inClass(expected.getMutatedClass())
                               .inMethod(expected.getMutatedMethod())
                               .inLine(expected.getLineNumber())
                               .withMethodParameters(expected.getMethodDescription())
                               .usingMutator(expected.getMutationOperator())
                               .atIndex(127)
                               .numberOfTestsRun(expected.getNumberOfTestsRun())
                               .killedBy(expected.getKillingTest())
                               .build();

    assertNotEquals(expected, other);
  }

  @Test
  public void testEquals_differentKillingTest_false() throws Exception {

    final Mutant expected = newDetectedMutant();
    final Mutant twin = Mutant.builder()
                              .mutantStatus(expected.getState())
                              .inSourceFile(expected.getSourceFile())
                              .inClass(expected.getMutatedClass())
                              .inMethod(expected.getMutatedMethod())
                              .inLine(expected.getLineNumber())
                              .withMethodParameters(expected.getMethodDescription())
                              .usingMutator(expected.getMutationOperator())
                              .atIndex(expected.getIndex())
                              .numberOfTestsRun(expected.getNumberOfTestsRun())
                              .killedBy("otherTest")
                              .build();

    assertNotEquals(expected, twin);
  }

  @Test
  public void testEquals_differentDescription_false() throws Exception {

    final Mutant expected = newDetectedMutant();
    final Mutant twin = Mutant.builder()
                              .mutantStatus(expected.getState())
                              .inSourceFile(expected.getSourceFile())
                              .inClass(expected.getMutatedClass())
                              .inMethod(expected.getMutatedMethod())
                              .inLine(expected.getLineNumber())
                              .withMethodParameters(expected.getMethodDescription())
                              .usingMutator(expected.getMutationOperator())
                              .atIndex(expected.getIndex())
                              .numberOfTestsRun(expected.getNumberOfTestsRun())
                              .killedBy(expected.getKillingTest())
                              .withDescription("other Description")
                              .build();

    assertNotEquals(expected, twin);
  }

  @Test
  public void testEquals_null_false() throws Exception {

    assertNotEquals(newDetectedMutant(), null);
  }

  @Test
  public void testEquals_otherObject_false() throws Exception {

    assertNotEquals(newDetectedMutant(), new Object());
  }

  @Test
  public void testHashCode_detected_reproducible() throws Exception {

    // for the same object we always have the same hashCode
    Mutant mutant = newDetectedMutant();
    final int prime = 31;
    int refCode = 1;
    refCode = prime * refCode + mutant.getIndex();
    refCode = prime * refCode + 1231;
    refCode = prime * refCode + mutant.getLineNumber();
    refCode = prime * refCode + mutant.getMethodDescription().hashCode();
    refCode = prime * refCode + mutant.getState().hashCode();
    refCode = prime * refCode + mutant.getMutatedClass().hashCode();
    refCode = prime * refCode + mutant.getMutatedMethod().hashCode();
    refCode = prime * refCode + mutant.getMutationOperator().hashCode();
    refCode = prime * refCode + mutant.getMutatorSuffix().hashCode();
    refCode = prime * refCode + mutant.getSourceFile().hashCode();
    refCode = prime * refCode + mutant.getKillingTest().hashCode();
    refCode = prime * refCode + mutant.getNumberOfTestsRun();
    refCode = prime * refCode + mutant.getDescription().hashCode();

    assertEquals(refCode, mutant.hashCode());
  }

  @Test
  public void testHashCode_undetected_reproducible() throws Exception {

    // for the same object we always have the same hashCode
    Mutant mutant = newSurvivedMutantWithSuffix();
    final int prime = 31;
    int refCode = 1;
    refCode = prime * refCode + mutant.getIndex();
    refCode = prime * refCode + 1237;
    refCode = prime * refCode + mutant.getLineNumber();
    refCode = prime * refCode + mutant.getMethodDescription().hashCode();
    refCode = prime * refCode + mutant.getState().hashCode();
    refCode = prime * refCode + mutant.getMutatedClass().hashCode();
    refCode = prime * refCode + mutant.getMutatedMethod().hashCode();
    refCode = prime * refCode + mutant.getMutationOperator().hashCode();
    refCode = prime * refCode + mutant.getMutatorSuffix().hashCode();
    refCode = prime * refCode + mutant.getSourceFile().hashCode();
    refCode = prime * refCode + mutant.getKillingTest().hashCode();
    refCode = prime * refCode + mutant.getNumberOfTestsRun();
    refCode = prime * refCode + mutant.getDescription().hashCode();

    assertEquals(refCode, mutant.hashCode());
  }

  @Test
  public void testHashCode_sameMutant() throws Exception {

    Mutant mutant = newDetectedMutant();
    assertEquals(mutant.hashCode(), mutant.hashCode());
  }

  @Test
  public void testHashCode_equalMutant_same() throws Exception {

    assertEquals(newDetectedMutant().hashCode(), newDetectedMutant().hashCode());
  }

  @Test
  public void testGetTestDescriptor_detected_nonEmptySpec() throws Exception {

    Mutant mutant = newDetectedMutant();
    TestDescriptor td = mutant.getTestDescriptor();

    assertEquals("com.foo.bar.SomeClassKillingTest", td.getSpec());
  }

  @Test
  public void testGetTestDescriptor_undetected_emptySpec() throws Exception {

    Mutant mutant = newUndetectedMutant();
    TestDescriptor td = mutant.getTestDescriptor();

    assertEquals("", td.getSpec());
  }

  @Test
  public void testHashCode_otherMutantObject_different() throws Exception {

    assertNotEquals(newDetectedMutant().hashCode(), newUndetectedMutant().hashCode());
  }

  @Test
  public void testBuilder() throws Exception {

    final Mutant.Builder builder = Mutant.builder();
    assertNotNull(builder);
  }

}
