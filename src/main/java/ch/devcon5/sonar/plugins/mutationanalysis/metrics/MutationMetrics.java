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

package ch.devcon5.sonar.plugins.mutationanalysis.metrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.sonar.api.measures.Metric;

/**
 * Metrics for the sonar pitest plugin.
 */
public final class MutationMetrics {

    private MutationMetrics(){}
    
    private static final int DIRECTION_BETTER = 1;
    private static final int DIRECTION_WORST = -1;
    private static final int DIRECTION_NONE = 0;
        
    

    public static final String MUTATION_ANALYSIS_DOMAIN = "Mutation Analysis";

    public static final String MUTATIONS_DATA_KEY = "dc5_mutationAnalysis_mutations_data";
    public static final Metric MUTATIONS_DATA = new Metric.Builder(MUTATIONS_DATA_KEY, "Mutations Data", Metric.ValueType.DATA)
        .setDirection(DIRECTION_NONE)
        .setDomain(MUTATION_ANALYSIS_DOMAIN)
        .setQualitative(true)
        .setDescription("None")
        .setHidden(true)
        .create();

    public static final String MUTATIONS_TOTAL_KEY = "dc5_mutationAnalysis_mutations_total";
    public static final Metric MUTATIONS_TOTAL = new Metric.Builder(MUTATIONS_TOTAL_KEY, "Mutations: Total", Metric.ValueType.INT)
        .setDirection(DIRECTION_WORST)
        .setDomain(MUTATION_ANALYSIS_DOMAIN)
        .setDescription("Total number of mutations generated")
        .create();

    public static final String MUTATIONS_TOTAL_PERCENT_KEY = "dc5_mutationAnalysis_mutations_hotspots";
    public static final Metric MUTATIONS_TOTAL_PERCENT = new Metric.Builder(MUTATIONS_TOTAL_PERCENT_KEY, "Mutations: Total %", Metric.ValueType.PERCENT)
        .setDirection(DIRECTION_WORST)
        .setDomain(MUTATION_ANALYSIS_DOMAIN)
        .setQualitative(true)
        .setDescription("Percent of total mutations per class")
        .setBestValue(0.)
        .setWorstValue(100.)
        .setDecimalScale(1)
        .create();

    public static final String MUTATIONS_NO_COVERAGE_KEY = "dc5_mutationAnalysis_mutations_noCoverage";
    public static final Metric MUTATIONS_NO_COVERAGE = new Metric.Builder(MUTATIONS_NO_COVERAGE_KEY, "Alive: Not Covered", Metric.ValueType.INT)
        .setDirection(DIRECTION_WORST)
        .setDomain(MUTATION_ANALYSIS_DOMAIN)
        .setDescription("Number of mutations non covered by any test.")
        .create();

    public static final String MUTATIONS_DETECTED_KEY = "dc5_mutationAnalysis_mutations_detected";
    public static final Metric MUTATIONS_DETECTED=new Metric.Builder(MUTATIONS_DETECTED_KEY, "Killed: Total", Metric.ValueType.INT)
        .setDirection(DIRECTION_WORST)
        .setDomain(MUTATION_ANALYSIS_DOMAIN)
        .setDescription("The number of all mutations that are either <ul>" 
                            + "<li>killed by a test</li>" 
                            + "<li>killed by a timeout</li>"
                            + "<li>killed by a memory error</li>" 
                            + "</ul>")
        .create();

    public static final String MUTATIONS_ALIVE_KEY = "dc5_mutationAnalysis_mutations_alive";
    public static final Metric MUTATIONS_ALIVE=new Metric.Builder(MUTATIONS_ALIVE_KEY, "Alive: Total", Metric.ValueType.INT)
        .setDirection(DIRECTION_WORST)
        .setDomain(MUTATION_ANALYSIS_DOMAIN)
        .setDescription("Number of mutations still alive this includes covered and non-covered mutations.")
        .create();

