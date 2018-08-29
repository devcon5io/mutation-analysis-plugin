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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import ch.devcon5.sonar.plugins.mutationanalysis.report.ReportFinder.ReportFinderVisitor;
import ch.devcon5.sonar.plugins.mutationanalysis.testharness.TestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class ReportFinderTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public ExpectedException expects = ExpectedException.none();

    private ReportFinder subject;

    @Before
    public void setUp() throws Exception {
        subject = new ReportFinder();
    }

    @Test
    public void testFindReport_existingReport() throws IOException {

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
    public void testFindReport_noReportInDirectory() throws IOException {

        final Path directory = folder.newFolder().toPath();

        // act
        final Path report = subject.findReport(directory);

        // assert
        assertNull(report);
    }

    @Test
    public void testFindReport_nullPath_nullReportPath() throws IOException {

        final Path report = subject.findReport(null);

        assertNull(report);
    }

    @Test
    public void testIsNewer_newer_true() throws Exception {

        // prepare

        final Path older = folder.newFile().toPath();
        final Path newer = folder.newFile().toPath();
        Files.setLastModifiedTime(older, FileTime.fromMillis(1000L));
        Files.setLastModifiedTime(newer, FileTime.fromMillis(2000L));
        // act
        final boolean result = subject.isNewer(older, newer);

        // assert

        assertTrue(result);
    }

    @Test
    public void testIsNewer_older_false() throws Exception {

        // prepare

        final Path older = folder.newFile().toPath();
        final Path newer = folder.newFile().toPath();
        Files.setLastModifiedTime(older, FileTime.fromMillis(1000L));
        Files.setLastModifiedTime(newer, FileTime.fromMillis(2000L));
        // act
        final boolean result = subject.isNewer(newer, older);

        // assert

        assertFalse(result);
    }

    @Test
    public void testIsNewer_equals_false() throws Exception {

        // prepare
        final Path older = folder.newFile().toPath();
        Files.setLastModifiedTime(older, FileTime.fromMillis(1000L));

        // act
        final boolean result = subject.isNewer(older, older);

        // assert

        assertFalse(result);
    }

    @Test
    public void testFindMostRecentReport_notMatchingPattern() throws Exception {

        folder.newFile("someFile.txt").toPath();
        // act
        final Path report = subject.findMostRecentReport(folder.getRoot().toPath(), "*.xml");

        // assert
        assertNull(report);
    }

    @Test
    public void testFindMostRecentReport_matchingPatternOnce() throws Exception {

        // prepare
        final Path newFile = folder.newFile("someFile.xml").toPath();
        // act
        final Path report = subject.findMostRecentReport(folder.getRoot().toPath(), "*.xml");

        // assert
        assertNotNull(report);
        assertEquals(newFile, report);
    }

    @Test
    public void testFindMostRecentReport_matchingPatternNewerFile() throws Exception {

        // prepare
        final Path newFile1 = folder.newFile("someFile1.xml").toPath();
        final Path newFile2 = folder.newFile("someFile2.xml").toPath();
        Files.setLastModifiedTime(newFile1, FileTime.fromMillis(1000L));
        Files.setLastModifiedTime(newFile2, FileTime.fromMillis(2000L));

        // act
        final Path report = subject.findMostRecentReport(folder.getRoot().toPath(), "*.xml");

        // assert
        assertNotNull(report);
        assertEquals(newFile2, report);
    }

    @Test
    public void testFindMostRecentReport_matchingPatternNewerFile_reverseOrder() throws Exception {

        // prepare
        final Path newFile1 = folder.newFile("someFile2.xml").toPath();
        final Path newFile2 = folder.newFile("someFile1.xml").toPath();
        Files.setLastModifiedTime(newFile1, FileTime.fromMillis(1000L));
        Files.setLastModifiedTime(newFile2, FileTime.fromMillis(2000L));

        // act
        final Path report = subject.findMostRecentReport(folder.getRoot().toPath(), "*.xml");

        // assert
        assertNotNull(report);
        assertEquals(newFile2, report);
    }


    @Test
    public void testFindMostRecentReport_inSubDirectories() throws IOException {

        // prepare
        final File olderReport = TestUtils.tempFileFromResource(folder, "target/pitest-reports/subDirectory1/mutations.xml",
                getClass(), "ReportFinderTest_mutations.xml");
        final File newReport = TestUtils.tempFileFromResource(folder, "target/pitest-reports/subDirectory2/mutations.xml",
                getClass(), "ReportFinderTest_mutations.xml");
        Files.setLastModifiedTime(olderReport.toPath(), FileTime.fromMillis(1000L));
        Files.setLastModifiedTime(newReport.toPath(), FileTime.fromMillis(2000L));

        // act
        final Path report = subject.findMostRecentReport(folder.getRoot().toPath(), "*.xml");

        // assert
        assertNotNull(report);
        assertEquals(newReport.toPath(), report);
    }

    @Test
    public void nullCheck_ReportFinderVisitor() throws Exception {

        expects.expect(IllegalArgumentException.class);
        expects.expectMessage("file must not be null");

        ReportFinderVisitor visitor =  new ReportFinderVisitor("mutatations.xml");
        visitor.visitFile(null, null);
    }

    @Test
    public void nullPath_ReportFinderVisitor() throws Exception {

        final ReportFinderVisitor visitor =  new ReportFinderVisitor("mutatations.xml");

        //according to javadoc, the filename can be null
        //however at least on windows, its practically impossible to construct such a path
        //therefore I find no other way than to mock the Path in order to check
        //if implementation is null-safe
        Path file = mock(Path.class);
        when(file.getFileName()).thenReturn(null);

        final FileVisitResult result = visitor.visitFile(file, null);

        assertEquals(FileVisitResult.CONTINUE, result);
        assertTrue(visitor.getReports().isEmpty());
    }
}
