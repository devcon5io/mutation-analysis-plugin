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

package ch.devcon5.sonar.plugins.mutationanalysis.rules;

import org.sonar.api.config.Configuration;
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;

/**
 * The definition of pitest rules. A new repository is created for the Pitest plugin and Java language. The rules are
 * defined in the rules.xml file in the classpath. The rule keys are accessible as constants.
 */
public class JavaRulesDefinition extends MutationAnalysisRulesDefinition {

  /**
   * Constructor to create the pitest rules definitions and repository. The constructor is invoked by Sonar.
   *
   * @param settings
   *         the settings of the Pitest-Sensor pluin
   * @param xmlLoader
   */
  public JavaRulesDefinition(final Configuration settings, final RulesDefinitionXmlLoader xmlLoader) {
    super(settings, xmlLoader);
  }

  @Override
  protected String getLanguageKey() {
    return "java";
  }


}
