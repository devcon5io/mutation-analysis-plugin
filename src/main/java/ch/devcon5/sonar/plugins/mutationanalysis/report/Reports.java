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

import ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to read a {@link ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant}s from a report located in a
 * directory.
 */
public final class Reports {

  /**
   * SLF4J Logger for this class
   */
  private static final Logger LOG = LoggerFactory.getLogger(Reports.class);

  private Reports() {}

  /**
   * Reads the {@link ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant}s from the report in the reports directory.
   * The method searches for the most recent {@code mutations.xml} report and returns its contents as a list.
   *
   * @param reportsDirectory the {@link Path} to the directory containing the report.
   * @return a collection of all {@link ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant}s declared in the report
   * or an empty list if neither report was found nor it contained any
   * {@link ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant}s. The method does not return <code>null</code>
   * @throws IOException if the search for the report failed or the report could not be read.
   */
  public static Collection<Mutant> readMutants(final Path reportsDirectory) throws IOException {
    LOG.debug("Searching pit reports in {}", reportsDirectory);

    final Path xmlReport;
    if (reportsDirectory.toFile().isDirectory()) {
      xmlReport = new ReportFinder().findReport(reportsDirectory);
    } else {
      xmlReport = reportsDirectory;
    }

    if (xmlReport == null) {
      LOG.warn("No XML PIT report found in directory {} !", reportsDirectory);
      LOG.warn(
          "Checkout plugin documentation for more detailed explanations: https://github.com/devcon5io/mutation-analysis-plugin");
      return Collections.emptyList();
    }

    return new PitestReportParser().parseMutants(xmlReport);
  }

}
