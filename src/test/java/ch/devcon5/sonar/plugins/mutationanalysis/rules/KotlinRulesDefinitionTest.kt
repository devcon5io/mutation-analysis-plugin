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

package ch.devcon5.sonar.plugins.mutationanalysis.rules

import ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition.MUTANT_RULES_PREFIX
import ch.devcon5.sonar.plugins.mutationanalysis.testharness.TestConfiguration
import org.sonar.api.config.Configuration
import org.sonar.api.rule.RuleStatus
import org.sonar.api.rules.RuleType
import org.sonar.api.server.rule.RulesDefinition
import org.sonar.api.server.rule.RulesDefinitionXmlLoader
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Before as BeforeTest
import org.junit.Test as test


class KotlinRulesDefinitionTest {


    private val configuration: Configuration = TestConfiguration()

    private var subject: KotlinRulesDefinition? = null

    @BeforeTest
    @Throws(Exception::class)
    fun setUp() {

        subject = KotlinRulesDefinition(configuration, RulesDefinitionXmlLoader())
    }

    @test
    @Throws(Exception::class)
    fun testDefine() {

        // prepare
        val context = RulesDefinition.Context()

        // act
        subject!!.define(context)

        // assert
        val repository = context.repository(MutationAnalysisRulesDefinition.REPOSITORY_KEY + ".kotlin")

        assertEquals("kotlin", repository!!.language())
        assertEquals(MutationAnalysisRulesDefinition.REPOSITORY_NAME, repository.name())
        assertRules(repository.rules())
    }

    private fun assertRules(rules: List<RulesDefinition.Rule>) {

        assertEquals(27, rules.size.toLong())

        for (rule in rules) {
            assertNotNull(rule.debtRemediationFunction())
            assertNotNull(rule.gapDescription())
            assertNotNull(rule.htmlDescription())
        }

        assertEquals(26, rules.stream()
                .filter { rule -> rule.key().startsWith(MUTANT_RULES_PREFIX) }
                .filter { rule -> RuleType.BUG == rule.type() }.count())
        assertEquals(6, rules.stream().filter { rule -> rule.status() == RuleStatus.BETA }.count())
        assertEquals(3, rules.stream().filter { rule -> rule.status() == RuleStatus.DEPRECATED }.count())
    }

}