    public static final String MUTATIONS_KILLED_KEY = "dc5_mutationAnalysis_mutations_killed";
    public static final Metric MUTATIONS_KILLED=new Metric.Builder(MUTATIONS_KILLED_KEY, "Killed: by Tests", Metric.ValueType.INT)
        .setDirection(DIRECTION_WORST)
        .setDomain(MUTATION_ANALYSIS_DOMAIN)
        .setDescription("Number of mutations killed by tests")
        .create();

    public static final String MUTATIONS_UNKNOWN_KEY = "dc5_mutationAnalysis_mutations_unknown";
    public static final Metric MUTATIONS_UNKNOWN=new Metric.Builder(MUTATIONS_UNKNOWN_KEY, "Unknown Status", Metric.ValueType.INT)
        .setDirection(DIRECTION_WORST)
        .setDomain(MUTATION_ANALYSIS_DOMAIN)
        .setDescription("Number of mutations with unknown status.")
        .create();

    public static final String MUTATIONS_COVERAGE_KEY = "dc5_mutationAnalysis_mutations_coverage";
    public static final Metric MUTATIONS_COVERAGE=new Metric.Builder(MUTATIONS_COVERAGE_KEY, "Mutation: Coverage", Metric.ValueType.PERCENT)
        .setDirection(DIRECTION_BETTER)
        .setDomain(MUTATION_ANALYSIS_DOMAIN)
        .setQualitative(true)
        .setDescription("Mutations coverage percentage")
        .setBestValue(100.)
        .setWorstValue(0.)
        .setDecimalScale(1)
        .create();
    
    public static final String MUTATIONS_DENSITY_KEY = "dc5_mutationAnalysis_mutations_density";
    public static final Metric MUTATIONS_DENSITY=new Metric.Builder(MUTATIONS_DENSITY_KEY, "Mutation: Density", Metric.ValueType.PERCENT)
        .setDirection(DIRECTION_WORST)
        .setDomain(MUTATION_ANALYSIS_DOMAIN)
        .setQualitative(true)
        .setDescription("Mutations per Statement")
        .setBestValue(100.)
        .setWorstValue(0.)
        .setDecimalScale(1)
        .create();

    public static final String MUTATIONS_ALIVE_PERCENT_KEY = "dc5_mutationAnalysis_mutations_survivor_hotspots";
    public static final Metric MUTATIONS_ALIVE_PERCENT=new Metric.Builder(MUTATIONS_ALIVE_PERCENT_KEY, "Alive: Total %", Metric.ValueType.PERCENT)
        .setDirection(DIRECTION_WORST)
        .setDomain(MUTATION_ANALYSIS_DOMAIN)
        .setQualitative(true)
        .setDescription("Distribution Percentage of all survivor")
        .setBestValue(0.)
        .setWorstValue(100.)
        .setDecimalScale(1)
        .create();

    public static final String MUTATIONS_TIMED_OUT_KEY = "dc5_mutationAnalysis_mutations_timedOut";
    public static final Metric MUTATIONS_TIMED_OUT=new Metric.Builder(MUTATIONS_TIMED_OUT_KEY, "Killed: by Timeout", Metric.ValueType.INT)
        .setDirection(DIRECTION_WORST)
        .setDomain(MUTATION_ANALYSIS_DOMAIN)
        .setDescription("Number of mutations detected by time outs.")
        .create();

    public static final String MUTATIONS_MEMORY_ERROR_KEY = "dc5_mutationAnalysis_mutations_memoryError";
    public static final Metric MUTATIONS_MEMORY_ERROR=new Metric.Builder(MUTATIONS_MEMORY_ERROR_KEY, "Killed: by Memory Error", Metric.ValueType.INT)
        .setDirection(DIRECTION_WORST)
        .setDomain(MUTATION_ANALYSIS_DOMAIN)
        .setDescription("Number of mutations detected by memory errors.")
        .create();

    public static final String MUTATIONS_SURVIVED_KEY = "dc5_mutationAnalysis_mutations_survived";
    public static final Metric MUTATIONS_SURVIVED=new Metric.Builder(MUTATIONS_SURVIVED_KEY, "Alive: Survivors", Metric.ValueType.INT)
        .setDirection(DIRECTION_WORST)
        .setDomain(MUTATION_ANALYSIS_DOMAIN)
        .setDescription("Number of mutations survived.")
        .create();

