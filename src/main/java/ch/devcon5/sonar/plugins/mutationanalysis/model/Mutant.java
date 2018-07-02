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

package ch.devcon5.sonar.plugins.mutationanalysis.model;

import static java.util.Objects.requireNonNull;

/**
 * Pojo representing a Mutant. The structure maps to the PIT output: <br>
 * <pre>
 *  &lt;mutation detected='true' status='KILLED'&gt;
 *      &lt;sourceFile&gt;ResourceInjection.java&lt;/sourceFile&gt;
 *      &lt;mutatedClass&gt;io.inkstand.scribble.inject.ResourceInjection$ResourceLiteral&lt;/mutatedClass&gt;
 *      &lt;mutatedMethod&gt;authenticationType&lt;/mutatedMethod&gt;
 *      &lt;methodDescription&gt;()Ljavax/annotation/Resource$AuthenticationType;&lt;/methodDescription&gt;
 *      &lt;lineNumber&gt;164&lt;/lineNumber&gt;
 *      &lt;mutationOperator&gt;org.pitest.mutationtest.engine.gregor.mutators.ReturnValsMutator&lt;/mutationOperator&gt;
 *      &lt;index&gt;5&lt;/index&gt;
 *      &lt;killingTest&gt;io.inkstand.scribble.inject.ResourceInjectionTest.testByMappedName_match(io.inkstand.scribble.inject.ResourceInjectionTest)&lt;/killingTest&gt;
 * &lt;/mutation&gt;
 * </pre>
 */
public class Mutant {

    private final boolean detected;
    private final int lineNumber;
    private final int index;
    private final State state;
    private final MutationOperator mutationOperator;
    private final String sourceFile;
    private final String mutatedClass;
    private final String mutatedMethod;
    private final String methodDescription;
    private final String killingTest;
    private final String mutatorSuffix;
    private final int hashCode;
    private final String toString;
    private final TestDescriptor testDescriptor;

    /**
     * Creates a new Mutant pojo. The constructor is not intended to be invoked directly, though it's possible. The
     * create a Mutant, the {@link MutantBuilder} should be used.
     *
     * @param detected
     *         flag to indicate if the mutant was detected by a test or not
     * @param state
     *         the {@link Mutant.State} of the mutant. Only killed mutants are
     *         good mutants.
     * @param sourceFile
     *         the path to the sourceFile that contains the mutant. The sourceFile is relative to the project path.
     * @param mutatedClass
     *         the fully qualified class name containing the mutant
     * @param mutatedMethod
     *         the name of the method containing the mutant
     * @param methodDescription
     *         the description of the method that specifies its signature.
     *         {@see http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.3}
     * @param lineNumber
     *         the line number where the mutant was found
     * @param mutationOperator
     *         the mutationOperator that was used to create the mutant
     * @param mutatorSuffix
     *         the suffix for the mutationOperator. Some mutators like the RemoveConditionalMutator have variants that are
     *         indicated by a suffix. If no suffix was specified this parameter has to be passes as empty string.
     *         <code>null</code> is not allowed
     * @param index
     *         the index of the mutationOperator. It has no relevance to the sonar results
     * @param killingTest
     *         the fully qualified name of the test including the test method that killed the test. If the mutant was
     *         not killed, this has to be an empty string, <code>null</code> is not allowed.
     */
    public Mutant(final boolean detected,
                  final State state,
                  final String sourceFile,
                  final String mutatedClass,
                  final String mutatedMethod,
                  final String methodDescription,
                  final int lineNumber,
                  final MutationOperator mutationOperator,
                  final String mutatorSuffix,
                  final int index,
                  final String killingTest) { // NOSONAR
        requireNonNull(state, "state must not be null");
        requireNonNull(sourceFile, "sourceFile must not be null");
        requireNonNull(mutatedClass, "mutatedClass must not be null");
        requireNonNull(mutatedMethod, "mutatedMethod must not be null");
        requireNonNull(methodDescription, "methodDescription must not be null");
        requireNonNull(mutationOperator, "mutationOperator must not be null");
        requireNonNull(mutatorSuffix, "mutatorSuffix must not be null");
        requireNonNull(killingTest, "killingTest must not be null");
        this.detected = detected;
        this.state = state;
        this.sourceFile = sourceFile;
        this.mutatedClass = mutatedClass;
        this.mutatedMethod = mutatedMethod;
        this.methodDescription = methodDescription;
        this.lineNumber = lineNumber;
        this.mutationOperator = mutationOperator;
        this.mutatorSuffix = mutatorSuffix;
        this.index = index;
        this.killingTest = killingTest;
        this.toString = "Mutant [sourceFile="
                + sourceFile
                + ", mutatedClass="
                + mutatedClass
                + ", mutatedMethod="
                + mutatedMethod
                + ", methodDescription="
                + methodDescription
                + ", lineNumber="
                + lineNumber
                + ", state="
                + state
                + ", mutationOperator="
                + mutationOperator.getName()
                + ", killingTest="
                + killingTest
                + "]";
        this.hashCode = calculateHashCode(1,
                                          index,
                                          detected ? 1231 : 1237,
                                          lineNumber,
                                          methodDescription.hashCode(),
                                          state.hashCode(),
                                          mutatedClass.hashCode(),
                                          mutatedMethod.hashCode(),
                                          mutationOperator.hashCode(),
                                          mutatorSuffix.hashCode(),
                                          sourceFile.hashCode(),
                                          killingTest.hashCode());

        this.testDescriptor = new TestDescriptor(this.killingTest);
    }

