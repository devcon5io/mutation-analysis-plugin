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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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
        .numberOfTestsRun(256)
        .killedBy("com.foo.bar.SomeClassKillingTest")
        .build();
  }

  public static Mutant newSurvivedMutantWithSuffix() {
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
        .numberOfTestsRun(42)
        .withDescription("removed conditional - replaced equality check with false")
        .build();
  }

  @Test
  void testIsDetected_true() {
    assertTrue(newDetectedMutant().isDetected());
  }

  @Test
  void testIsDetected_false() {
    assertFalse(newUndetectedMutant().isDetected());
  }

  @Test
  void testGetMutantStatus_killed() {
    assertEquals(Mutant.State.KILLED, newDetectedMutant().getState());
  }

  @Test
  void testGetMutantStatus_noCoverage() {
    assertEquals(Mutant.State.NO_COVERAGE, newUndetectedMutant().getState());
  }

  @Test
  void testGetSourceFile() {
    assertEquals("SomeClass.java", newDetectedMutant().getSourceFile());
  }


  @Test
  void testGetState_killed() {
    assertEquals(Mutant.State.KILLED, newDetectedMutant().getState());
  }

  @Test
  void testGetMutatedClass() {
    assertEquals("com.foo.bar.SomeClass", newDetectedMutant().getMutatedClass());
  }

  @Test
  void testGetMutatedMethod() {
    assertEquals("anyMethod", newDetectedMutant().getMutatedMethod());
  }

  @Test
  void testGetMethodDescription() {
    assertEquals("anyMethodDesc", newDetectedMutant().getMethodDescription());
  }

  @Test
  void testGetDescription_defaultEmpty() {
    assertFalse(newDetectedMutant().getDescription().isPresent());
  }

  @Test
  void testGetDescription() {
    assertTrue(newSurvivedMutantWithSuffix().getDescription().isPresent());
    assertEquals("removed conditional - replaced equality check with false", newSurvivedMutantWithSuffix().getDescription().get());
  }

  @Test
  void testGetLineNumber() {
    assertEquals(17, newDetectedMutant().getLineNumber());
    assertEquals(8, newUndetectedMutant().getLineNumber());
  }

  @Test
  void testGetNumberOfTestsRun() {
    assertEquals(256, newDetectedMutant().getNumberOfTestsRun());
  }

  @Test
  void testGetMutator() {
    final MutationOperator mutationOperator = MutationOperators.find("org.pitest.mutationtest.engine.gregor.mutators.InvertNegsMutator");
    assertNotNull(mutationOperator);
    assertEquals(mutationOperator, newDetectedMutant().getMutationOperator());
  }

  @Test
  void testGetMutatorSuffix_emptySuffix() {
    assertEquals("", newDetectedMutant().getMutatorSuffix());
  }

  @Test
  void testGetMutatorSuffix_nonEmptySuffix() {
    Mutant mutant = newSurvivedMutantWithSuffix();
    assertEquals("EQUAL_ELSE", mutant.getMutatorSuffix());
  }

  @Test
  void testGetIndex() {
    assertEquals(5, newDetectedMutant().getIndex());
    assertEquals(10, newUndetectedMutant().getIndex());
  }

  @Test
  void testGetKillingTest() {
    assertEquals("com.foo.bar.SomeClassKillingTest", newDetectedMutant().getKillingTest());
  }

  @Test
  void testToString() {
    assertEquals("Mutant [sourceFile=SomeClass.java, "
            + "mutatedClass=com.foo.bar.SomeClass, "
            + "mutatedMethod=anyMethod, "
            + "methodDescription=anyMethodDesc, "
            + "lineNumber=17, "
            + "state=KILLED, "
            + "mutationOperator=Invert Negs Mutator, "
            + "numberOfTestsRun=256, "
            + "killingTest=com.foo.bar.SomeClassKillingTest]",
        newDetectedMutant().toString());
  }

  @Test
  void testToString_withDescription() {
    assertEquals("Mutant [sourceFile=SomeClass.java, "
            + "mutatedClass=com.foo.bar.SomeClass, "
            + "mutatedMethod=anyMethod, "
            + "methodDescription=anyMethodDesc, "
            + "lineNumber=8, "
            + "state=SURVIVED, "
            + "mutationOperator=Remove Conditional Mutator, "
            + "numberOfTestsRun=42, "
            + "killingTest=, "
            + "description=removed conditional - replaced equality check with false]",
        newSurvivedMutantWithSuffix().toString());
  }

  @Test
  void testEquals_same_true() {
    Mutant mutant = newDetectedMutant();
    assertEquals(mutant, mutant);
  }

  @Test
  void testEquals_clone_true() {
    final Mutant expected = newDetectedMutant();
    final Mutant twin = newDetectedMutant();
    assertEquals(expected, twin);
  }

  @Test
  void testEquals_differentDetected_false() {
    final Mutant expected = newDetectedMutant();
    final Mutant other = newUndetectedMutant();
    assertNotEquals(expected, other);
  }

  @Test
  void testEquals_differentStatus_false() {
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
  void testEquals_differentSourceFile_false() {
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
  void testEquals_differentMutatedClass_false() {
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
  void testEquals_differentMutatedMethod_false() {
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
  void testEquals_differentMethodDescription_false() {
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
  void testEquals_differentLineNumber_false() {
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
  void testEquals_differentMutator_false() {
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
  void testEquals_differentSuffix_false() {
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
  void testEquals_differentNumberOfTestsRun_false() {
    final Mutant expected = newDetectedMutant();
    final Mutant other = Mutant.builder()
        .mutantStatus(expected.getState())
        .inSourceFile(expected.getSourceFile())
        .inClass(expected.getMutatedClass())
        .inMethod(expected.getMutatedMethod())
        .inLine(expected.getLineNumber())
        .withMethodParameters(expected.getMethodDescription())
        .usingMutator(expected.getMutationOperator())
        .atIndex(expected.getIndex())
        .numberOfTestsRun(-1)
        .killedBy(expected.getKillingTest())
        .build();
    assertNotEquals(expected, other);
  }

  @Test
  void testEquals_differentIndex_false() {
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
  void testEquals_differentKillingTest_false() {
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
  void testEquals_differentDescription_false() {
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
  void testEquals_null_false() {
    assertNotEquals(null, newDetectedMutant());
  }

  @Test
  void testEquals_otherObject_false() {
    assertNotEquals(new Object(), newDetectedMutant());
  }

  @Test
  void testHashCode_detected_reproducible() {
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
  void testHashCode_undetected_reproducible() {
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
  void testHashCode_sameMutant() {
    Mutant mutant = newDetectedMutant();
    assertEquals(mutant.hashCode(), mutant.hashCode());
  }

  @Test
  void testHashCode_equalMutant_same() {
    assertEquals(newDetectedMutant().hashCode(), newDetectedMutant().hashCode());
  }

  @Test
  void testGetTestDescriptor_detected_nonEmptySpec() {
    Mutant mutant = newDetectedMutant();
    TestDescriptor td = mutant.getTestDescriptor();
    assertEquals("com.foo.bar.SomeClassKillingTest", td.getSpec());
  }

  @Test
  void testGetTestDescriptor_undetected_emptySpec() {
    Mutant mutant = newUndetectedMutant();
    TestDescriptor td = mutant.getTestDescriptor();
    assertEquals("", td.getSpec());
  }

  @Test
  void testHashCode_otherMutantObject_different() {
    assertNotEquals(newDetectedMutant().hashCode(), newUndetectedMutant().hashCode());
  }

  @Test
  void testBuilder() {
    final Mutant.Builder builder = Mutant.builder();
    assertNotNull(builder);
  }

}
