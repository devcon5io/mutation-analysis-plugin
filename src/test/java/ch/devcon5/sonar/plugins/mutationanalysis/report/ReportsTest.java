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


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReportsTest {

  @TempDir
  public Path folder;

  @Test
  void testReadMutants_fromDirectory_noReport() throws Exception {
    // act
    final Collection<Mutant> mutants = Reports.readMutants(folder);

    // assert
    assertNotNull(mutants);
    assertTrue(mutants.isEmpty());
  }

  @Test
  void testReadMutants_fromDirectory_withReport() throws Exception {
    // prepare
    fileFromResource("ReportsTest_mutations.xml", "mutations.xml");

    // act
    final Collection<Mutant> mutants = Reports.readMutants(folder);

    // assert
    assertNotNull(mutants);
    assertEquals(3, mutants.size());
  }

  @Test
  void testReadMutants_fromFile() throws Exception {
    // prepare
    final File file = fileFromResource("ReportsTest_mutations.xml", "mutations.xml");

    // act
    final Collection<Mutant> mutants = Reports.readMutants(file.toPath());

    // assert
    assertNotNull(mutants);
    assertEquals(3, mutants.size());
  }

  private File fileFromResource(final String resourcePath, final String fileName) throws IOException {
    final File newFile = Files.createFile(folder.resolve(fileName)).toFile();
    IOUtils.copy(getClass().getResourceAsStream(resourcePath), Files.newOutputStream(newFile.toPath()));
    return newFile;
  }

}
