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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant;
import ch.devcon5.sonar.plugins.mutationanalysis.model.MutationOperators;
import org.junit.Before;
import org.junit.Test;

public class PitestReportParserTest {

  private PitestReportParser subject;

  @Before
  public void setUp() {

    subject = new PitestReportParser();
  }

  @Test
  public void testParseReport_findMutants() throws IOException, URISyntaxException {

    // prepare
    final Path report = Paths.get(getClass().getResource("PitestReportParserTest_mutations.xml").toURI());

    // act
    final Collection<Mutant> mutants = subject.parseMutants(report);

    // assert
    assertEquals(3, mutants.size());


        assertTrue(mutants.contains(
            Mutant.builder()
                  .mutantStatus(Mutant.State.KILLED)
                  .inSourceFile("Mutant.java")
            .inClass("ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant")
            .inMethod("equals")
            .withMethodParameters("(Ljava/lang/Object;)Z")
            .inLine(162)
          .atIndex(5)
            .usingMutator(MutationOperators.find("org.pitest.mutationtest.engine.gregor.mutators.NegateConditionalsMutator"))
            .killedBy("ch.devcon5.sonar.plugins.mutationanalysis.model.MutantTest.testEquals_different_false(ch.devcon5.sonar.plugins.mutationanalysis.model.MutantTest)")
            .build()
            ));

        assertTrue(mutants.contains(
            Mutant.builder()
                  .mutantStatus(Mutant.State.SURVIVED)
                  .inSourceFile("Mutant.java")
                  .inClass("ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant")
                  .inMethod("equals")
                  .withMethodParameters("(Ljava/lang/Object;)Z")
                  .inLine(172)
                  .atIndex(43)
                  .usingMutator(MutationOperators.find("org.pitest.mutationtest.engine.gregor.mutators.NegateConditionalsMutator"))
                  .killedBy("")
                  .build()
            ));
        assertTrue(mutants.contains(
            Mutant.builder()
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
    assertEquals(0, mutants.stream().filter(m -> m.getState() == Mutant.State.UNKNOWN).count());

  }
}
