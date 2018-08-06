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
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Optional;

import org.slf4j.Logger;

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
  private String description;

  /**
   * Creates a new Mutant using the specified builder. This constructor is invoked by the builder.
   * All properties of the build must be non-null.
   *
   * @param builder the builder containing the parameters for creating the mutant. The parameters have
   *                to be non-null for the construction to succeed.
   */
  private Mutant(final Builder builder) {

    this.detected = builder.detected;
    this.state = builder.state;
    this.sourceFile = builder.sourceFile;
    this.mutatedClass = builder.mutatedClass;
    this.mutatedMethod = builder.mutatedMethod;
    this.methodDescription = builder.methodDescription;
    this.lineNumber = builder.lineNumber;
    this.mutationOperator = builder.mutationOperator;
    this.mutatorSuffix = builder.mutatorSuffix;
    this.index = builder.index;
    this.killingTest = builder.detected ? builder.killingTest : "";
    this.description = builder.description;
    this.toString = "Mutant [sourceFile="
        + builder.sourceFile
        + ", mutatedClass="
        + builder.mutatedClass
        + ", mutatedMethod="
        + builder.mutatedMethod
        + ", methodDescription="
        + builder.methodDescription
        + ", lineNumber="
        + builder.lineNumber
        + ", state="
        + builder.state
        + ", mutationOperator="
        + builder.mutationOperator.getName()
        + ", killingTest="
        + this.killingTest
        + (this.description == null ? "" : ", description=" + this.description)
        + "]";
    this.hashCode = calculateHashCode(this.index,
                                      this.detected ? 1231 : 1237,
                                      this.lineNumber,
                                      this.methodDescription.hashCode(),
                                      this.state.hashCode(),
                                      this.mutatedClass.hashCode(),
                                      this.mutatedMethod.hashCode(),
                                      this.mutationOperator.hashCode(),
                                      this.mutatorSuffix.hashCode(),
                                      this.sourceFile.hashCode(),
                                      this.killingTest.hashCode(),
                                      this.description == null ? 0 : this.description.hashCode());

    this.testDescriptor = new TestDescriptor(this.killingTest);

  }

  /**
   * Creates a new build to define a mutant. As the {@link Mutant} class is designed as being immutable, the builder
   * allows sequential definition of the {@link Mutant}'s properties instead of passing all at once to the
   * constructor.
   *
   * @return a {@link ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant.Builder} for creating a {@link Mutant}
   */
  public static Builder builder() {

    return new Builder();
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

  /**
   * Newer versions of Pit produce a description containing more details about what has been mutated.
   * @return
   *  the description if the mutant contained any or an empty optional
   */
  public Optional<String> getDescription() {

    return Optional.ofNullable(this.description);
  }

  public TestDescriptor getTestDescriptor() {

    return this.testDescriptor;
  }

  @Override
  public int hashCode() {

    return this.hashCode;
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

  @Override
  public String toString() {

    return toString;
  }

  private int calculateHashCode(final int... values) {

    final int prime = 31;
    int result = 1;
    for (final int value : values) {
      result = prime * result + value;
    }
    return result;
  }

  private boolean equalsMutant(final Mutant other) { // NOSONAR

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
    return getDescription().equals(other.getDescription());
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
     * Parses the String to a State.
     *
     * @param stateName
     *     the String representation of the state.
     *
     * @return If the statusName is <code>null</code> or does not represent a valid mutant, UNKNOWN is returned,
     * otherwise the matching state.
     */
    public static State parse(final String stateName) {

      for (final State state : State.values()) {
        if (state.name().equals(stateName)) {
          return state;
        }
      }
      return UNKNOWN;
    }

    /**
     * Indicates whether the status represents an alive or a killed mutant
     *
     * @return <code>true</code> if the {@link Mutant} is still alive.
     */
    public boolean isAlive() {

      return alive;
    }
  }

  /**
   * A builder for creating a new mutant. The builder allows a sequential setting of the mutant parameters while the
   * {@link ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant} class itself is for immutable instances and therefore needs all parameters at construction time.
   */
  public static class Builder {

    private static final Logger LOGGER = getLogger(Builder.class);

    private boolean detected;
    private State state;
    private String sourceFile;
    private String mutatedClass;
    private String mutatedMethod;
    private String methodDescription;
    private int lineNumber;
    private MutationOperator mutationOperator;
    private String mutatorSuffix;
    private int index;
    private String killingTest;
    private String description;

    Builder() {

    }

    /**
     * @param state
     *     the {@link ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant.State} of the mutant. Only killed mutants are good mutants.
     *
     * @return this builder
     */
    public Builder mutantStatus(final State state) {

      this.detected = !state.isAlive();
      this.state = state;
      return this;
    }

    /**
     * @param statusName
     *     the {@link ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant.State} of the mutant as a string. Only killed mutants are good mutants.
     *
     * @return this builder
     */
    public Builder mutantStatus(final String statusName) {

      return mutantStatus(State.parse(statusName));
    }

    /**
     * @param sourceFile
     *     the path to the sourceFile that contains the mutant. The sourceFile is relative to the project path.
     *
     * @return this builder
     */
    public Builder inSourceFile(final String sourceFile) {

      this.sourceFile = sourceFile;
      return this;
    }

    /**
     * @param mutatedClass
     *     the fully qualified class name containing the mutant
     *
     * @return this builder
     */
    public Builder inClass(final String mutatedClass) {

      this.mutatedClass = mutatedClass;
      return this;
    }

    /**
     * @param mutatedMethod
     *     the name of the method containing the mutant
     *
     * @return this builder
     */
    public Builder inMethod(final String mutatedMethod) {

      this.mutatedMethod = mutatedMethod;
      return this;
    }

    /**
     * @param methodDescription
     *     the description of the method that specifies its signature.
     *     {@see http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.3}
     *
     * @return this builder
     */
    public Builder withMethodParameters(final String methodDescription) {

      this.methodDescription = methodDescription;
      return this;
    }

    /**
     * @param lineNumber
     *     the line number where the mutant was found
     *
     * @return this builder
     */
    public Builder inLine(final int lineNumber) {

      this.lineNumber = lineNumber;
      return this;
    }

    /**
     * @param mutationOperator
     *     the mutationOperator that was used to create the mutant
     *
     * @return this builder
     */
    public Builder usingMutator(final MutationOperator mutationOperator) {

      this.mutationOperator = mutationOperator;
      mutatorSuffix = "";
      return this;
    }

    /**
     * @param mutagenName
     *     the mutationOperator that was used to create the mutant specified as String. The string may be either the the ID,
     *     the fully qualified class name or the fully qualified class name and a suffix. If the mutagenName is
     *     specified with suffix, the mutationOperator suffix is set accordingly, otherwise the empty string is used.
     *
     * @return this builder
     */
    public Builder usingMutator(final String mutagenName) {

      mutationOperator = MutationOperators.find(mutagenName);

      if (mutationOperator == MutationOperators.UNKNOWN) {
        LOGGER.warn("Found unknown mutation operator: {}", mutagenName);
        mutatorSuffix = "";
      } else if (mutagenName.startsWith(mutationOperator.getClassName())) {
        mutatorSuffix = mutagenName.substring(mutationOperator.getClassName().length());
      } else {
        mutatorSuffix = "";
      }

      if (mutatorSuffix.startsWith("_")) {
        mutatorSuffix = mutatorSuffix.substring(1);
      }
      return this;

    }

    /**
     * @param index
     *     the index of the mutationOperator. It has no relevance to the sonar results
     *
     * @return this builder
     */
    public Builder atIndex(final int index) {

      this.index = index;
      return this;
    }

    /**
     * @param killingTest
     *     the fully qualified name of the test including the test method that killed the test. This method is
     *     optional and only has to be invoked, if the mutant was actually killed. If not invoked, the the
     *     killingTest property is passed as empty string
     *
     * @return this builder
     */
    public Builder killedBy(final String killingTest) {

      this.killingTest = killingTest;
      return this;
    }

    /**
     * Creates a new {@link ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant} with all the parameters specified.
     * As the {@link ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant} requires all parameter to
     * be not-null this method will fail if some parameters have not been specified.
     *
     * @return a new instance of a {@link ch.devcon5.sonar.plugins.mutationanalysis.model.Mutant}
     */
    public Mutant build() {

      requireNonNull(state, "state must be set");
      requireNonNull(sourceFile, "sourceFile must not be set");
      requireNonNull(mutatedClass, "mutatedClass must be set");
      requireNonNull(mutatedMethod, "mutatedMethod must be set");
      requireNonNull(methodDescription, "methodDescription must be set");
      requireNonNull(mutationOperator, "mutationOperator must be set");

      if (detected) {
        requireNonNull(killingTest, "killingTest must be set");
      }

      return new Mutant(this);
    }

    public Builder withDescription(final String description) {
      this.description = description;
      return this;
    }
  }
}
