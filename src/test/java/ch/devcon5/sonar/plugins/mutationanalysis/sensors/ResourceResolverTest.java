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

import java.io.IOException;

import ch.devcon5.sonar.plugins.mutationanalysis.testharness.SensorTestHarness;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.FileSystem;

public class ResourceResolverTest {

   @ClassRule
   public static TemporaryFolder folder = new TemporaryFolder();

   private static SensorTestHarness HARNESS;
   private static FileSystem FILESYSTEM;

   private ResourceResolver resolver;

   @BeforeClass
   public static void setUpFilesystem() throws Exception {
      SensorTestHarness harness = SensorTestHarness.builder().withTempFolder(folder).build();

      harness.createSourceFile("src/main/java/ch/example/java/", "Example.java");
      harness.createSourceFile("src/main/java/ch/example/java/", "Example$Nested.java");
      harness.createSourceFile("src/main/java/ch/example/java/", "Example$Nested$Nested.java");
      harness.createSourceFile("src/main/kotlin/ch/example/kotlin/", "Example.kt");
      harness.createSourceFile("src/main/kotlin/ch/example/kotlin/", "Example$Nested.kt");
      harness.createSourceFile("src/main/kotlin/ch/example/kotlin/", "Example$Nested$Nested.kt");

      FILESYSTEM = harness.createSensorContext().scanFiles().fileSystem();
   }

   @Before
   public void setUp() throws Exception {
      resolver = new ResourceResolver(FILESYSTEM);
   }

   @Test
   public void resolve_javaFile() throws IOException {
      assertEquals("Example.java", resolver.resolve("ch.example.java.Example").get().filename());
   }
   @Test
   public void resolve_nestedJavaClass() throws IOException {
      assertEquals("Example.java", resolver.resolve("ch.example.java.Example$Nested").get().filename());
   }
   @Test
   public void resolve_deeplyNestedJavaClass() throws IOException {
      assertEquals("Example.java", resolver.resolve("ch.example.java.Example$Nested$Nested").get().filename());
   }
   @Test
   public void resolve_kotlinClass() throws IOException {
      assertEquals("Example.kt", resolver.resolve("ch.example.kotlin.Example").get().filename());
   }
   @Test
   public void resolve_nestedKotlinClass() throws IOException {
      assertEquals("Example.kt", resolver.resolve("ch.example.kotlin.Example$Nested").get().filename());
   }
   @Test
   public void resolve_deeplyNestedKotlinClass() throws IOException {
      assertEquals("Example.kt", resolver.resolve("ch.example.kotlin.Example$Nested$Nested").get().filename());
   }
}
