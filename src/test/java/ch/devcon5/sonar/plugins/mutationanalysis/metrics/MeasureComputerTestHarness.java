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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import ch.devcon5.sonar.plugins.mutationanalysis.MutationAnalysisPlugin;
import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.ce.measure.test.TestComponent;
import org.sonar.api.ce.measure.test.TestMeasureComputerContext;
import org.sonar.api.ce.measure.test.TestMeasureComputerDefinitionContext;
import org.sonar.api.ce.measure.test.TestSettings;
import org.sonar.api.config.Configuration;

/**
 *
 */
public class MeasureComputerTestHarness<T extends MeasureComputer> {

  private final T computer;
  private final Configuration config;

  public MeasureComputerTestHarness(final T computer) {

    this(computer, null);
  }

  public MeasureComputerTestHarness(final T computer, final Configuration configuration) {

    this.computer = computer;
    this.config = configuration;
    enableExperimentalFeatures(true);
  }

  public static <T extends MeasureComputer> MeasureComputerTestHarness<T> createFor(Class<T> computer) {

    try {
      Constructor<T> c = computer.getConstructor(Configuration.class);
      Configuration configuration = mock(Configuration.class);
      return new MeasureComputerTestHarness<>(c.newInstance(configuration), configuration);
    } catch (NoSuchMethodException e) {
      try {
        return new MeasureComputerTestHarness<>(computer.getConstructor().newInstance());
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e1) {
        throw new RuntimeException("Could not create computer from default constructor", e1);
      }
    } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw new RuntimeException("Could not create computer with configuration", e);
    }
  }

  public T getComputer() {

    return computer;
  }

  public void enableExperimentalFeatures(final boolean enabled) {

    getConfig().ifPresent(conf -> when(conf.getBoolean(MutationAnalysisPlugin.EXPERIMENTAL_FEATURE_ENABLED)).thenReturn(Optional.of(enabled)));
  }

  public Optional<Configuration> getConfig() {

    return Optional.ofNullable(config);
  }

  public TestMeasureComputerContext createMeasureContextForSourceFile(String componentKey) {

    return createMeasureContext(componentKey, Component.Type.FILE, "java", false);
  }

  public TestMeasureComputerContext createMeasureContextForUnitTest(String componentKey) {
    return createMeasureContext(componentKey, Component.Type.FILE, "java", true);

  }
  public TestMeasureComputerContext createMeasureContextForDirectory(String componentKey) {

    return createMeasureContext(componentKey, Component.Type.DIRECTORY, "java", false);
  }

  public TestMeasureComputerContext createMeasureContextForModule(String componentKey) {

    return createMeasureContext(componentKey, Component.Type.MODULE, "java", false);
  }
  public TestMeasureComputerContext createMeasureContextForProject(String componentKey) {

    return createMeasureContext(componentKey, Component.Type.PROJECT, "java", false);
  }

  public TestMeasureComputerContext createMeasureContext(String componentKey, Component.Type type) {

    return createMeasureContext(componentKey, type, "java", false);
  }

  public TestMeasureComputerContext createMeasureContext(String componentKey, Component.Type type, String language, boolean unitTest) {

    final TestMeasureComputerDefinitionContext context = new TestMeasureComputerDefinitionContext();
    final MeasureComputer.MeasureComputerDefinition def = computer.define(context);
    final TestSettings settings = new TestSettings();

    if(type == Component.Type.FILE){
      return new TestMeasureComputerContext(new TestComponent(componentKey, type, new TestComponent.FileAttributesImpl(language, unitTest)), settings, def);
    }
    return new TestMeasureComputerContext(new TestComponent(componentKey, type, null), settings, def);
  }

  public TestMeasureComputerContext createMeasureContext(String componentKey, Component.Type type, boolean unitTest) {

    return createMeasureContext(componentKey, type, "java", unitTest);
  }
}
