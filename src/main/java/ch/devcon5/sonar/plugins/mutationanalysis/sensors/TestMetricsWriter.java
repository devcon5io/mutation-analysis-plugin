package ch.devcon5.sonar.plugins.mutationanalysis.sensors;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics;
import ch.devcon5.sonar.plugins.mutationanalysis.metrics.ResourceMutationMetrics;
import ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant;
import ch.devcon5.sonar.plugins.mutationanalysis.model.TestDescriptor;
import org.slf4j.Logger;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.sensor.SensorContext;

/**
 *
 */
public class TestMetricsWriter {

  private static final Logger LOG = getLogger(TestMetricsWriter.class);
  private final ResourceResolver resourceResolver;

  public TestMetricsWriter(final FileSystem fileSystem) {

    this.resourceResolver = new ResourceResolver(fileSystem);

  }

  public void writeMetrics(final Collection<ResourceMutationMetrics> metrics, final SensorContext context, final Collection<Mutant> globalMutants) {

    Map<TestDescriptor, List<Mutant>> testKills = metrics.stream()
                                                         .flatMap(rmm -> rmm.getMutants().stream())
                                                         .filter(m -> isNotBlank(m.getKillingTest()))
                                                         .collect(Collectors.groupingBy(m -> new TestDescriptor(m.getKillingTest()), Collectors.toList()));

    final int total = globalMutants.isEmpty() ? sumTotal(metrics) : globalMutants.size();

    testKills.forEach((t, m) -> {
      LOG.debug("Test {} kills {} mutants ", t.getClassName(), m.size());
      this.resourceResolver.resolve(t.getClassName()).ifPresent(f -> {
        context.newMeasure().forMetric(MutationMetrics.TEST_KILLS).on(f).withValue(m.size()).save();
        context.newMeasure().forMetric(MutationMetrics.UTILITY_GLOBAL_MUTATIONS).on(f).withValue(total).save();
      });
    });
  }

  private boolean isNotBlank(final String t) {

    return t != null && !t.trim().isEmpty();
  }

  private int sumTotal(final Collection<ResourceMutationMetrics> metrics) {

    return (int) metrics.stream().mapToLong(ResourceMutationMetrics::getMutationsTotal).sum();
  }
}
