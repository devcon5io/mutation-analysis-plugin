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

import static ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin.REPORT_DIRECTORY_KEY;
import static ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin.PITEST_SENSOR_ENABLED;
import static ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition.REPOSITORY_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Optional;

import ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin;
import ch.devcon5.sonar.plugins.mutationanalysis.TestUtils;
import ch.devcon5.sonar.plugins.mutationanalysis.model.MutationOperators;
import ch.devcon5.sonar.plugins.mutationanalysis.report.PitestReportParser;
import ch.devcon5.sonar.plugins.mutationanalysis.report.ReportFinder;
import ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.mockito.verification.VerificationMode;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueLocation;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.Issuable;
import org.sonar.api.measures.Metric;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;

@RunWith(MockitoJUnitRunner.class)
public class PitestSensorTest {

    public static final int METRICS_COUNT = 13;

    @org.junit.Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @InjectMocks
    private PitestSensor subject;

    // constructor mocks
    @Mock
    private Configuration settings;
    @Mock
    private PitestReportParser parser;
    @Mock
    private RulesProfile rulesProfile;
    @Mock
    private ReportFinder reportFinder;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FileSystem fileSystem;

    // method arg mocks
    @Mock
    private SensorDescriptor descriptor;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SensorContext context;
    @Mock
    private Issuable issuable;
    @Mock
    private DefaultInputFile javaFile;


    private final ActiveRule negateConditionalsRule = createRuleMock("mutant.NEGATE_CONDITIONALS");
    private final ActiveRule survivedMutantRule = createRuleMock("mutant.survived");
    private final ActiveRule uncoveredMutantRule = createRuleMock("mutant.uncovered");
    private final ActiveRule unknownStatusRule = createRuleMock("mutant.unknownStatus");
    private final ActiveRule coverageThresholdRule = createRuleMock("mutant.coverage");

    private final NewMeasure<Serializable> measure = createMeasureMock();
    private final NewIssue issue = createIssueMock();

    @SuppressWarnings("unchecked")
    private NewMeasure<Serializable> createMeasureMock() {

        final NewMeasure<Serializable> measure = mock(NewMeasure.class);
        when(measure.on(any(InputComponent.class))).thenReturn(measure);
        when(measure.forMetric(any(Metric.class))).thenReturn(measure);
        when(measure.withValue(any(Serializable.class))).thenReturn(measure);
        return measure;
    }

    private NewIssue createIssueMock() {

        final NewIssue issue = mock(NewIssue.class);
        when(issue.forRule(any(RuleKey.class))).thenReturn(issue);
        when(issue.gap(anyDouble())).thenReturn(issue);
        when(issue.at(any(NewIssueLocation.class))).thenReturn(issue);
        return issue;
    }

    private ActiveRule createRuleMock(final String ruleName) {

        final ActiveRule activeRule = mock(ActiveRule.class);
        final Rule rule = mock(Rule.class);
        final RuleKey ruleKey = mock(RuleKey.class);
        when(activeRule.getRuleKey()).thenReturn(ruleName);
        when(activeRule.getRule()).thenReturn(rule);
        when(rule.getKey()).thenReturn(ruleName);
        when(rule.ruleKey()).thenReturn(ruleKey);
        when(ruleKey.rule()).thenReturn(ruleName);
        return activeRule;
    }

    @Before
    public void setUp() throws IOException {

        // setup filesystem and settings
        when(fileSystem.baseDir()).thenReturn(folder.getRoot());
        when(context.fileSystem()).thenReturn(fileSystem);
        when(javaFile.isFile()).thenReturn(true);
        when(javaFile.newRange(anyInt(), anyInt(), anyInt(), anyInt())).then(new IntegerRangeAnswer());
        when(javaFile.selectLine(anyInt())).then(new IntegerRangeAnswer());
        when(javaFile.type()).thenReturn(InputFile.Type.MAIN);
        when(settings.get(REPORT_DIRECTORY_KEY)).thenReturn(Optional.of("target/pit-reports"));
        when(settings.getBoolean(MutationAnalysisPlugin.EXPERIMENTAL_FEATURE_ENABLED)).thenReturn(Optional.of(true));

    }

    private String getResourceAsString(final String resource) throws IOException {
        try(InputStream is = PitestSensorTest.class.getResourceAsStream(resource)) {
            return IOUtils.toString(is);
        }
    }

    private void setupEnvironment(final boolean hasJavaFiles, final boolean sensorEnabled) {

        final FilePredicate hasJavaFilesPredicate = mock(FilePredicate.class);
        when(fileSystem.predicates().hasLanguage("java")).thenReturn(hasJavaFilesPredicate);
        when(fileSystem.hasFiles(hasJavaFilesPredicate)).thenReturn(hasJavaFiles);
        when(settings.getBoolean(PITEST_SENSOR_ENABLED)).thenReturn(Optional.of(sensorEnabled));
    }

