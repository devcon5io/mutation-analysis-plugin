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
package ch.devcon5.sonar.plugins.mutationanalysis;

import static ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition.MUTANT_RULES_PREFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinition.Context;
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;

@RunWith(MockitoJUnitRunner.class)
public class MutationAnalysisRulesDefinitionTest {

  @Mock
  private Configuration configuration;

  @InjectMocks
  private MutationAnalysisRulesDefinition subject;

  @Before
  public void setUp() throws Exception {

    when(configuration.get(anyString())).thenReturn(Optional.empty());

    subject = new MutationAnalysisRulesDefinition(configuration, new RulesDefinitionXmlLoader());
  }

  @Test
  public void testDefine() throws Exception {

    // prepare
    Context context = new RulesDefinition.Context();

    // act
    subject.define(context);

    // assert
    RulesDefinition.Repository repository = context.repository(MutationAnalysisRulesDefinition.REPOSITORY_KEY);
    assertNotNull(repository);

    assertEquals("java", repository.language());
    assertEquals(MutationAnalysisRulesDefinition.REPOSITORY_NAME, repository.name());
    assertRules(repository.rules());
  }

  private void assertRules(final List<RulesDefinition.Rule> rules) {

    assertEquals(27, rules.size());

    for (RulesDefinition.Rule rule : rules) {
      assertNotNull(rule.debtRemediationFunction());
      assertNotNull(rule.gapDescription());
      assertNotNull(rule.htmlDescription());
    }

    assertEquals(23, rules.stream()
                          .filter(rule -> rule.key().startsWith(MUTANT_RULES_PREFIX))
                          .filter(rule -> RuleType.BUG.equals(rule.type())).count());
    assertEquals(6, rules.stream().filter(rule -> rule.status() == RuleStatus.BETA).count());
  }

}
