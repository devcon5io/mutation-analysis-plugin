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

import static ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition.REPOSITORY_KEY;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin;
import ch.devcon5.sonar.plugins.mutationanalysis.metrics.ResourceMutationMetrics;
import ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;

/**
 * Sonar sensor for pitest mutation coverage analysis.
 * The pitest sensor supports Java and Kotlin as languages, both can be enabled/disabled separately and are
 */
public class PitestSensor implements Sensor {

  /**
   * SLF4J Logger for this class
   */
  private static final Logger LOG = LoggerFactory.getLogger(PitestSensor.class);

  /**
   * the Settings for the Pitest Sonar plugin
   */
  private final Configuration settings;
  private final ResourceResolver resourceResolver;
  private final RulesProcessor rulesProcessor;
  private final ReportCollector reportCollector;
  private final SourceMetricsWriter sourceMetricsWriter;
  private final TestMetricsWriter testMetricsWriter;

  /**
   * Constructor that is invoked by Sonar to create the sensor instance.
   *
   * @param configuration
   *     the Configuration for the Pitest Sonar plugin
   * @param rulesProfile
   *     the active rules profile containing all the active rules.
   * @param fileSystem
   *     the FileSystem reference to access the project resources
   */
  public PitestSensor(final Configuration configuration, final ActiveRules rulesProfile, final FileSystem
      fileSystem) {

    this.resourceResolver = new ResourceResolver(fileSystem);
    this.settings = configuration;
    this.rulesProcessor = new RulesProcessor(configuration, rulesProfile);
    this.reportCollector = new ReportCollector(configuration, fileSystem);
    this.sourceMetricsWriter = new SourceMetricsWriter();
    this.testMetricsWriter = new TestMetricsWriter(fileSystem);
  }

  @Override
  public void describe(final SensorDescriptor descriptor) {

    descriptor.name("Mutation Analysis");

    descriptor.onlyOnLanguages(toArray("java", "kotlin"));
    descriptor.createIssuesForRuleRepositories(toArray(REPOSITORY_KEY + ".java", REPOSITORY_KEY + ".kotlin"));

  }

  /**
   * Helper method to produce an array out of two items without creating unkillable mutations.
   *
   *  a sane implementation would simply call a varargs method such as descriptor.onlyOnLanguages("java", "kotlin")
   *
   *  unfortunately the varargs produce un-killable mutations which was tried to avoid for the plugin
   *  the list in combination with the stream to produce an array has no unkillable mutations
   *  the list has no varargs (unlike Arrays.asList())
   *  the stream to array requires no size argument that can be mutated
   */
  private String[] toArray(String element1, String element2) {
    final List<String> list = new ArrayList<>();
    list.add(element1);
    list.add(element2);
    return list.stream().toArray(String[]::new);
  }

  private List<String> getLanguageKeys(){

    final List<String> keys = new ArrayList<>();
    if( settings.getBoolean(MutationAnalysisPlugin.PITEST_JAVA_SENSOR_ENABLED).orElse(true)){
      keys.add("java");
    }
    if( settings.getBoolean(MutationAnalysisPlugin.PITEST_KOTLIN_SENSOR_ENABLED).orElse(true)){
      keys.add("kotlin");
    }
    LOG.debug("Enabled Languages for Pitest: {}", keys);

    return keys;
  }


  @Override
  public void execute(final SensorContext context) {

    if (isEnabled()) {
      LOG.info("Pitest Sensor {} running on {} in {}", getLanguageKeys(), context.project(), context.fileSystem().baseDir());
    } else {
      LOG.info("Pitest Sensor {} disabled", getLanguageKeys());
      return;
    }

    try {
      LOG.debug("Reading mutants");
      final Collection<Mutant> globalMutants = this.reportCollector.collectGlobalMutants(context);
      final Collection<Mutant> localMutants = this.reportCollector.collectLocalMutants();

      LOG.debug("collecting metrics");
      final Collection<ResourceMutationMetrics> metrics = collectMetrics(localMutants);

      getLanguageKeys().forEach(language -> {
        LOG.debug("applying {} rules", language);
        this.rulesProcessor.processRules(metrics, context, language);

      });

      LOG.debug("saving metrics");
      this.sourceMetricsWriter.writeMetrics(metrics, context, globalMutants);

      if (MutationAnalysisPlugin.isExperimentalFeaturesEnabled(this.settings)) {
        LOG.debug("calculating test metrics");
        this.testMetricsWriter.writeMetrics(metrics, context, globalMutants);
      } else {
        LOG.debug("Experimental features disabled");
      }
      LOG.debug("Done");
    } catch (final IOException e) {
      LOG.error("Could not read mutants", e);
    }

  }

  private boolean isEnabled() {

    return !getLanguageKeys().isEmpty();
  }

  /**
   * Collect the metrics per resource (from the context) for the given mutants found on the project.
   *
   * @param mutants
   *     the mutants found in by PIT
   *
   * @return
   */
  private Collection<ResourceMutationMetrics> collectMetrics(final Collection<Mutant> mutants) {

    final Map<InputFile, ResourceMutationMetrics> metricsByResource = new HashMap<>();

    for (final Mutant mutant : mutants) {

      this.resourceResolver.resolve(mutant.getMutatedClass())
                           .ifPresent(file -> metricsByResource.computeIfAbsent(file, ResourceMutationMetrics::new).addMutant(mutant));

    }
    return metricsByResource.values();
  }

  @Override
  public String toString() {

    return getClass().getSimpleName();
  }

}