    /**
     * @return flag to indicate if the mutant was detected by a test or not
     */
    public boolean isDetected() {

        return detected;
    }

    /**
     * @return the {@link Mutant.State} of the mutant. Only killed mutants are
     * good mutants.
     */
    public State getState() {

        return state;
    }

    /**
     * @return the path to the sourceFile that contains the mutant. The sourceFile is relative to the project path.
     */
    public String getSourceFile() {

        return sourceFile;
    }

    /**
     * @return the fully qualified class name containing the mutant
     */
    public String getMutatedClass() {

        return mutatedClass;
    }

    /**
     * @return the name of the method containing the mutant
     */
    public String getMutatedMethod() {

        return mutatedMethod;
    }

    /**
     * @return the description of the method that specifies its signature. {@see http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.3}
     */
    public String getMethodDescription() {

        return methodDescription;
    }

    /**
     * @return the line number where the mutant was found
     */
    public int getLineNumber() {

        return lineNumber;
    }

    /**
     * @return the mutationOperator that was used to create the mutant
     */
    public MutationOperator getMutationOperator() {

        return mutationOperator;
    }

    /**
     * @return the suffix for the mutationOperator. Some mutators like the RemoveConditionalMutator have variants that are
     * indicated by a suffix. If no suffix was specified this parameter has to be passes as empty string.
     * <code>null</code> is not allowed
     */
    public String getMutatorSuffix() {

        return mutatorSuffix;
    }

    /**
     * @return the index of the mutationOperator. It has no relevance to the sonar results
     */
    public int getIndex() {

        return index;
    }

    /**
     * @return the fully qualified name of the test including the test method that killed the test. If the mutant was
     * not killed, this has to be an empty string, <code>null</code> is not allowed.
     */
    public String getKillingTest() {

        return killingTest;
    }

    public TestDescriptor getTestDescriptor() {
        return this.testDescriptor;
    }


    /**
     * As the source file in the mutant reports is without a package path, the method determines the path to the source
     * file from the fully qualified name of the mutated class.
     *
     * @return returns the full path to the source file including the name of file itself. The path is relative to the
     * source folder.
     */
    @Deprecated
    public String getPathToSourceFile() {

        final int packageSeparatorPos = mutatedClass.lastIndexOf('.');
        final String packagePath = mutatedClass.substring(0, packageSeparatorPos).replaceAll("\\.", "/");

        return new StringBuilder(packagePath).append('/').append(sourceFile).toString();
    }

    @Override
    public int hashCode() {

        return this.hashCode;
    }

    private int calculateHashCode(final int initial, final int... values) {

        final int prime = 31;
        int result = initial;
        for (final int value : values) {
            result = prime * result + value;
        }
        return result;
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return equalsMutant((Mutant) obj);
    }

    private boolean equalsMutant(final Mutant other) { // NOSONAR

        if (detected != other.detected) {
            return false;
        }
        if (index != other.index) {
            return false;
        }
        if (lineNumber != other.lineNumber) {
            return false;
        }
        if (!methodDescription.equals(other.methodDescription)) {
            return false;
        }
        if (state != other.state) {
            return false;
        }
        if (!mutatedClass.equals(other.mutatedClass)) {
            return false;
        }
        if (!mutatedMethod.equals(other.mutatedMethod)) {
            return false;
        }
        if (!mutationOperator.equals(other.mutationOperator)) {
            return false;
        }
        if (!mutatorSuffix.equals(other.mutatorSuffix)) {
            return false;
        }
        if (!sourceFile.equals(other.sourceFile)) {
            return false;
        }
        if (!killingTest.equals(other.killingTest)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {

        return toString;
    }

    /**
     * Enumeration of status a {@link Mutant} may have.
     */
    public enum State {
        /**
         * The mutant was not covered by a test (Lurker)
         */
        NO_COVERAGE(true),
        /**
         * The mutant was killed by a test
         */
        KILLED(false),
        /**
         * The mutant was covered but not killed by a test (Survivor)
         */
        SURVIVED(true),
        /**
         * The mutant was killed by a memory error during mutation analysis (i.e. memory leak caused by the mutant)
         */
        MEMORY_ERROR(false),
        /**
         * The mutant was killed by a time-out during the mutation analysis (i.e. endless loop caused by the mutant)
         */
        TIMED_OUT(false),
        /**
         * The status of the mutant is unknown.
         */
        UNKNOWN(true);

        private boolean alive;

        State(final boolean alive) {

            this.alive = alive;
        }

        /**
         * Indicates whether the status represents an alive or a killed mutant
         *
         * @return <code>true</code> if the {@link Mutant} is still alive.
         */
        public boolean isAlive() {

            return alive;
        }

        /**
         * Parses the String to a State.
         *
         * @param stateName
         *         the String representation of the state.
         *
         * @return If the statusName is <code>null</code> or does not represent a valid mutant, UNKNOWN is returned,
         * otherwise the matching state.
         */
        public static State parse(final String stateName) {

            if (stateName == null) {
                return UNKNOWN;
            }
            for (final State state : State.values()) {
                if (state.name().equals(stateName)) {
                    return state;
                }
            }
            return UNKNOWN;
        }
    }

    /**
     * Creates a new build to define a mutant. As the {@link Mutant} class is designed as being immutable, the builder
     * allows sequential definition of the {@link Mutant}'s properties instead of passing all at once to the
     * constructor.
     *
     * @return a {@link MutantBuilder} for creating a {@link Mutant}
     */
    public static MutantBuilder newMutant() {

        return new MutantBuilder();
    }
}
