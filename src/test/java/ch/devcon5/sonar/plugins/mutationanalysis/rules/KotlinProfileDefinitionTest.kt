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

import ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition.REPOSITORY_KEY
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition
import kotlin.test.assertEquals
import org.junit.Test as test


class KotlinProfileDefinitionTest {

    val context = BuiltInQualityProfilesDefinition.Context()

    @test fun define() {

        val def = KotlinProfileDefinition()

        def.define(context)

        val kotlinProfile = context.profile("kotlin", "Mutation Analysis")

        assertEquals(23, kotlinProfile.rules().stream().filter { r -> "$REPOSITORY_KEY.kotlin" == r.repoKey() }.count())
    }
}