    private void setupSensorTest(final ActiveRule... rules) throws IOException, FileNotFoundException {

        setupEnvironment(true, true);
        // create input file and filesystem
        TestUtils.tempFileFromResource(folder,
                                       "target/pit-reports/mutations.xml",
                                       getClass(),
                                       "PitestSensorTest_mutations.xml");
        when(fileSystem.inputFile(any(FilePredicate.class))).thenReturn(javaFile);
        // the rules repository
        when(rulesProfile.getActiveRulesByRepository(REPOSITORY_KEY)).thenReturn(Arrays.asList(rules));
        // context measure and issues
        when(context.newMeasure()).thenReturn(measure);
        when(context.newIssue()).thenReturn(issue);
    }

    private void setupSettings(final String settingsKey, final double value) {

        when(settings.getDouble(settingsKey)).thenReturn(Optional.of(Double.valueOf(value)));

    }

    @Test
    public void testDescribe() throws Exception {

        // prepare

        // act
        subject.describe(descriptor);

        // assert
        verify(descriptor).name(eq("Mutation Analysis"));
        verify(descriptor).onlyOnLanguages(eq("java"));
//        verify(descriptor).onlyOnFileType(eq(Type.MAIN));
        verify(descriptor).createIssuesForRuleRepositories(eq(REPOSITORY_KEY));

    }

    @Test
    public void testToString() throws Exception {

        assertEquals("PitestSensor", subject.toString());
    }

    @Test
    public void testExecute_noFilesAndSensorDisabled_noIssuesAndMeasures() throws Exception {

        // prepare
        setupEnvironment(false, false);

        // act
        subject.execute(context);

        // assert
        verify(context, times(0)).newIssue();
        verify(context, times(0)).newMeasure();

    }

    @Test
    public void testExecute_withFilesAndSensorDisabled_noIssuesAndMeasures() throws Exception {

        // prepare
        setupEnvironment(true, false);

        // act
        subject.execute(context);

        // assert
        verify(context, times(0)).newIssue();
        verify(context, times(0)).newMeasure();

    }

    @Test
    public void testExecute_withFilesAndSensorEnabled_noReport_noIssuesAndMeasure() throws Exception {

        // prepare
        setupEnvironment(true, true);
        when(rulesProfile.getActiveRulesByRepository("pitest.pro")).thenReturn(Arrays.asList(negateConditionalsRule));

        // act
        subject.execute(context);

        // assert
        verify(context, times(0)).newIssue();
        verify(context, times(0)).newMeasure();

    }

    @Test
    public void testExecute_withFilesAndSensorEnabledAndReportExist_noActiveRule_noIssues() throws Exception {

        // prepare
        setupEnvironment(true, true);
        setupSensorTest();

        // act
        subject.execute(context);

        // assert
        // verify issues have been create
        verify(context, times(0)).newIssue();

        verifyMeasures(METRICS_COUNT);
    }

    @Test
    public void testExecute_mutatorSpecificRuleActive() throws Exception {

        // prepare

        final double EXPECTED_EFFORT_TO_FIX = 1.0;
        final double FACTOR = 2.0;
        setupEnvironment(true, true);
        setupSensorTest(negateConditionalsRule);
        setupSettings(MutationAnalysisPlugin.EFFORT_FACTOR_SURVIVED_MUTANT, FACTOR);

        // act
        subject.execute(context);

        // assert
        // verify issues have been create
        verify(context, times(2)).newIssue();
        verifyRuleKey(issue, "mutant.NEGATE_CONDITIONALS", times(2));
        // verify the file and the lines of the mutants
        final ArgumentCaptor<NewIssueLocation> captor = forClass(NewIssueLocation.class);
        verify(issue, times(2)).at(captor.capture());
        final DefaultIssueLocation loc1 = (DefaultIssueLocation) captor.getAllValues().get(0);
        final DefaultIssueLocation loc2 = (DefaultIssueLocation) captor.getAllValues().get(1);
        assertEquals(172, loc1.textRange().start().line());
        assertEquals(172, loc1.textRange().end().line());
        assertEquals(175, loc2.textRange().start().line());
        assertEquals(175, loc2.textRange().end().line());
        // verify the violation description
        Assert.assertEquals(MutationOperators.find("NEGATE_CONDITIONALS").getViolationDescription(), loc1.message());
        assertEquals(MutationOperators.find("NEGATE_CONDITIONALS").getViolationDescription() + " (WITH_SUFFIX)", loc2.message());

        verify(issue, times(2)).forRule(any(RuleKey.class));
        verifyEffortToFix(issue, EXPECTED_EFFORT_TO_FIX * FACTOR, times(2));
        verify(issue, times(2)).save();

        verifyMeasures(METRICS_COUNT);
    }

