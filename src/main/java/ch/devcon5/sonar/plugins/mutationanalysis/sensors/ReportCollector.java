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

import static ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin.PROJECT_ROOT_FOLDER;
import static javax.xml.xpath.XPathConstants.NODESET;
import static javax.xml.xpath.XPathConstants.STRING;
import static org.slf4j.LoggerFactory.getLogger;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
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

  /*
  This whole class should be refactored. For the moment it is ok-ish, but once other build tools
  are supported (i.e. from the JS world), it might be better to split support for multiple tools
  into different classes.
   */

  public static final String POM_XML = "pom.xml";
  public static final String SETTINGS_GRADLE = "settings.gradle";
  private static final String XPATH_RELATIVE_PARENT_PATH = "//*[local-name() = 'project']/*[local-name() = 'parent']/*[local-name() = 'relativePath']";
  private static final String XPATH_MODULE = "//*[local-name() = 'module']";
  private static final Logger LOG = getLogger(ReportCollector.class);
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
   * Reads the Mutants from the PIT reports for the current maven project the sensor analyzes
   *
   * @return a collection of all mutants found in the reports. If the report could not be located, the list is empty.
   *
   * @throws IOException
   *     if the search for the report file failed
   */
  public Collection<Mutant> collectLocalMutants() throws IOException {

    return Reports.readMutants(getReportDirectory());
  }

  /**
   * Collects all mutation reports from all parent and sibling modules. This method assumes a standard maven layout and
   * and a standard gradle layout
   *
   * @param context
   */
  private Collection<Mutant> collectReports(final SensorContext context) {

    final Path root = getProjectRootFromSettings().orElseGet(() -> findProjectRoot(context.fileSystem().baseDir().toPath()));
    LOG.info("Using {} as project root", root);
    final String reportDirectoryPath = getReportDirectoryPath();
    return findModuleRoots(root).map(module -> module.resolve(reportDirectoryPath)).flatMap(this::readMutantsFromReport).collect(Collectors.toList());

  }

  private Optional<Path> getProjectRootFromSettings() {

    return settings.get(PROJECT_ROOT_FOLDER).map(Paths::get);
  }

  Path findProjectRoot(Path child) {

    LOG.debug("Searching project root for {}", child);

    return getRelativeParentPathFromPom(child).orElseGet(() -> getParentPathFromFilesystem(child).orElse(child));
  }

  private Function<Path, Optional<Path>> findRootInParents(final Path child) {
    return parentPath -> {
      if(isMultiModuleParent(parentPath, child)) {
        LOG.debug("Path {} is parent module of {}", parentPath, child);
        return Optional.of(findProjectRoot(parentPath));
      } else {
        return Optional.empty();
      }
    };
  }

  /**
   * Checks if the specified parent is a multi-module reactor pom or settings.gradle that contains the child module in
   * it's module definition
   * @param parentPath
   *  the path to the presumed multi-module parent pom or settings.gradle
   * @param child
   *  the child that should be contained in the multi-module reactor pom or settings.gradle
   * @return
   *  true if the the parentPath refers to a multi-module parent and the child is referenced in its
   *  modules list
   */
  private boolean isMultiModuleParent(final Path parentPath, final Path child) {

    return getModulePaths(parentPath).stream().anyMatch(module -> isSamePath(child, module));
  }

  /**
   * Gets the parent for the child module from the folder structure of the filesystem. The parent path is checked
   * if it's a parent module of the module defined by child
   * @param child
   *  the child module for which the parent should be found
   * @return
   *  the parent folder that is a multi-module module that defines the child-module in its module list
   */
  private Optional<Path> getParentPathFromFilesystem(final Path child) {
    LOG.info("Could not determine project root of {} from parent", child);
    return findRootInParents(child).apply(child.getParent());
  }

  /**
   * Evaluates the relative path definition - if present - from the pom file of the child. If the element is present
   * and the parent exists, it's checked, whether the parent is a reactor module that defines the child in its
   * modules list.
   * @param child
   *  the path of the child module that should contain a pom file
   * @return
   *  the parent module that defines the child in its modules list.
   *  if neithe the pom.xml nor the relativePath element are defined, or the parent does not define the child in its
   *  modules list, an empty optional is returned
   */
  private Optional<Path> getRelativeParentPathFromPom(final Path child) {
    return resolveExisting(child, POM_XML).flatMap(pomXml -> {
      try (InputStream is = Files.newInputStream(pomXml)) {
        final InputSource in = new InputSource(is);
        return Optional.ofNullable((String) this.xpath.evaluate(XPATH_RELATIVE_PARENT_PATH, in, STRING))
                       .filter(relPath -> !relPath.isEmpty())
                       .map(child::resolve);
      } catch (IOException | XPathExpressionException e) {
        LOG.debug("Could not parse pom {}", pomXml, e);
        return Optional.empty();
      }
    }).flatMap(findRootInParents(child));
  }

  /**
   * Extracts all module definition from the current module. The module definitions can either be defined in a
   * reactor pom.xml or a settings.gradle file. If both exists, both are evaluated.
   * @param parentPath
   *  the path of the multi-module root folder
   * @return
   *  a list of all child module paths
   */
  private List<Path> getModulePaths(final Path parentPath) {

    final SortedSet<Path> pathSet = new TreeSet<>();

    //checking both maven and gradle module and retaining unique modules
    //for the case, when a project has both maven pom and gradle setting
    //to get the union of both modules

    resolveExisting(parentPath, POM_XML).map(this::getModulePathsForMaven).ifPresent(pathSet::addAll);
    resolveExisting(parentPath, SETTINGS_GRADLE).map(this::getModulePathsForGradle).ifPresent(pathSet::addAll);

    return new ArrayList<>(pathSet);
  }

  /**
   * Resolves the relativePath relative to the root path and checks if the resolved path exists.
   * @param root
   *  the root path from which the relative path should be resolved
   * @param relativePath
   *  the path relative to the root
   * @return
   *  the the resolved path if it exists or an empty optional if it doesn't exist
   */
  private Optional<Path> resolveExisting(Path root, String relativePath){
    //we don't need to run an existing check on the file, as any access on it will also result in an optional
    //and could therefore handle any cases where the file doesn't exist in its exception handling
    return Optional.of(root.resolve(relativePath));
  }

  private List<Path> getModulePathsForMaven(Path configurationFilePath) {

    final Path parent = configurationFilePath.getParent();
    final List<String> modulePaths = new ArrayList<>();

    try (InputStream is = Files.newInputStream(configurationFilePath)) {
      final InputSource in = new InputSource(is);
      //TODO add support for profile-activated modules
      final NodeList modules = (NodeList) this.xpath.evaluate(XPATH_MODULE, in, NODESET);
      //creating a pre-sized list is - mutation wise - equivalent to creating the list without size hint
      //we choose the less efficient way of not pre-sizing the array because this kills another mutant
      //nevertheless, if the size known before creation, one should create the issue with size
      for (int i = 0, len = modules.getLength(); i < len; i++) {
        modulePaths.add(modules.item(i).getTextContent());
      }
      return modulePaths.stream().map(parent::resolve).collect(Collectors.toList());
    } catch (IOException | XPathExpressionException e) {
      LOG.debug("Could not resolve module paths for pom {}",configurationFilePath, e);
      //we can safely return null as the method is used in a mapping of an optional, hence if the result of this
      // method is null, the Optional becomes empty. So as long as this method is private and consumer are capable
      // dealing with null, we could keep it that way. Otherwise Collections.emptySet() would be better
      return null;
    }
  }

  private List<Path> getModulePathsForGradle(Path configurationFilePath) {
    try (BufferedReader br = new BufferedReader(new FileReader(configurationFilePath.toFile()))) {
      final Path parent = configurationFilePath.getParent();
      final List<String> modulePaths = new ArrayList<>();
      String line;
      while ((line = br.readLine()) != null) {
        if (line.toUpperCase().startsWith("INCLUDE ")) {
          modulePaths.addAll(Arrays.asList(line.substring("INCLUDE ".length()).replace("'", "").split(",")));
        }
      }
      return modulePaths.stream().map(parent::resolve).collect(Collectors.toList());
    } catch (IOException e) {
      LOG.debug("Could not resolve gradle module paths for {}", configurationFilePath, e);
      //we can safely return null as the method is used in a mapping of an optional, hence if the result of this
      // method is null, the Optional becomes empty. So as long as this method is private and consumer are capable
      // dealing with null, we could keep it that way. Otherwise Collections.emptySet() would be better
      return null;
    }
  }

  //package protected visibilty for testing exception handling
  Stream<Mutant> readMutantsFromReport(final Path reportPath) {

    Stream<Mutant> result;
    try {
      result = Reports.readMutants(reportPath).stream();
    } catch (IOException e) {
      //this branch is really hard to reach through unit tests. And should only occur, if something is really wrong with the underlying filesystem
      LOG.debug("Could not read report from path {}", reportPath, e);
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
   * Determine the absolute path of the directory where the PIT reports are located. The path is assembled using the
   * base directory of the fileSystem and the reports directory configured in the plugin's {@link org.sonar.api.config.Settings}.
   *
   * @return the path to PIT reports directory
   */
  private Path getReportDirectory() {

    return fileSystem.baseDir().toPath().resolve(getReportDirectoryPath());
  }
}
