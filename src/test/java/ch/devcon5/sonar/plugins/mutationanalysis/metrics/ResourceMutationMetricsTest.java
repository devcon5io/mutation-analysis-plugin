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
package ch.devcon5.sonar.plugins.mutationanalysis.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.util.Collection;

import ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant;
import ch.devcon5.sonar.plugins.mutationanalysis.model.MutationOperators;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;

public class ResourceMutationMetricsTest {

    private InputFile resource = new DefaultInputFile(new DefaultIndexedFile("test", Paths.get("."), "src/main/java/example/Test.java", "java"), x -> {});

    private Mutant mutant = defaultMutantBuilder().mutantStatus(Mutant.State.UNKNOWN).build();

    private Mutant.Builder defaultMutantBuilder() {
        return Mutant.builder()
                     .inSourceFile("Test.java")
                     .inClass("example.Test")
                .inMethod("helloWorld")
                .withMethodParameters("(Ljava/lang/Object;)Z")
                .usingMutator(MutationOperators.find("MATH"));
    }

    private ResourceMutationMetrics subject = new ResourceMutationMetrics(resource);

    @Test
    public void testDefaults() throws Exception {
        ResourceMutationMetrics rmm = new ResourceMutationMetrics(resource);

        assertEquals(0, rmm.getMutationsDetected());
        assertEquals(0, rmm.getMutationsTotal());
        assertEquals(0, rmm.getMutationsKilled());
        assertEquals(0, rmm.getMutationsMemoryError());
        assertEquals(0, rmm.getMutationsNoCoverage());
        assertEquals(0, rmm.getMutationsSurvived());
        assertEquals(0, rmm.getMutationsTimedOut());
        assertEquals(0, rmm.getMutationsUnknown());
        assertEquals(0.0, rmm.getMutationCoverage(), 0.0001);
        assertTrue(rmm.getMutants().isEmpty());

    }

    @Test
    public void testAddMutant() throws Exception {

        // act
        subject.addMutant(mutant);
        subject.addMutant(mutant);
        subject.addMutant(mutant);

        // assert
        final Collection<Mutant> mutants = subject.getMutants();
        assertNotNull(mutants);
        assertEquals(3, mutants.size());

    }

    @Test
    public void testGetMutationsTotal() throws Exception {

        // prepare
        subject.addMutant(mutant);
        subject.addMutant(mutant);
        subject.addMutant(mutant);

        // act
        final int value = subject.getMutationsTotal();

        // assert
        assertEquals(3, value);
    }

    @Test
    public void testGetMutationsNoCoverage() throws Exception {

        // prepare
        Mutant mutant = defaultMutantBuilder().mutantStatus(Mutant.State.NO_COVERAGE).build();

        subject.addMutant(mutant);

        // act
        final int value = subject.getMutationsNoCoverage();

        // assert
        assertEquals(1, value);
    }

    @Test
    public void testGetMutationsKilled() throws Exception {

        // prepare
        Mutant mutant = defaultMutantBuilder().mutantStatus(Mutant.State.KILLED).killedBy("ATest").build();
        subject.addMutant(mutant);

        // act
        final int value = subject.getMutationsKilled();

        // assert
        assertEquals(1, value);
    }

    @Test
    public void testGetMutationsSurvived() throws Exception {

        // prepare
        Mutant mutant = defaultMutantBuilder().mutantStatus(Mutant.State.SURVIVED).build();
        subject.addMutant(mutant);

        // act
        final int value = subject.getMutationsSurvived();

        // assert
        assertEquals(1, value);
    }

    @Test
    public void testGetMutationsMemoryError() throws Exception {

        // prepare
        Mutant mutant = defaultMutantBuilder().mutantStatus(Mutant.State.MEMORY_ERROR).killedBy("ATest").build();
        subject.addMutant(mutant);

        // act
        final int value = subject.getMutationsMemoryError();

        // assert
        assertEquals(1, value);
    }

    @Test
    public void testGetMutationsTimedOut() throws Exception {

        // prepare
        Mutant mutant = defaultMutantBuilder().mutantStatus(Mutant.State.TIMED_OUT).killedBy("ATest").build();
        subject.addMutant(mutant);

        // act
        final int value = subject.getMutationsTimedOut();

        // assert
        assertEquals(1, value);
    }

    @Test
    public void testGetMutationsUnknown() throws Exception {

        // prepare
        Mutant mutant = defaultMutantBuilder().mutantStatus(Mutant.State.UNKNOWN).build();
        subject.addMutant(mutant);

        // act
        final int value = subject.getMutationsUnknown();

        // assert
        assertEquals(1, value);
    }

    @Test
    public void testGetMutationsDetected() throws Exception {

        // prepare
        subject.addMutant(defaultMutantBuilder().mutantStatus(Mutant.State.KILLED).killedBy("ATest").build());
        subject.addMutant(defaultMutantBuilder().mutantStatus(Mutant.State.SURVIVED).build());
        subject.addMutant(defaultMutantBuilder().mutantStatus(Mutant.State.NO_COVERAGE).build());

        // act
        final int value = subject.getMutationsDetected();

        // assert
        assertEquals(1, value);
    }

    @Test
    public void testGetMutationCoverage() throws Exception {

        // prepare
        subject.addMutant(defaultMutantBuilder().mutantStatus(Mutant.State.KILLED).killedBy("ATest").build());
        subject.addMutant(defaultMutantBuilder().mutantStatus(Mutant.State.SURVIVED).build());
        subject.addMutant(defaultMutantBuilder().mutantStatus(Mutant.State.NO_COVERAGE).build());

        // act
        final double value = subject.getMutationCoverage();

        // assert
        assertEquals(100.0 / 3.0, value, 0.000001);
    }

    @Test
    public void testGetMutationCoverage_fullCoverage() throws Exception {

        // prepare
        subject.addMutant(defaultMutantBuilder().mutantStatus(Mutant.State.KILLED).killedBy("ATest").build());
        subject.addMutant(defaultMutantBuilder().mutantStatus(Mutant.State.SURVIVED).build());
        subject.addMutant(defaultMutantBuilder().mutantStatus(Mutant.State.NO_COVERAGE).build());

        // act
        final double value = subject.getMutationCoverage();

        // assert
        assertEquals(33.33, value, 0.01);
    }

    @Test
    public void testGetResource() throws Exception {

        assertEquals(resource, subject.getResource());
    }

}
