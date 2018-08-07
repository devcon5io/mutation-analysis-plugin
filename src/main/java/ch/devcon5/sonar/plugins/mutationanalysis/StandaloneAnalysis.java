package ch.devcon5.sonar.plugins.mutationanalysis;

import ch.devcon5.sonar.plugins.mutationanalysis.metrics.ResourceMutationMetrics;
import ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant;
import ch.devcon5.sonar.plugins.mutationanalysis.report.PitestReportParser;
import ch.devcon5.sonar.plugins.mutationanalysis.report.ReportFinder;
import ch.devcon5.sonar.plugins.mutationanalysis.standalone.Json;
import ch.devcon5.sonar.plugins.mutationanalysis.standalone.StandaloneResourceResolver;
import ch.devcon5.sonar.plugins.mutationanalysis.standalone.StandaloneRulesProcessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ch.devcon5.sonar.plugins.mutationanalysis.standalone.Json.arr;
import static ch.devcon5.sonar.plugins.mutationanalysis.standalone.Json.propObj;

public class StandaloneAnalysis {

    public static void main(String... args) throws IOException {

        Path baseDir = Paths.get(".");
        Path reportsPath = new ReportFinder().findReport(baseDir.resolve("target/pit-reports"));
        Collection<Mutant> mutants = new PitestReportParser().parseMutants(reportsPath);

        final Collection<ResourceMutationMetrics> metrics = collectMetrics(baseDir, mutants);

        StandaloneRulesProcessor proc = new StandaloneRulesProcessor();
        List<String> issues = proc.processRules(metrics, baseDir);

        Files.write(baseDir.resolve("issuesReport.json"), Json.obj(propObj("issues", arr(issues))).getBytes("UTF-8"));

        System.out.println(issues.size() + " issues written");
    }

    /**
     * Collect the metrics per resource (from the context) for the given mutants found on the project.
     *
     * @param mutants the mutants found in by PIT
     * @return
     */
    private static Collection<ResourceMutationMetrics> collectMetrics(Path baseDir, final Collection<Mutant> mutants) throws IOException {

        final StandaloneResourceResolver resolver = new StandaloneResourceResolver(baseDir);
        final Map<Path, ResourceMutationMetrics> metricsByResource = new HashMap<>();

        for (final Mutant mutant : mutants) {
            resolver.resolve(mutant.getMutatedClass())
                    .ifPresent(file -> metricsByResource.computeIfAbsent(file, ResourceMutationMetrics::new)
                                                        .addMutant(mutant));
        }
        return metricsByResource.values();
    }
}
