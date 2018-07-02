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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Searches the latest xml file in the reports directory.
 *
 */
public class ReportFinder {

  /**
   * SLF4J Logger for this class
   */
  private static final Logger LOG = LoggerFactory.getLogger(ReportFinder.class);

  /**
   * Finds the PIT report in the given report directory.
   *
   * @param reportDirectory
   *         the report directory to search for the report. The report directory must not be <code>null</code>, must
   *         exist and must be a directory.
   *
   * @return the Path to the found PIT report or null, if no report was found or the directory is no valid directory
   *
   * @throws IOException
   *         if the most recent report could not be determined
   */
  public Path findReport(final Path reportDirectory) throws IOException {

    if (reportDirectory == null || !Files.exists(reportDirectory) || !Files.isDirectory(reportDirectory)) {
      LOG.warn("ReportDirectory {} is no valid directory", reportDirectory);
      return null;
    }

    return findMostRecentReport(reportDirectory, "*.xml");
  }

  /**
   * Locates the most recent report in the report directory by searching all xml files in the reports directory and
   * selecting the most recent file.
   *
   * @param reportDirectory
   *         the path to the report directory to search the report in
   * @param pattern
   *         a globbing pattern, i.e. *.java or *.xml
   *
   * @return the {@link Path} to the most recent report
   *
   * @throws java.io.IOException if the report or the directory of the report can not be accessed
   */
  protected Path findMostRecentReport(final Path reportDirectory, final String pattern) throws IOException {

    Path mostRecent = null;
    try (DirectoryStream<Path> reports = Files.newDirectoryStream(reportDirectory, pattern)) {
      for (final Path report : reports) {
        if (mostRecent == null || isNewer(mostRecent, report)) {
          mostRecent = report;
        }
      }
    }
    return mostRecent;
  }

  /**
   * Determines if the otherPath is newer than the referencePath.
   *
   * @param referencePath
   *         the path to compare the other path against
   * @param otherPath
   *         the other path to be comapred against the reference path
   *
   * @return <code>true</code> if the otherPath is newer than the referencePath
   *
   * @throws IOException if the last modification time can not be determined
   */
  protected boolean isNewer(final Path referencePath, final Path otherPath) throws IOException {

    return Files.getLastModifiedTime(referencePath).compareTo(Files.getLastModifiedTime(otherPath)) < 0;
  }
}
