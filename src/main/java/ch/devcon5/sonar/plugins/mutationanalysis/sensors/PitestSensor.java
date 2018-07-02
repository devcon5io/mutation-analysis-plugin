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

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin;
import ch.devcon5.sonar.plugins.mutationanalysis.metrics.ResourceMutationMetrics;
import ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant;
import ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.profiles.RulesProfile;

/**
 * Sonar sensor for pitest mutation coverage analysis.
 */
public class PitestSensor implements Sensor {

  /**
   * SLF4J Logger for this class
   */
  private static final Logger LOG = LoggerFactory.getLogger(PitestSensor.class);

  /**
   * the FileSystem reference to access the project resources
   */
  private final FileSystem fileSystem;
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
  public PitestSensor(final Configuration configuration, final RulesProfile rulesProfile, final FileSystem fileSystem) {

    this.fileSystem = fileSystem;
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
    descriptor.onlyOnLanguages("java");
    descriptor.createIssuesForRuleRepositories(MutationAnalysisRulesDefinition.REPOSITORY_KEY);

  }

  @Override
  public void execute(final SensorContext context) {

    if (!(fileSystem.hasFiles(fileSystem.predicates().hasLanguage("java")) && settings.getBoolean(MutationAnalysisPlugin.PITEST_SENSOR_ENABLED).orElse(true))) {
      LOG.info("PIT Sensor disabled");
      return;
    } else {
      LOG.info("PIT Sensor running on {} in {}", context.module(), context.fileSystem().baseDir());
    }

    try {
      LOG.debug("Reading mutants");
      final Collection<Mutant> globalMutants = this.reportCollector.collectGlobalMutants(context);
      final Collection<Mutant> localMutants = this.reportCollector.collectLocalMutants();

      LOG.debug("collecting metrics");
      final Collection<ResourceMutationMetrics> metrics = collectMetrics(localMutants);

      LOG.debug("applying rules");
      this.rulesProcessor.processRules(metrics, context);

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
