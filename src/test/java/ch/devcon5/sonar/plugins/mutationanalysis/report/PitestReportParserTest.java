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

package ch.devcon5.sonar.plugins.mutationanalysis.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant;
import ch.devcon5.sonar.plugins.mutationanalysis.model.MutationOperators;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class PitestReportParserTest {

   @Rule
   public TemporaryFolder folder = new TemporaryFolder();

   @Rule
   public ExpectedException expect = ExpectedException.none();

   private PitestReportParser subject;

   @Before
   public void setUp() {

      subject = new PitestReportParser();
   }

   @Test
   public void parseReport_findMutants_withoutDescription() throws IOException, URISyntaxException {

      // prepare
      final Path report = Paths.get(getClass().getResource("PitestReportParserTest_mutations.xml").toURI());

      // act
      final Collection<Mutant> mutants = subject.parseMutants(report);

      // assert
      assertEquals(3, mutants.size());

      assertTrue(mutants.contains(Mutant.builder()
                                        .mutantStatus(Mutant.State.KILLED)
                                        .inSourceFile("Mutant.java")
                                        .inClass("ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant")
                                        .inMethod("equals")
                                        .withMethodParameters("(Ljava/lang/Object;)Z")
                                        .inLine(162)
                                        .atIndex(5)
                                        .numberOfTestsRun(0)
                                        .usingMutator(MutationOperators.find("org.pitest.mutationtest.engine.gregor.mutators.NegateConditionalsMutator"))
                                        .killedBy(
                                                "ch.devcon5.sonar.plugins.mutationanalysis.model.MutantTest.testEquals_different_false(ch.devcon5.sonar.plugins.mutationanalysis.model.MutantTest)")
                                        .build()));

      assertTrue(mutants.contains(Mutant.builder()
                                        .mutantStatus(Mutant.State.SURVIVED)
                                        .inSourceFile("Mutant.java")
                                        .inClass("ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant")
                                        .inMethod("equals")
                                        .withMethodParameters("(Ljava/lang/Object;)Z")
                                        .inLine(172)
                                        .atIndex(43)
                                        .usingMutator(MutationOperators.find("org.pitest.mutationtest.engine.gregor.mutators.NegateConditionalsMutator"))
                                        .killedBy("")
                                        .build()));
      assertTrue(mutants.contains(Mutant.builder()
                                        .mutantStatus(Mutant.State.NO_COVERAGE)
                                        .inSourceFile("Mutant.java")
                                        .inClass("ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant")
                                        .inMethod("equals")
                                        .withMethodParameters("(Ljava/lang/Object;)Z")
                                        .inLine(175)
                                        .atIndex(55)
                                        .usingMutator(MutationOperators.find("org.pitest.mutationtest.engine.gregor.mutators.NegateConditionalsMutator"))
                                        .killedBy("")
                                        .build()));

      assertEquals(0, mutants.stream().filter(m -> m.getState() == Mutant.State.UNKNOWN).count());
      assertEquals(0, mutants.stream().filter(m -> m.getState() == Mutant.State.MEMORY_ERROR).count());
      assertEquals(0, mutants.stream().filter(m -> m.getState() == Mutant.State.TIMED_OUT).count());

   }

   @Test
   public void parseReport_findMutants_withDescriptions() throws IOException, URISyntaxException {

      // prepare
      final Path report = Paths.get(getClass().getResource("PitestReportParserTest_mutationsWithDescriptions.xml").toURI());
      final Mutant expected = Mutant.builder()
                                    .mutantStatus(Mutant.State.KILLED)
                                    .inSourceFile("Mutant.java")
                                    .inClass("ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant")
                                    .inMethod("equals")
                                    .withMethodParameters("(Ljava/lang/Object;)Z")
                                    .inLine(268)
                                    .atIndex(8)
                                    .usingMutator("org.pitest.mutationtest.engine.gregor.mutators.InlineConstantMutator")
                                    .killedBy("ch.devcon5.sonar.plugins.mutationanalysis.model.MutantTest.testEquals_same_true(ch.devcon5.sonar.plugins.mutationanalysis.model.MutantTest)")
                                    .withDescription("Substituted 1 with 0")
                                    .build();

      // act
      final Collection<Mutant> mutants = subject.parseMutants(report);

      // assert
      assertEquals(expected, mutants.iterator().next());
   }

   @Test
   public void parseReport_findMutants_withNumberOfTests() throws IOException, URISyntaxException {

      // prepare
      final Path report = Paths.get(getClass().getResource("PitestReportParserTest_mutationsWithNumTests.xml").toURI());
      final Mutant expected = Mutant.builder()
                                    .mutantStatus(Mutant.State.KILLED)
                                    .inSourceFile("Mutant.java")
                                    .inClass("ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant")
                                    .inMethod("equals")
                                    .withMethodParameters("(Ljava/lang/Object;)Z")
                                    .inLine(162)
                                    .atIndex(5)
                                    .numberOfTestsRun(40)
                                    .usingMutator("org.pitest.mutationtest.engine.gregor.mutators.NegateConditionalsMutator")
                                    .killedBy("ch.devcon5.sonar.plugins.mutationanalysis.model.MutantTest.testEquals_different_false(ch.devcon5.sonar.plugins.mutationanalysis.model.MutantTest)")
                                    .build();

      // act
      final Collection<Mutant> mutants = subject.parseMutants(report);

      // assert
      assertEquals(expected, mutants.iterator().next());
   }

   @Test
   public void parseReport_emptyFile_emptyList() throws Exception {

      Path emptyFile = folder.newFile().toPath();

      Collection<Mutant> result = subject.parseMutants(emptyFile);

      assertTrue(result.isEmpty());
   }

   @Test
   public void parseReport_brokenXml_emptyList() throws Exception {

      final Path report = Paths.get(getClass().getResource("PitestReportParserTest_broken.xml").toURI());

      Collection<Mutant> result = subject.parseMutants(report);

      assertTrue(result.isEmpty());
   }

   @Test
   public void parseReport_nonExistingFile_emptyList() throws Exception {

      Path missingFile = Paths.get("anyNonExistingPath");

      Collection<Mutant> result = subject.parseMutants(missingFile);

      assertTrue(result.isEmpty());
   }

   @Test
   public void readMutants_brokenXml_exceptionWithDetails() throws Exception {

      expect.expect(XMLStreamException.class);
      expect.expectMessage("ParseError at [row,col]:[23,5]\nMessage: sourceFile must be set");

      subject.readMutants(getClass().getResourceAsStream("PitestReportParserTest_broken.xml"));
   }


   @Test(expected = XMLStreamException.class)
   public void readMutants_XXE_attack_entityNotReplaced() throws Exception {

      //we prepare a secret file with content that should not be disclosed
      //this file acts as a placeholder for any file with sensitive information such as /etc/passwd
      final String expectedSecret = "MY_SECRET";
      File secretFile = folder.newFile("secret");
      Files.write(secretFile.toPath(), expectedSecret.getBytes("UTF-8"));

      //we forge a pitest report that should be processed by the plugin parser
      //the attack is not hypothetical, especially in managed sonarqube instances where forged
      //pitest reports may reveal sensitve information in the sonarqube results
      String template = IOUtils.toString(getClass().getResourceAsStream("PitestReportParserTest_XXE.xml"));
      String xxeAttack = template.replace("$SECRET$", secretFile.toURI().toURL().toString());

      Collection<Mutant> mutants = subject.readMutants(new ByteArrayInputStream(xxeAttack.getBytes("UTF-8")));

      //this code should never be  executed as the the processing of the xml should
      //already encounter an unresolvable entity (&xxe;), causing an exception
      Mutant mutant = mutants.iterator().next();
      String actualSecret = mutant.getMethodDescription();
      assertNotEquals(expectedSecret, actualSecret);
   }

}