    @Test
    public void testExecute_survivedMutantRuleActive() throws Exception {

        // prepare
        final double EXPECTED_EFFORT_TO_FIX = 1.0;
        final double FACTOR = 2.0;
        setupEnvironment(true, true);
        setupSensorTest(survivedMutantRule);
        setupSettings(MutationAnalysisPlugin.EFFORT_FACTOR_SURVIVED_MUTANT, FACTOR);

        // act
        subject.execute(context);

        // assert
        verify(context).newIssue();
        verifyRuleKey(issue, "mutant.survived");
        verifyEffortToFix(issue, EXPECTED_EFFORT_TO_FIX * FACTOR);
        verifyNewIssueLocation(javaFile, issue, 172);
        verify(issue).save();
        verifyMeasures(METRICS_COUNT);
    }

    @Test
    public void testExecute_uncoverdMutantRuleActive() throws Exception {

        // prepare
        final double EXPECTED_EFFORT_TO_FIX = 1.0;
        final double FACTOR = 2.0;
        setupEnvironment(true, true);
        setupSensorTest(uncoveredMutantRule);
        setupSettings(MutationAnalysisPlugin.EFFORT_FACTOR_SURVIVED_MUTANT, FACTOR);

        // act
        subject.execute(context);

        // assert
        // verify issues have been create
        verify(context).newIssue();
        verifyRuleKey(issue, "mutant.uncovered");
        verifyEffortToFix(issue, EXPECTED_EFFORT_TO_FIX * FACTOR);
        verifyNewIssueLocation(javaFile, issue, 175);
        verify(issue).save();
        verifyMeasures(METRICS_COUNT);
    }

    @Test
    public void testExecute_unknownMutantStatusRuleActive() throws Exception {

        // prepare
        final double EXPECTED_EFFORT_TO_FIX = 1.0;
        final double FACTOR = 2.0;
        setupEnvironment(true, true);
        setupSensorTest(unknownStatusRule);
        setupSettings(MutationAnalysisPlugin.EFFORT_FACTOR_SURVIVED_MUTANT, FACTOR);

        // act
        subject.execute(context);

        // assert
        verify(context).newIssue();
        verifyRuleKey(issue, "mutant.unknownStatus");
        verifyEffortToFix(issue, EXPECTED_EFFORT_TO_FIX * FACTOR);
        verifyNewIssueLocation(javaFile, issue, 175);
        verify(issue).save();
        verifyMeasures(METRICS_COUNT);
    }

    private void verifyNewIssueLocation(final DefaultInputFile javaFile, final NewIssue issue, final int lineNumber) {

        DefaultIssueLocation loc = verifyNewIssueLocation(javaFile, issue);
        assertNotNull(loc.textRange());
        assertEquals(lineNumber, loc.textRange().start().line());
        assertEquals(lineNumber, loc.textRange().end().line());
    }

    private DefaultIssueLocation verifyNewIssueLocation(final InputComponent component, final NewIssue issue) {

        final ArgumentCaptor<NewIssueLocation> captor = forClass(NewIssueLocation.class);
        verify(issue).at(captor.capture());
        final NewIssueLocation issueLocation = captor.getValue();
        assertTrue(issueLocation instanceof DefaultIssueLocation);
        final DefaultIssueLocation loc = (DefaultIssueLocation) issueLocation;
        assertNotNull(loc.message());
        assertNotNull(loc.inputComponent());
        assertEquals(component, loc.inputComponent());
        return loc;
    }

    @Test
    public void testExecute_coverageThresholdRuleActive_belowThreshold_oneMutantMissing() throws Exception {

        // prepare

        final double EXPECTED_EFFORT_TO_FIX = 1.8;
        final double FACTOR = 2.0;
        setupEnvironment(true, true);
        setupSensorTest(coverageThresholdRule);
        when(coverageThresholdRule.getParameter(MutationAnalysisRulesDefinition.PARAM_MUTANT_COVERAGE_THRESHOLD)).thenReturn("80.0");
        setupSettings(MutationAnalysisPlugin.EFFORT_FACTOR_MISSING_COVERAGE, FACTOR);

        // act
        subject.execute(context);

        // assert
        // verify issues have been create
        verify(context).newIssue();
        verifyRuleKey(issue, "mutant.coverage");
        verifyEffortToFix(issue, EXPECTED_EFFORT_TO_FIX * FACTOR);
        verifyNewIssueLocation(javaFile, issue);
        verify(issue).save();
        verifyMeasures(METRICS_COUNT);
    }

