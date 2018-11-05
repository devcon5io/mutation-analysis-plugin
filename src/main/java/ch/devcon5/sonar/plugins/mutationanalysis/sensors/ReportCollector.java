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

package ch.devcon5.sonar.plugins.mutationanalysis.sensors;

import static org.slf4j.LoggerFactory.getLogger;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin;
import ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant;
import ch.devcon5.sonar.plugins.mutationanalysis.report.Reports;
import org.slf4j.Logger;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.config.Configuration;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *
 */
public class ReportCollector {

    private static final Logger LOG = getLogger(ReportCollector.class);
    public static final String POM_XML = "pom.xml";
    public static final String SETTINGS_GRADLE = "settings.gradle";

    private final Configuration settings;
    private final FileSystem fileSystem;
    private final XPath xpath;

    public ReportCollector(final Configuration configuration, FileSystem fileSystem) {
        this.settings = configuration;
        this.fileSystem = fileSystem;
        this.xpath = XPathFactory.newInstance().newXPath();
    }

    public Collection<Mutant> collectGlobalMutants(final SensorContext context) {

        final Collection<Mutant> globalMutants;
        if (MutationAnalysisPlugin.isExperimentalFeaturesEnabled(this.settings)) {
            globalMutants = collectReports(context);
        } else {
            globalMutants = Collections.emptyList();
        }
        return globalMutants;
    }

    /**
     * Collects all mutation reports from all parent and sibling modules. This method assumes a standard maven layout and
     * and a standard gradle layout
     *
     * @param context
     */
    private Collection<Mutant> collectReports(final SensorContext context) {
        //TODO add option to specify multi-module root
        final Path root = findProjectRoot(context.fileSystem().baseDir().toPath());
        LOG.info("Using {} as project root", root);
        final String reportDirectoryPath = getReportDirectoryPath();
        return findModuleRoots(root).map(module -> module.resolve(reportDirectoryPath)).flatMap(this::readMutantsFromReport).collect(Collectors.toList());

    }

    Path findProjectRoot(Path child) {

        LOG.debug("Searching project root for {}", child);
        //TODO support parents by relative path
        final Path parentPath = child.getParent();
        final List<Path> childModules = getModulePaths(parentPath);
        if (childModules.stream().anyMatch(module -> isSamePath(child, module))) {
            LOG.debug("Path {} is parent module of {}", parentPath, child);
            return findProjectRoot(parentPath);
        }
        LOG.debug("Path {} is not a child of {}", child, parentPath);
        return child;
    }

    private List<Path> getModulePaths(final Path parentPath) {
        final SortedSet<Path> pathSet = new TreeSet<>();
        try {

            //checking both maven and gradle module and retaining unique modules
            //for the case, when a project has both maven pom and gradle setting
            //to get the union of both modules

            if (parentPath.resolve(POM_XML).toFile().exists()) {
                pathSet.addAll(getModulePathsForMaven(parentPath.resolve(POM_XML)));
            }
            if (parentPath.resolve(SETTINGS_GRADLE).toFile().exists()) {
                pathSet.addAll(getModulePathsForGradle(parentPath.resolve(SETTINGS_GRADLE)));
            }
            return new ArrayList<>(pathSet);
        } catch (IOException | XPathExpressionException e) {
            LOG.warn("Could not parse {}", parentPath.toAbsolutePath(), e);
            return Collections.emptyList();
        }
    }

    private List<Path> getModulePathsForMaven(Path configurationFilePath) throws IOException, XPathExpressionException {

        final Path parent = configurationFilePath.getParent();
        final List<String> modulePaths = new ArrayList<>();
        final InputSource is;
        is = new InputSource(new FileReader(configurationFilePath.toFile()));
        //TODO add support for profile-activated modules
        final NodeList modules = (NodeList) this.xpath.evaluate("//*[local-name() = 'module']", is, XPathConstants.NODESET);
        //creating a pre-sized list is - mutation wise - equivalent to creating the list without size hint
        //we choose the less efficient way of not pre-sizing the array because this kills another mutant
        //nevertheless, if the size known before creation, one should create the issue with size
        for (int i = 0, len = modules.getLength(); i < len; i++) {
            modulePaths.add(modules.item(i).getTextContent());
        }
        return modulePaths.stream().map(parent::resolve).collect(Collectors.toList());
    }

    private List<Path> getModulePathsForGradle(Path configurationFilePath) throws IOException {

        final Path parent = configurationFilePath.getParent();
        final List<String> modulePaths = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(configurationFilePath.toFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.toUpperCase().startsWith("INCLUDE ")) {
                    modulePaths.addAll(Arrays.asList(line.substring("INCLUDE ".length()).replace("'", "").split(",")));
                }
            }
        }
        return modulePaths.stream().map(parent::resolve).collect(Collectors.toList());
    }

    //package protected visibilty for testing exception handling
    Stream<Mutant> readMutantsFromReport(final Path reportPath) {

        Stream<Mutant> result;
        try {
            result = Reports.readMutants(reportPath).stream();
        } catch (IOException e) {
            //this branch is really hard to reach through unit tests. And should only occur, if something is really wrong with the underlying filesystem
            LOG.error("Could not read report from path {}", reportPath, e);
            result = Stream.empty();
        }
        return result;
    }

    private String getReportDirectoryPath() {

        return settings.get(MutationAnalysisPlugin.REPORT_DIRECTORY_KEY).orElse(MutationAnalysisPlugin.REPORT_DIRECTORY_DEF);
    }

    private Stream<Path> findModuleRoots(final Path root) {

        return Stream.concat(Stream.of(root), getModulePaths(root).stream().flatMap(this::findModuleRoots));
    }

    //package protected visibilty for testing exception handling
    boolean isSamePath(final Path child, final Path module) {

        boolean result;
        try {
            result = Files.isSameFile(module, child);
        } catch (IOException e) {
            //this branch is really hard to reach through unit tests. And should only occur, if something is really wrong with the underlying filesystem
            LOG.error("Could not compare {} and {}", module, child, e);
            result = false;
        }
        return result;
    }


    /**
     * Reads the Mutants from the PIT reports for the current maven project the sensor analyzes
     *
     * @return a collection of all mutants found in the reports. If the report could not be located, the list is empty.
     * @throws IOException if the search for the report file failed
     */
    public Collection<Mutant> collectLocalMutants() throws IOException {

        return Reports.readMutants(getReportDirectory());
    }

    /**
     * Determine the absolute path of the directory where the PIT reports are located. The path is assembled using the
     * base directory of the fileSystem and the reports directory configured in the plugin's {@link org.sonar.api.config.Settings}.
     *
     * @return the path to PIT reports directory
     */
    private Path getReportDirectory() {

        return fileSystem.baseDir().toPath().resolve(getReportDirectoryPath());
    }
}
