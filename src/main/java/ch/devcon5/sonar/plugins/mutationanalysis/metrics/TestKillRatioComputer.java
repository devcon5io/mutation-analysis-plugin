package ch.devcon5.sonar.plugins.mutationanalysis.metrics;

import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.TEST_KILLS_KEY;
import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.TEST_KILL_RATIO;
import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.TEST_KILL_RATIO_KEY;
import static ch.devcon5.sonar.plugins.mutationanalysis.metrics.MutationMetrics.UTILITY_GLOBAL_MUTATIONS_KEY;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.stream.StreamSupport;

import ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin;
import org.slf4j.Logger;
import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.config.Configuration;

public class TestKillRatioComputer implements MeasureComputer {

  private static final Logger LOG = getLogger(TestKillRatioComputer.class);

  private Configuration config;

  public TestKillRatioComputer(final Configuration config) {

    this.config = config;
  }

  @Override
  public MeasureComputerDefinition define(final MeasureComputerDefinitionContext defContext) {

    return defContext.newDefinitionBuilder().setInputMetrics(UTILITY_GLOBAL_MUTATIONS_KEY, TEST_KILLS_KEY).setOutputMetrics(TEST_KILL_RATIO_KEY).build();
  }

  @Override
  public void compute(final MeasureComputerContext context) {

    if (!MutationAnalysisPlugin.isExperimentalFeaturesEnabled(config)) {
      LOG.info("ExperimentalFeature disabled");
      return;
    }

    final Component comp = context.getComponent();
    if (comp.getType() == Component.Type.FILE && !comp.getFileAttributes().isUnitTest()) {
      LOG.info("Skipping {} because it's not test resource", comp);
      return;
    }

    final double mutationsGlobal = getMutationsGlobal(context, UTILITY_GLOBAL_MUTATIONS_KEY);
    final double testKillsLocal = getTestKillsLocal(context);

    if (mutationsGlobal > 0.0) {
      final double percentage = 100.0 * testKillsLocal / mutationsGlobal;
      LOG.info("Computed {} of {}% from ({} / {}) for {}", TEST_KILL_RATIO.getName(), percentage, testKillsLocal, mutationsGlobal, comp);
      context.addMeasure(TEST_KILL_RATIO_KEY, percentage);
    }
  }

  private double getMutationsGlobal(final MeasureComputerContext context, String key) {

    final double mutationsGlobal;
    final Measure globalMutationsMeasure = context.getMeasure(key);

    if (globalMutationsMeasure == null) {
      mutationsGlobal = StreamSupport.stream(context.getChildrenMeasures(key).spliterator(), false).mapToInt(Measure::getIntValue).findFirst().orElse(0);
      LOG.info("Component {} has no global mutation information, using first child's: {}", context.getComponent(), mutationsGlobal);
    } else {
      mutationsGlobal = globalMutationsMeasure.getIntValue();
      LOG.info("Using global mutation count: {}", mutationsGlobal);
    }
    return mutationsGlobal;
  }

  private double getTestKillsLocal(final MeasureComputerContext context) {

    final Measure localTestKills = context.getMeasure(TEST_KILLS_KEY);

    if (localTestKills == null) {
      return StreamSupport.stream(context.getChildrenMeasures(TEST_KILLS_KEY).spliterator(), false).mapToInt(Measure::getIntValue).sum();
    } else {
      return localTestKills.getIntValue();
    }
  }

}
