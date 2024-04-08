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

package ch.devcon5.sonar.plugins.mutationanalysis.testharness;

import static org.slf4j.LoggerFactory.getLogger;

import ch.devcon5.sonar.plugins.mutationanalysis.metrics.ResourceMutationMetrics;
import ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant;
import ch.devcon5.sonar.plugins.mutationanalysis.model.MutationOperator;
import ch.devcon5.sonar.plugins.mutationanalysis.model.MutationOperators;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.batch.fs.internal.Metadata;
import org.sonar.api.batch.fs.internal.SensorStrategy;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.DefaultActiveRules;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.code.NewSignificantCode;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.batch.sensor.coverage.internal.DefaultCoverage;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.batch.sensor.cpd.internal.DefaultCpdTokens;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.batch.sensor.error.NewAnalysisError;
import org.sonar.api.batch.sensor.error.internal.DefaultAnalysisError;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.internal.DefaultHighlighting;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.NewExternalIssue;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.batch.sensor.rule.AdHocRule;
import org.sonar.api.batch.sensor.rule.NewAdHocRule;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.api.batch.sensor.symbol.internal.DefaultSymbolTable;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.scanner.fs.InputProject;
import org.sonar.api.utils.PathUtils;
import org.sonar.api.utils.Version;

/**
 * Sensor context allowing to read files and record issues and measures
 */
public class TestSensorContext implements SensorContext {

  private static final Logger LOGGER = getLogger(TestSensorContext.class);

  private static final List<MutationOperator> MUTATION_OPERATORS = Collections.unmodifiableList(
      new ArrayList<>(MutationOperators.allMutationOperators()));
  private final DefaultFileSystem fs;
  private final DefaultInputProject inputProject;
  private final InputModule inputModule;
  private final MapSettings settings = new MapSettings();
  private Configuration configuration = new TestConfiguration();
  private final SonarRuntime runtime = SonarRuntimeImpl.forSonarQube(
      Version.create(8, 9), SonarQubeSide.SCANNER, SonarEdition.COMMUNITY);
  private final Version version = Version.create(6, 5);
  private final TestSensorStorage storage = new TestSensorStorage();
  private ActiveRules activeRules = new DefaultActiveRules(Collections.emptyList());

  TestSensorContext(final Path basePath, String moduleName) {
    this.fs = new DefaultFileSystem(basePath).setWorkDir(basePath);
    final ProjectDefinition pd = ProjectDefinition.create()
        .setName(moduleName)
        .setKey(moduleName)
        .setBaseDir(basePath.toFile())
        .setWorkDir(basePath.toFile());
    this.inputProject = new DefaultInputProject(pd);
    this.inputModule = new DefaultInputModule(pd);
  }

  public static MutationOperator getMutationOperatorForLine(final int line) {
    return MUTATION_OPERATORS.get(line % MUTATION_OPERATORS.size());
  }

  @Override
  public Settings settings() {
    return settings;
  }

  @Override
  public Configuration config() {
    return configuration;
  }

  @Override
  public FileSystem fileSystem() {
    return fs;
  }

  @Override
  public ActiveRules activeRules() {
    return activeRules;
  }

  @Override
  public InputModule module() {
    return inputModule;
  }

  @Override
  public InputProject project() {
    return inputProject;
  }

  @Override
  public Version getSonarQubeVersion() {
    return version;
  }

