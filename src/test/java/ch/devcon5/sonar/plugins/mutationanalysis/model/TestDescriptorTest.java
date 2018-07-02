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
import static org.junit.Assert.assertNotEquals;

import java.util.Objects;

import org.junit.Test;

/**
 *
 */
public class TestDescriptorTest {

  @Test
  public void standardClass() {

    TestDescriptor td = new TestDescriptor("some.package.ClassName.testMethod(some.package.ClassName)");
    assertEquals("some.package.ClassName", td.getClassName());
    assertEquals("testMethod", td.getMethodName());
  }

  @Test
  public void standardClass_noMethod() {

    TestDescriptor td = new TestDescriptor("some.package.ClassName");
    assertEquals("some.package.ClassName", td.getClassName());
    assertEquals("unknown", td.getMethodName());
  }


  @Test
  public void nestedClass() {

    TestDescriptor td = new TestDescriptor("some.package.ClassName$SubTest.testMethod(some.package.ClassName$SubTest)");
    assertEquals("some.package.ClassName", td.getClassName());
    assertEquals("testMethod", td.getMethodName());
  }

  @Test
  public void nestedClass_noMethod() {

    TestDescriptor td = new TestDescriptor("some.package.ClassName$SubTest");
    assertEquals("some.package.ClassName", td.getClassName());
    assertEquals("unknown", td.getMethodName());
  }

  @Test
  public void unknownSpec() throws Exception {
    TestDescriptor td = new TestDescriptor("some/package/ClassName");

    assertEquals("some/package/ClassName", td.getSpec());
    assertEquals("some/package/ClassName", td.getClassName());
    assertEquals("unknown", td.getMethodName());

  }

  @Test
  public void getSpec() {
    TestDescriptor td = new TestDescriptor("some.package.ClassName.testMethod(some.package.ClassName)");

    assertEquals("some.package.ClassName.testMethod(some.package.ClassName)", td.getSpec());
  }

  @Test
  public void test_toString() {
    TestDescriptor td = new TestDescriptor("some.package.ClassName.testMethod(some.package.ClassName)");
    assertEquals("TestDescriptor{class='some.package.ClassName', method='testMethod'}", td.toString());

  }

  @Test
  public void test_equals() {
    TestDescriptor td1 = new TestDescriptor("some.package.ClassName.testMethod_one(some.package.ClassName)");
    TestDescriptor td2 = new TestDescriptor("some.package.ClassName.testMethod_two(some.package.ClassName)");

    assertEquals(td1, td2);
  }

  @Test
  public void test_equals_sameRef() {
    TestDescriptor td1 = new TestDescriptor("some.package.ClassName.testMethod_one(some.package.ClassName)");

    assertEquals(td1, td1);
  }

  @Test
  public void test_equals_differentClass() {
    TestDescriptor td1 = new TestDescriptor("some.package.ClassName.testMethod_one(some.package.ClassName)");

    assertNotEquals(td1, new Object());
  }

  @Test
  public void test_equals_not() {
    TestDescriptor td1 = new TestDescriptor("some.package.ClassName.testMethod_one(some.package.ClassName)");
    TestDescriptor td2 = new TestDescriptor("some.otherpackage.ClassName.testMethod_two(some.package.ClassName)");

    assertNotEquals(td1, td2);
  }

  @Test
  public void test_hashCode() {
    TestDescriptor td = new TestDescriptor("some.package.ClassName.testMethod_one(some.package.ClassName)");
    assertEquals(Objects.hash("some.package.ClassName"), td.hashCode());

  }
}
