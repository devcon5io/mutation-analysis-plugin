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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.devcon5.sonar.plugins.mutationanalysis.report.ReportFinder.ReportFinderVisitor;
import ch.devcon5.sonar.plugins.mutationanalysis.testharness.TestUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReportFinderTest {

  @TempDir
  public Path folder;

  private ReportFinder subject;

  @BeforeEach
  public void setUp() throws Exception {
    subject = new ReportFinder();
  }

  @Test
  void testFindReport_existingReport() throws IOException {
    // prepare
    final File reportsFile = TestUtils.tempFileFromResource(folder, "target/pitest-reports/mutations.xml",
        getClass(), "ReportFinderTest_mutations.xml");
    final Path directory = reportsFile.toPath().getParent();

    // act
    final Path report = subject.findReport(directory);

    // assert
    assertEquals(reportsFile.toPath(), report);
  }

  @Test
  void testFindReport_noReportInDirectory_nullReportPath() throws IOException {
    // act
    final Path report = subject.findReport(folder);

    // assert
    assertNull(report);
  }

  @Test
  void testFindReport_noReportDirectory_nullReportPath() throws IOException {
    // prepare
    final Path directory = Paths.get("nonexisting");

    // act
    final Path report = subject.findReport(directory);

    // assert
    assertNull(report);
  }

  @Test
  void testFindReport_nullPath_nullReportPath() throws IOException {
    final Path report = subject.findReport(null);
    assertNull(report);
  }

  @Test
  void testIsNewer_newer_true() throws Exception {
    // prepare
    final Path older = Files.createFile(folder.resolve("testIsNewer_older_true_one"));
    final Path newer = Files.createFile(folder.resolve("testIsNewer_older_true_two"));
    Files.setLastModifiedTime(older, FileTime.fromMillis(1000L));
    Files.setLastModifiedTime(newer, FileTime.fromMillis(2000L));

    // act
    final boolean result = subject.isNewer(older, newer);

    // assert
    assertTrue(result);
  }

  @Test
  void testIsNewer_older_false() throws Exception {
    // prepare
    final Path older = Files.createFile(folder.resolve("testIsNewer_older_false_one"));
    final Path newer = Files.createFile(folder.resolve("testIsNewer_older_false_two"));
    Files.setLastModifiedTime(older, FileTime.fromMillis(1000L));
    Files.setLastModifiedTime(newer, FileTime.fromMillis(2000L));

    // act
    final boolean result = subject.isNewer(newer, older);

    // assert
    assertFalse(result);
  }

  @Test
  void testIsNewer_equals_false() throws Exception {
    // prepare
    final Path older = Files.createFile(folder.resolve("testIsNewer_equals_false_one"));
    Files.setLastModifiedTime(older, FileTime.fromMillis(1000L));

    // act
    final boolean result = subject.isNewer(older, older);

    // assert
    assertFalse(result);
  }

  @Test
  void testFindMostRecentReport_notMatchingPattern() throws Exception {
    // prepare
    folder.resolve("someFile.txt");

    // act
    final Path report = subject.findMostRecentReport(folder, "*.xml");

    // assert
    assertNull(report);
  }

  @Test
  void testFindMostRecentReport_matchingPatternOnce() throws Exception {
    // prepare
    final Path newFile = Files.createFile(folder.resolve("someFile.xml"));

    // act
    final Path report = subject.findMostRecentReport(folder, "*.xml");

    // assert
    assertNotNull(report);
    assertEquals(newFile, report);
  }

  @Test
  void testFindMostRecentReport_matchingPatternNewerFile() throws Exception {
    // prepare
    final Path newFile1 = Files.createFile(folder.resolve("someFile1.xml"));
    final Path newFile2 = Files.createFile(folder.resolve("someFile2.xml"));
    Files.setLastModifiedTime(newFile1, FileTime.fromMillis(1000L));
    Files.setLastModifiedTime(newFile2, FileTime.fromMillis(2000L));

    // act
    final Path report = subject.findMostRecentReport(folder, "*.xml");

    // assert
    assertNotNull(report);
    assertEquals(newFile2, report);
  }

  @Test
  void testFindMostRecentReport_matchingPatternNewerFile_reverseOrder() throws Exception {
    // prepare
    final Path newFile1 = Files.createFile(folder.resolve("someFile2.xml"));
    final Path newFile2 = Files.createFile(folder.resolve("someFile1.xml"));
    Files.setLastModifiedTime(newFile1, FileTime.fromMillis(1000L));
    Files.setLastModifiedTime(newFile2, FileTime.fromMillis(2000L));

    // act
    final Path report = subject.findMostRecentReport(folder, "*.xml");

    // assert
    assertNotNull(report);
    assertEquals(newFile2, report);
  }

  @Test
  void testFindMostRecentReport_inSubDirectories() throws IOException {
    // prepare
    final File olderReport = TestUtils.tempFileFromResource(folder, "target/pitest-reports/subDirectory1/mutations.xml",
        getClass(), "ReportFinderTest_mutations.xml");
    final File newReport = TestUtils.tempFileFromResource(folder, "target/pitest-reports/subDirectory2/mutations.xml",
        getClass(), "ReportFinderTest_mutations.xml");
    Files.setLastModifiedTime(olderReport.toPath(), FileTime.fromMillis(1000L));
    Files.setLastModifiedTime(newReport.toPath(), FileTime.fromMillis(2000L));

    // act
    final Path report = subject.findMostRecentReport(folder, "*.xml");

    // assert
    assertNotNull(report);
    assertEquals(newReport.toPath(), report);
  }

  @Test
  void nullCheck_ReportFinderVisitor() {
    ReportFinderVisitor visitor = new ReportFinderVisitor("mutatations.xml");
    IllegalArgumentException thrownException = assertThrows(IllegalArgumentException.class, () ->
        visitor.visitFile(null, null));
    assertEquals("file must not be null", thrownException.getMessage());
  }

  @Test
  void nullPath_ReportFinderVisitor() {
    final ReportFinderVisitor visitor = new ReportFinderVisitor("mutatations.xml");
    final FileVisitResult result = visitor.visitFile(Paths.get("/"), null);
    assertEquals(FileVisitResult.CONTINUE, result);
    assertTrue(visitor.getReports().isEmpty());
  }

}
