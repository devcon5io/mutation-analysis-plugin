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

        //@formatter:off

        assertTrue(mutants.contains( new Mutant(true,Mutant.State.KILLED,"Mutant.java",
                "ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant","equals","(Ljava/lang/Object;)Z",162,
                MutationOperators.find("org.pitest.mutationtest.engine.gregor.mutators.NegateConditionalsMutator"),"",
                5,"ch.devcon5.sonar.plugins.mutationanalysis.model.MutantTest.testEquals_different_false(ch"
                                                    + ".devcon5.sonar.plugins.mutationanalysis.model.MutantTest)")));
        assertTrue(mutants.contains(new Mutant(false, Mutant.State.SURVIVED, "Mutant.java",
                "ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant", "equals","(Ljava/lang/Object;)Z", 172,
                MutationOperators.find("org.pitest.mutationtest.engine.gregor.mutators.NegateConditionalsMutator"), "",
                43,"")));
        assertTrue(mutants.contains(new Mutant(false,Mutant.State.NO_COVERAGE,"Mutant.java",
                "ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant","equals","(Ljava/lang/Object;)Z",175,
                MutationOperators.find("org.pitest.mutationtest.engine.gregor.mutators.NegateConditionalsMutator"),"",
                55, "")));
        // @formatter:on

        assertEquals(0, mutants.stream().filter(m -> m.getState() == Mutant.State.UNKNOWN).count());
        assertEquals(0, mutants.stream().filter(m -> m.getState() == Mutant.State.MEMORY_ERROR).count());
        assertEquals(0, mutants.stream().filter(m -> m.getState() == Mutant.State.UNKNOWN).count());

    }
}
