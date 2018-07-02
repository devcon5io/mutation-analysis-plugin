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

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

/**
 * A builder for creating a new mutant. The builder allows a sequential setting of the mutant parameters while the
 * {@link Mutant} class itself is for immutable instances and therefore needs all parameters at construction time.
 *
 */
public class MutantBuilder {

  private static final Logger LOGGER = getLogger(MutantBuilder.class);

  private boolean detected = false;
  private Mutant.State state;
  private String sourceFile;
  private String mutatedClass;
  private String mutatedMethod;
  private String methodDescription;
  private int lineNumber;
  private MutationOperator mutationOperator;
  private String mutatorSuffix;
  private int index;
  private String killingTest = "";

  MutantBuilder() {

  }

  /**
   * @param detected
   *         flag to indicate if the mutant was detected by a test or not
   *
   * @return this builder
   */
  public MutantBuilder detected(final boolean detected) {

    this.detected = detected;
    return this;
  }

  /**
   * @param state
   *         the {@link Mutant.State} of the mutant. Only killed mutants are good mutants.
   *
   * @return this builder
   */
  public MutantBuilder mutantStatus(final Mutant.State state) {

    this.state = state;
    return this;
  }

  /**
   * @param statusName
   *         the {@link Mutant.State} of the mutant as a string. Only killed mutants are good mutants.
   *
   * @return this builder
   */
  public MutantBuilder mutantStatus(final String statusName) {

    this.state = Mutant.State.parse(statusName);
    return this;

  }

  /**
   * @param sourceFile
   *         the path to the sourceFile that contains the mutant. The sourceFile is relative to the project path.
   *
   * @return this builder
   */
  public MutantBuilder inSourceFile(final String sourceFile) {

    this.sourceFile = sourceFile;
    return this;
  }

  /**
   * @param mutatedClass
   *         the fully qualified class name containing the mutant
   *
   * @return this builder
   */
  public MutantBuilder inClass(final String mutatedClass) {

    this.mutatedClass = mutatedClass;
    return this;
  }

  /**
   * @param mutatedMethod
   *         the name of the method containing the mutant
   *
   * @return this builder
   */
  public MutantBuilder inMethod(final String mutatedMethod) {

    this.mutatedMethod = mutatedMethod;
    return this;
  }

  /**
   * @param methodDescription
   *         the description of the method that specifies its signature.
   *         {@see http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.3}
   *
   * @return this builder
   */
  public MutantBuilder withMethodParameters(final String methodDescription) {

    this.methodDescription = methodDescription;
    return this;
  }

  /**
   * @param lineNumber
   *         the line number where the mutant was found
   *
   * @return this builder
   */
  public MutantBuilder inLine(final int lineNumber) {

    this.lineNumber = lineNumber;
    return this;
  }

  /**
   * @param mutationOperator
   *         the mutationOperator that was used to create the mutant
   *
   * @return this builder
   */
  public MutantBuilder usingMutator(final MutationOperator mutationOperator) {

    this.mutationOperator = mutationOperator;
    mutatorSuffix = "";
    return this;
  }

  /**
   * @param mutagenName
   *         the mutationOperator that was used to create the mutant specified as String. The string may be either the the ID,
   *         the fully qualified class name or the fully qualified class name and a suffix. If the mutagenName is
   *         specified with suffix, the mutationOperator suffix is set accordingly, otherwise the empty string is used.
   *
   * @return this builder
   */
  public MutantBuilder usingMutator(final String mutagenName) {

    mutationOperator = MutationOperators.find(mutagenName);

    if(mutationOperator == MutationOperators.UNKNOWN){
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
   *         the index of the mutationOperator. It has no relevance to the sonar results
   *
   * @return this builder
   */
  public MutantBuilder atIndex(final int index) {

    this.index = index;
    return this;
  }

  /**
   * @param killingTest
   *         the fully qualified name of the test including the test method that killed the test. This method is
   *         optional and only has to be invoked, if the mutant was actually killed. If not invoked, the the
   *         killingTest property is passed as empty string
   *
   * @return this builder
   */
  public MutantBuilder killedBy(final String killingTest) {

    this.killingTest = killingTest;
    return this;
  }

  /**
   * Creates a new {@link Mutant} with all the parameters specified. As the {@link Mutant} requires all parameter to
   * be not-null this method will fail if some parameters are not specified.
   *
   * @return a new instance of a {@link Mutant}
   */
  public Mutant build() {

    return new Mutant(detected, state, sourceFile, mutatedClass, mutatedMethod, methodDescription,
                      lineNumber, mutationOperator, mutatorSuffix, index, killingTest);
  }
}