    @Test
    public void testExecute_coverageThresholdRuleActive_belowThreshold_moreMutantsMissing() throws Exception {

        // prepare
        final double EXPECTED_EFFORT_TO_FIX = 2.4;
        final double FACTOR = 2.0;
        setupSensorTest(coverageThresholdRule);
        when(coverageThresholdRule.getParameter(MutationAnalysisRulesDefinition.PARAM_MUTANT_COVERAGE_THRESHOLD)).thenReturn("90.0");
        setupSettings(MutationAnalysisPlugin.EFFORT_FACTOR_MISSING_COVERAGE, FACTOR);

        // act
        subject.execute(context);

        // assert
        // verify issues have been create
        verify(context).newIssue();
        verifyRuleKey(issue, "mutant.coverage");
        verifyEffortToFix(issue, EXPECTED_EFFORT_TO_FIX * FACTOR);
        verifyNewIssueLocation(javaFile, issue);
        verify(issue).save();
        verifyMeasures(METRICS_COUNT);
    }

    @Test
    public void testExecute_coverageThresholdRuleActive_aboveThreshold() throws Exception {

        // prepare
        setupEnvironment(true, true);
        setupSensorTest(coverageThresholdRule);
        when(coverageThresholdRule.getParameter(MutationAnalysisRulesDefinition.PARAM_MUTANT_COVERAGE_THRESHOLD)).thenReturn("20.0");

        // act
        subject.execute(context);

        // assert
        verify(context, times(0)).newIssue();
        verifyMeasures(METRICS_COUNT);
    }

    @Test
    public void testExecute_coverageThresholdRuleActive_onThreshold() throws Exception {

        // prepare
        setupEnvironment(true, true);
        setupSensorTest(coverageThresholdRule);
        when(coverageThresholdRule.getParameter(MutationAnalysisRulesDefinition.PARAM_MUTANT_COVERAGE_THRESHOLD)).thenReturn("50.0");

        // act
        subject.execute(context);

        // assert
        verify(context, times(0)).newIssue();
        verifyMeasures(METRICS_COUNT);
    }

    private void verifyRuleKey(final NewIssue issue, final String ruleKey) {

        final ArgumentCaptor<RuleKey> captor = forClass(RuleKey.class);
        verify(issue).forRule(captor.capture());
        assertEquals(ruleKey, captor.getValue().rule());
    }

    private void verifyRuleKey(final NewIssue issue, final String ruleKey, final VerificationMode mode) {

        final ArgumentCaptor<RuleKey> captor = forClass(RuleKey.class);
        verify(issue, mode).forRule(captor.capture());
        assertEquals(ruleKey, captor.getValue().rule());
    }

    private void verifyEffortToFix(final NewIssue issue, final double expectedEffortToFix) {

        final ArgumentCaptor<Double> effortCaptor = forClass(Double.class);
        verify(issue).gap(effortCaptor.capture());
        final Double actualEffort = effortCaptor.getValue();
        assertEquals(expectedEffortToFix, actualEffort, 0.01);

    }

    private void verifyEffortToFix(final NewIssue issue2,
                                   final double expectedEffortToFix,
                                   final VerificationMode times) {

        final ArgumentCaptor<Double> effortCaptor = forClass(Double.class);
        verify(issue, times).gap(effortCaptor.capture());
        final Double actualEffort = effortCaptor.getValue();
        assertEquals(expectedEffortToFix, actualEffort, 0.01);

    }

    private void verifyMeasures(int times) {

        //TODO replace without verification
        // verify measures have been recorded
        verify(context, times(times)).newMeasure();
        verify(measure, times(times)).on(javaFile);
        verify(measure, times(times)).withValue(any(Serializable.class));
        /*
        for (final Metric m : MutationMetrics.getQuantitativeMetrics()) {
            verify(measure).forMetric(m);
        }
        */
        verify(measure, times(times)).save();
    }

    /**
     * Answer that repsonds with a text range mock containing the line number and offsets passed to the method as 4 int
     * args.
     */
    private static class IntegerRangeAnswer implements Answer {

        @Override
        public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {

            TextRange range = mock(TextRange.class);
            TextPointer start = mock(TextPointer.class);
            TextPointer end = mock(TextPointer.class);
            when(range.start()).thenReturn(start);
            when(range.end()).thenReturn(end);
            when(start.line()).thenReturn((Integer) invocationOnMock.getArguments()[0]);
            if(invocationOnMock.getArguments().length > 1){
                when(start.lineOffset()).thenReturn((Integer) invocationOnMock.getArguments()[1]);
                when(end.line()).thenReturn((Integer) invocationOnMock.getArguments()[2]);
                when(start.lineOffset()).thenReturn((Integer) invocationOnMock.getArguments()[3]);
            } else {
                when(start.lineOffset()).thenReturn(0);
                when(end.line()).thenReturn((Integer) invocationOnMock.getArguments()[0]);
                when(start.lineOffset()).thenReturn(0);
            }
            return range;
        }
    }
}