  @Override
  public SonarRuntime runtime() {
    return runtime;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public <G extends Serializable> NewMeasure<G> newMeasure() {
    return new DefaultMeasure<>(this.storage);
  }

  @Override
  public NewIssue newIssue() {
    return new DefaultIssue(this.inputProject, this.storage);
  }

  @Override
  public NewExternalIssue newExternalIssue() {
    return null;
  }

  @Override
  public NewAdHocRule newAdHocRule() {
    return null;
  }

  @Override
  public NewHighlighting newHighlighting() {

    return new DefaultHighlighting(this.storage);
  }

  @Override
  public NewSymbolTable newSymbolTable() {
    return new DefaultSymbolTable(this.storage);
  }

  @Override
  public NewCoverage newCoverage() {
    return new DefaultCoverage(this.storage);
  }

  @Override
  public NewCpdTokens newCpdTokens() {
    return new DefaultCpdTokens(this.storage);
  }

  @Override
  public NewAnalysisError newAnalysisError() {
    return new DefaultAnalysisError();
  }

  @Override
  public NewSignificantCode newSignificantCode() {
    return null;
  }

  @Override
  public void addContextProperty(final String key, final String value) {
    this.storage.storeProperty(key, value);
  }

  @Override
  public void markForPublishing(final InputFile inputFile) {
  }

  /**
   * Uses configuration that is backed by the settings map. If not invoked, the configuration object is separated.
   *
   * @return
   */
  public TestSensorContext useBridgedConfiguration() {
    this.configuration = settings.asConfig();
    return this;
  }

  public MapSettings getSettings() {
    return settings;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public SonarRuntime getRuntime() {
    return runtime;
  }

  public TestSensorStorage getStorage() {
    return storage;
  }

  public TestSensorContext withActiveRules(NewActiveRule... rules) {
    this.activeRules = new DefaultActiveRules(Arrays.asList(rules));
    return this;
  }

  public InputFile addTestFile(String filename) throws IOException {
    return addTestFile(filename, md -> {
    });
  }

  public InputFile addTestFile(String filename, Consumer<TestFileMetadata> mdGenerator) throws IOException {
    final TestFileMetadata metadata = new TestFileMetadata();
    mdGenerator.accept(metadata);
    return addTestFile(filename, metadata);
  }

  public ResourceMutationMetrics newResourceMutationMetrics(String filename, Consumer<TestFileMetadata> mdGenerator) {
    final TestFileMetadata metadata = new TestFileMetadata();
    mdGenerator.accept(metadata);
    try {
      ResourceMutationMetrics rmm = new ResourceMutationMetrics(addTestFile(filename, metadata));
      addMutants(rmm, Mutant.State.UNKNOWN, metadata.mutants.unknown, metadata);
      addMutants(rmm, Mutant.State.NO_COVERAGE, metadata.mutants.noCoverage, metadata);
      addMutants(rmm, Mutant.State.SURVIVED, metadata.mutants.survived, metadata);
      addMutants(rmm, Mutant.State.TIMED_OUT, metadata.mutants.timedOut, metadata);
      addMutants(rmm, Mutant.State.MEMORY_ERROR, metadata.mutants.memoryError, metadata);
      addMutants(rmm, Mutant.State.KILLED, metadata.mutants.killed, metadata);
      return rmm;
    } catch (IOException e) {
      throw new RuntimeException("Failed to create test file", e);
    }
  }

  /**
   * Method for conveniently setting a test configuration
   *
   * @param key the key of the configuration by which it can be retrieved as well
   * @param aValue
   */
  public TestSensorContext setConfiguration(final String key, final Object aValue) {
    ((TestConfiguration) configuration).set(key, aValue);
    return this;
  }

  public DefaultInputFile registerFile(final String filename) {
    return registerFile(filename, new TestFileMetadata());
  }

  public DefaultInputFile registerFile(final String filename, final TestFileMetadata metadata) {
    final DefaultInputFile file = createNewInputFile(filename, metadata);
    this.fs.add(file);
    return file;
  }

  /**
   * Scans the (physical) filesystem of the sensor context to register all files in the sensor filesystem (cached)
   *
   * @return
   * @throws IOException
   */
  public TestSensorContext scanFiles() throws IOException {
    final AtomicInteger files = new AtomicInteger();
    final Path base = this.fs.baseDirPath();

    Files.walkFileTree(base, new SimpleFileVisitor<Path>() {

      @Override
      public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
        final FileVisitResult result = super.visitFile(file, attrs);
        registerFile(base.relativize(file).toString());
        files.incrementAndGet();
        return result;
      }
    });

    LOGGER.info("Scanned {} files", files.get());
    return this;
  }

  private InputFile addTestFile(String filename, TestFileMetadata metadata) throws IOException {
    final Path basePath = this.fs.baseDirPath();
    final Path filePath = basePath.resolve(filename);
    final Path fileDir = filePath.getParent();
    if (!Files.exists(fileDir)) {
      Files.createDirectories(fileDir);
    }
    Files.createFile(filePath);
    return registerFile(filename, metadata);
  }

  private DefaultInputFile createNewInputFile(final String filename, TestFileMetadata md) {
    final String fileExtension = filename.substring(filename.lastIndexOf('.') + 1);
    final String language;
    if ("java".equals(fileExtension)) {
      language = "java";
    } else if ("kt".equals(fileExtension)) {
      language = "kotlin";
    } else {
      language = "unknown";
    }

    DefaultIndexedFile indexedFile;
    if (md.isTestResource) {
      indexedFile = new DefaultIndexedFile(this.fs.baseDirPath().resolve(filename),
          this.inputModule.key(),
          PathUtils.sanitize(filename),
          PathUtils.sanitize(filename),
          InputFile.Type.TEST,
          language,
          TestInputFileBuilder.nextBatchId(),
          new SensorStrategy());
    } else {
      indexedFile = new DefaultIndexedFile(this.inputModule.key(), this.fs.baseDirPath(), filename, language);
    }
    final DefaultInputFile file = new DefaultInputFile(indexedFile, dif -> dif.setMetadata(md.toMetadata()));
    file.checkMetadata();
    return file;
  }


  private void addMutants(final ResourceMutationMetrics rmm, final Mutant.State state, final int count,
      TestFileMetadata metadata) {
    String filename = rmm.getResource().filename();
    for (int i = 0; i < count; i++) {
      rmm.addMutant(newMutant(filename, state, count + i, metadata));
    }
  }

  private Mutant newMutant(String file, Mutant.State state, final int line, TestFileMetadata metadata) {
    return Mutant.builder()
        .mutantStatus(state)
        .inSourceFile(file)
        .inClass(file.substring(0, file.lastIndexOf('.')).replaceAll("/|\\\\", "."))
        .inMethod("aMethod")
        .withMethodParameters("desc")
        .inLine(line)
        .atIndex(0)
        .numberOfTestsRun(metadata.mutants.numTestRun)
        .usingMutator(getMutationOperatorForLine(line))
        .killedBy(metadata.test.name)
        .withDescription(metadata.mutants.description)
        .build();
  }

  public static class TestFileMetadata {

    public final TestFileMetadata.MutantMetadata mutants = new TestFileMetadata.MutantMetadata();
    public final TestFileMetadata.TestMetadata test = new TestFileMetadata.TestMetadata();
    public int lines = 1;
    public int nonBlankLines = 1;
    public String hash = "";
    public int lastValidOffset = 1;
    public boolean isTestResource;

    public Metadata toMetadata() {
      int[] originalLineEndOffsets = IntStream.range(0, lines).map(i -> ((i + 1) * 80) - 1).toArray();
      int[] originalLineStartOffsets = IntStream.range(0, lines).map(i -> i * 80).toArray();
      return new Metadata(lines, nonBlankLines, hash, originalLineStartOffsets, originalLineEndOffsets,
          lastValidOffset);
    }

    public static class MutantMetadata implements Serializable {

      private static final long serialVersionUID = 7717508300632296958L;
      public int unknown = 0;
      public int noCoverage = 0;
      public int survived = 0;
      public int memoryError = 0;
      public int timedOut = 0;
      public int killed = 0;
      public String description;
      public int numTestRun;
    }

    public static class TestMetadata implements Serializable {

      private static final long serialVersionUID = 7691838474280645687L;
      public String name = "ATest";
    }

  }

  public static class TestSensorStorage implements SensorStorage {

    private List<Measure> measures = new ArrayList<>();
    private List<Issue> issues = new ArrayList<>();
    private List<ExternalIssue> extIssues = new ArrayList<>();
    private List<AdHocRule> adHocRules = new ArrayList<>();
    private List<DefaultHighlighting> highlightings = new ArrayList<>();
    private List<DefaultCoverage> coverages = new ArrayList<>();
    private List<DefaultCpdTokens> cpdTokens = new ArrayList<>();
    private List<DefaultSymbolTable> symbolTables = new ArrayList<>();
    private List<AnalysisError> analysisErrors = new ArrayList<>();
    private Map<String, String> properties = new HashMap<>();

    @Override
    public void store(final Measure measure) {
      measures.add(measure);
    }

    @Override
    public void store(final Issue issue) {
      issues.add(issue);
    }

    @Override
    public void store(ExternalIssue issue) {
      extIssues.add(issue);
    }

    @Override
    public void store(AdHocRule adHocRule) {
      adHocRules.add(adHocRule);
    }

    @Override
    public void store(NewHighlighting highlighting) {
      highlightings.add((DefaultHighlighting) highlighting);
    }

    @Override
    public void store(NewCoverage newCoverage) {
      coverages.add((DefaultCoverage) newCoverage);
    }

    @Override
    public void store(NewCpdTokens cpdToken) {
      cpdTokens.add((DefaultCpdTokens) cpdToken);
    }

    @Override
    public void store(NewSymbolTable symbolTable) {
      symbolTables.add((DefaultSymbolTable) symbolTable);
    }

    @Override
    public void store(final AnalysisError analysisError) {
      analysisErrors.add(analysisError);
    }

    @Override
    public void storeProperty(final String key, final String value) {
      properties.put(key, value);
    }

    @Override
    public void store(NewSignificantCode significantCode) {
    }

    public List<Measure> getMeasures() {
      return measures;
    }

    public List<Issue> getIssues() {
      return issues;
    }

    public List<DefaultHighlighting> getHighlightings() {
      return highlightings;
    }

    public List<DefaultCoverage> getCoverages() {
      return coverages;
    }

    public List<DefaultCpdTokens> getCpdTokens() {
      return cpdTokens;
    }

    public List<DefaultSymbolTable> getSymbolTables() {
      return symbolTables;
    }

    public List<AnalysisError> getAnalysisErrors() {
      return analysisErrors;
    }

    public Map<String, String> getProperties() {
      return properties;
    }

  }

}
