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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StateTest {

    @Test
    public void testIsAlive_enumValues() throws Exception {

        assertTrue(Mutant.State.NO_COVERAGE.isAlive());
        assertTrue(Mutant.State.UNKNOWN.isAlive());
        assertTrue(Mutant.State.SURVIVED.isAlive());
        assertFalse(Mutant.State.MEMORY_ERROR.isAlive());
        assertFalse(Mutant.State.TIMED_OUT.isAlive());
        assertFalse(Mutant.State.KILLED.isAlive());
    }

    @Test
    public void testParse_enumValues() throws Exception {

        assertEquals(Mutant.State.NO_COVERAGE, Mutant.State.parse("NO_COVERAGE"));
        assertEquals(Mutant.State.KILLED, Mutant.State.parse("KILLED"));
        assertEquals(Mutant.State.SURVIVED, Mutant.State.parse("SURVIVED"));
        assertEquals(Mutant.State.MEMORY_ERROR, Mutant.State.parse("MEMORY_ERROR"));
        assertEquals(Mutant.State.TIMED_OUT, Mutant.State.parse("TIMED_OUT"));
        assertEquals(Mutant.State.UNKNOWN, Mutant.State.parse("UNKNOWN"));
    }

    @Test
    public void testParse_null_unknown() throws Exception {

        assertEquals(Mutant.State.UNKNOWN, Mutant.State.parse(null));
    }

    @Test
    public void testParse_unknown_unknown() throws Exception {

        assertEquals(Mutant.State.UNKNOWN, Mutant.State.parse("xxx"));
    }

}
