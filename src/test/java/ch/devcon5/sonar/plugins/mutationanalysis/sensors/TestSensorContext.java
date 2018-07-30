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

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import ch.devcon5.sonar.plugins.mutationanalysis.metrics.ResourceMutationMetrics;
import ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant;
import ch.devcon5.sonar.plugins.mutationanalysis.model.MutationOperator;
import ch.devcon5.sonar.plugins.mutationanalysis.model.MutationOperators;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.Metadata;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.DefaultActiveRules;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.SensorContext;
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
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.api.batch.sensor.symbol.internal.DefaultSymbolTable;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.Version;

/**
 *
 */
public class TestSensorContext implements SensorContext {

  private static final List<MutationOperator> MUTATION_OPERATORS = Collections.unmodifiableList(new ArrayList<>(MutationOperators.allMutagens()));
  private final DefaultFileSystem fs;
  private final InputModule inputModule;
  private MapSettings settings = new MapSettings();
  private Configuration configuration = mock(Configuration.class);
  private SonarRuntime runtime = mock(SonarRuntime.class);
  private Version version = Version.create(6, 5);
  private TestSensorStorage storage = new TestSensorStorage();
  private ActiveRules activeRules = new DefaultActiveRules(Collections.emptyList());

  private TestSensorContext(final Path basePath, String moduleName) {

    this.fs = new DefaultFileSystem(basePath).setWorkDir(basePath);

    final ProjectDefinition pd = ProjectDefinition.create().setName(moduleName).setKey(moduleName).setBaseDir(basePath.toFile()).setWorkDir(basePath.toFile());
    this.inputModule = new DefaultInputModule(pd);
  }

  public static TestSensorContext create(Path basePath, String moduleName) {

    return new TestSensorContext(basePath, moduleName);
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

    return new DefaultIssue(this.storage);
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

    return new DefaultCpdTokens(configuration, this.storage);
  }

  @Override
  public NewAnalysisError newAnalysisError() {

    return new DefaultAnalysisError();
  }

  @Override
  public void addContextProperty(final String key, final String value) {

    this.storage.storeProperty(key, value);
  }

  @Override
  public void markForPublishing(final InputFile inputFile) {

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

  public ResourceMutationMetrics newResourceMutationMetrics(String filename) {

    return newResourceMutationMetrics(filename, md -> {
    });
  }

  public ResourceMutationMetrics newResourceMutationMetrics(String filename, Consumer<TestFileMetadata> mdGenerator) {

    final TestFileMetadata metadata = new TestFileMetadata();
    mdGenerator.accept(metadata);
    try {
      ResourceMutationMetrics rmm = new ResourceMutationMetrics(addTestFile(filename, metadata));

      addMutants(rmm, Mutant.State.UNKNOWN, metadata.mutants.unknown, metadata.test.name);
      addMutants(rmm, Mutant.State.NO_COVERAGE, metadata.mutants.noCoverage, metadata.test.name);
      addMutants(rmm, Mutant.State.SURVIVED, metadata.mutants.survived, metadata.test.name);
      addMutants(rmm, Mutant.State.TIMED_OUT, metadata.mutants.timedOut, metadata.test.name);
      addMutants(rmm, Mutant.State.MEMORY_ERROR, metadata.mutants.memoryError, metadata.test.name);
      addMutants(rmm, Mutant.State.KILLED, metadata.mutants.killed, metadata.test.name);

      return rmm;
    } catch (IOException e) {
      throw new RuntimeException("Failed to create test file", e);
    }
  }

  private InputFile addTestFile(String filename, TestFileMetadata metadata) throws IOException {

    final Path basePath = this.fs.baseDirPath();
    final Path filePath = basePath.resolve(filename);
    Files.createFile(filePath);

    final DefaultInputFile file = createNewInputFile(filename, "java", metadata);
    this.fs.add(file);
    return file;
  }

  private DefaultInputFile createNewInputFile(final String filename, String language, TestFileMetadata md) {

    final DefaultIndexedFile indexedFile = new DefaultIndexedFile(this.inputModule.key(), this.fs.baseDirPath(), filename, language);
    final DefaultInputFile file = new DefaultInputFile(indexedFile, dif -> dif.setMetadata(md.toMetadata()));
    file.checkMetadata();

    return file;
  }

  private void addMutants(final ResourceMutationMetrics rmm, final Mutant.State state, final int count) {

    addMutants(rmm, state, count, "ATest");
  }

  private void addMutants(final ResourceMutationMetrics rmm, final Mutant.State state, final int count, String testname) {

    String filename = rmm.getResource().filename();

    for (int i = 0; i < count; i++) {
      rmm.addMutant(newMutant(filename, state, count + i, testname));
    }
  }

  private Mutant newMutant(String file, Mutant.State state, final int line, String testName) {

    String className = file.substring(0, file.lastIndexOf('.')).replaceAll("/|\\\\", ".");
    MutationOperator mutator = MUTATION_OPERATORS.get(line % MUTATION_OPERATORS.size());

    return new Mutant(!state.isAlive(), state, file, className, "aMethod", "decs", line, mutator, "", 0, state.isAlive() ? "" : testName);
  }

  public static class TestFileMetadata {

    public final MutantMetadata mutants = new MutantMetadata();
    public final TestMetadata test = new TestMetadata();
    public int lines = 1;
    public int nonBlankLines = 1;
    public String hash = "";
    public int lastValidOffset = 1;

    public Metadata toMetadata() {

      return new Metadata(lines, nonBlankLines, hash, IntStream.range(0, lines).map(i -> i * 80).toArray(), lastValidOffset);
    }

    public static class MutantMetadata implements Serializable {

      private static final long serialVersionUID = 7717508300632296958L;
      public int unknown = 0;
      public int noCoverage = 0;
      public int survived = 0;
      public int memoryError = 0;
      public int timedOut = 0;
      public int killed = 0;
    }

    public static class TestMetadata implements Serializable {

      private static final long serialVersionUID = 7691838474280645687L;
      public String name = "ATest";
    }
  }

  public static class TestSensorStorage implements SensorStorage {

    private List<Measure> measures = new ArrayList<>();
    private List<Issue> issues = new ArrayList<>();
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
    public void store(final DefaultHighlighting highlighting) {

      highlightings.add(highlighting);
    }

    @Override
    public void store(final DefaultCoverage defaultCoverage) {

      coverages.add(defaultCoverage);
    }

    @Override
    public void store(final DefaultCpdTokens defaultCpdTokens) {

      cpdTokens.add(defaultCpdTokens);
    }

    @Override
    public void store(final DefaultSymbolTable symbolTable) {

      symbolTables.add(symbolTable);
    }

    @Override
    public void store(final AnalysisError analysisError) {

      analysisErrors.add(analysisError);
    }

    @Override
    public void storeProperty(final String key, final String value) {

      properties.put(key, value);
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