    public static final String TEST_KILLS_KEY = "dc5_mutationAnalysis_mutations_testkills";
    public static final Metric TEST_KILLS=new Metric.Builder(TEST_KILLS_KEY, "Test: Kills", Metric.ValueType.INT)
        .setDirection(DIRECTION_WORST)
        .setDomain(MUTATION_ANALYSIS_DOMAIN)
        .setDescription("Kills per Test")
        .create();

    public static final String TEST_KILL_RATIO_KEY = "dc5_mutationAnalysis_mutations_testkill_ratio";
    public static final Metric TEST_KILL_RATIO=new Metric.Builder(TEST_KILL_RATIO_KEY, "Test: Kill Ratio", Metric.ValueType.PERCENT)
        .setDirection(DIRECTION_WORST)
        .setDomain(MUTATION_ANALYSIS_DOMAIN)
        .setQualitative(true)
        .setDescription("Kills per Test")
        .setBestValue(100.)
        .setWorstValue(0.)
        .setDecimalScale(1)
        .create();

    public static final String UTILITY_GLOBAL_MUTATIONS_KEY = "dc5_mutationAnalysis_mutations_global";
    public static final Metric UTILITY_GLOBAL_MUTATIONS=new Metric.Builder(UTILITY_GLOBAL_MUTATIONS_KEY, "Utility: Total Mutations Global", Metric.ValueType.INT)
        .setDirection(DIRECTION_BETTER)
        .setDomain(MUTATION_ANALYSIS_DOMAIN)
        .setDescription("Utility measure for computation to keep track of total number of mutations on each resource")
        .setHidden(true)
        .create();

    public static final String UTILITY_GLOBAL_ALIVE_KEY = "dc5_mutationAnalysis_survivors_global";
    public static final Metric UTILITY_GLOBAL_ALIVE=new Metric.Builder(UTILITY_GLOBAL_ALIVE_KEY, "Utility: Total Survivors Global", Metric.ValueType.INT)
        .setDirection(DIRECTION_BETTER)
        .setDomain(MUTATION_ANALYSIS_DOMAIN)
        .setDescription("Utility measure for computation")
        .setHidden(true)
        .create();

    private static final List<Metric> QUANTITATIVE_METRICS = Collections.unmodifiableList(Arrays.asList(
        MUTATIONS_TOTAL,
        MUTATIONS_NO_COVERAGE,
        MUTATIONS_DETECTED,
        MUTATIONS_ALIVE,
        MUTATIONS_KILLED,
        MUTATIONS_UNKNOWN,
        MUTATIONS_TIMED_OUT,
        MUTATIONS_MEMORY_ERROR,
        MUTATIONS_SURVIVED,
        TEST_KILLS,
        UTILITY_GLOBAL_MUTATIONS,
        UTILITY_GLOBAL_ALIVE
    ));

    private static final List<Metric> QUALITATIVE_METRICS = Collections.unmodifiableList(Arrays.asList(
        MUTATIONS_DATA,
        MUTATIONS_TOTAL_PERCENT,
        MUTATIONS_COVERAGE,
        MUTATIONS_DENSITY,
        MUTATIONS_ALIVE_PERCENT,
        TEST_KILL_RATIO
    ));

    private static final List<Metric> SENSOR_METRICS;
    
    static {
        final List<Metric> metrics  = new ArrayList<>(QUALITATIVE_METRICS.size() + QUANTITATIVE_METRICS.size());
        metrics.addAll(QUALITATIVE_METRICS);
        metrics.addAll(QUANTITATIVE_METRICS);
        SENSOR_METRICS = Collections.unmodifiableList(metrics);
    }

    /**
     * Returns the pitest quantitative metrics list.
     *
     * @return The pitest quantitative metrics list.
     */
    public static List<Metric> getQuantitativeMetrics() {

        return QUANTITATIVE_METRICS;
    }

    /**
     * Returns the all metrics the pitest sensor provides.
     *
     * @return The pitest sensor metrics list.
     */
    public static List<Metric> getSensorMetrics() {

       return SENSOR_METRICS;
    }
}
