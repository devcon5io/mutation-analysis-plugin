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

import static ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition.REPOSITORY_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition.BuiltInQualityProfile;

class JavaProfileDefinitionTest {

  private final BuiltInQualityProfilesDefinition.Context context = new BuiltInQualityProfilesDefinition.Context();

  @Test
  void define_java() {
    MutationAnalysisProfileDefinition def = new JavaProfileDefinition();

    def.define(context);

    BuiltInQualityProfile javaProfile = context.profile("java", "Mutation Analysis");

    assertEquals(23, javaProfile.rules().stream().filter(r -> (REPOSITORY_KEY + ".java").equals(r.repoKey())).count());
  }

}
