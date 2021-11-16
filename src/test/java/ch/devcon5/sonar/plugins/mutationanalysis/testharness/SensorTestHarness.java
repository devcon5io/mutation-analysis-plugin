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

package ch.devcon5.sonar.plugins.mutationanalysis.testharness;

import static ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition.REPOSITORY_KEY;
import static org.sonar.api.rules.RulePriority.MAJOR;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import ch.devcon5.sonar.plugins.mutationanalysis.rules.MutationAnalysisRulesDefinition;
import org.jetbrains.annotations.NotNull;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.DefaultActiveRules;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;

public class SensorTestHarness {

   private final String language;
   private final Optional<TemporaryFolder> folder;
   private final Path basePath;
   private final String moduleName;
   private final Class<?> resourceResolver;

   public SensorTestHarness(final Builder builder) {
      this.language = builder.language;
      this.basePath = builder.basePath;
      this.moduleName = builder.moduleName;
      this.resourceResolver = builder.resourceResolver;
      this.folder = Optional.ofNullable(builder.tempFolder);

   }

   public SensorTestHarness resolveResourcesFrom(Class<?> caller) {
      return builder().from(this).withResourceResolver(caller).build();
   }

   public SensorTestHarness changeBasePath(Path newPath) {
      return builder().from(this).withBasePath(newPath).build();
   }

   public SensorTestHarness changeLanguage(String newLanguage) {
      return builder().from(this).withLanguage(newLanguage).build();
   }

   public SensorTestHarness changeModuleName(String newModuleName) {
      return builder().from(this).withModuleName(newModuleName).build();
   }

   public Path createSourceFile(String path, String filename) throws IOException {
      return this.folder.map(f -> {
         try {
            final File file = TestUtils.createTempFile(f, path, filename);
            file.createNewFile();
            return file.toPath();
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      }).orElseGet(() -> {
         try {
            return Files.createFile(this.basePath.resolve(path).resolve(filename));
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      });
   }

   public Path resourceToFile(final String filePath, final String resource) throws IOException {
      return this.folder.map(f -> {
         try {
            return TestUtils.tempFileFromResource(f, filePath, resourceResolver, resource).toPath();
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      }).orElseGet(() -> {
         try (InputStream is = resourceResolver.getResourceAsStream(resource)) {
            if (is != null) {
               Path destination = basePath.resolve(filePath);
               Files.copy(is, destination);
               return destination;
            } else {
               throw new RuntimeException("Resource " + resource + " not found using " + resourceResolver);
            }
         } catch (IOException e) {
            throw new RuntimeException(e);
         }
      });

   }

   public TestConfiguration createConfiguration() {
      return new TestConfiguration();
   }

   public TestSensorContext createSensorContext() {

      return new TestSensorContext(basePath, moduleName);
   }

   public ActiveRules createEmptyActiveRules() {
      return new DefaultActiveRules(Collections.emptyList());
   }

   public RulesProfile createEmptyRulesProfile() {
      return RulesProfile.create("test.profile", language);
   }

   /**
    * @param ruleKeys
    * @return
    * @deprecated use createActiveRules
    */
   @Deprecated
   public RulesProfile createRulesProfile(String... ruleKeys) {
      return createRulesProfile(Arrays.stream(ruleKeys)
                                      .map(this::createRule)
                                      .toArray(Rule[]::new));
   }
   public ActiveRules createActiveRules(String... ruleKeys) {

      final ActiveRulesBuilder builder = new ActiveRulesBuilder();

      for(String ruleKey : ruleKeys){

         builder.addRule(newActiveRule().setRuleKey(RuleKey.of(MutationAnalysisRulesDefinition.REPOSITORY_KEY + ".kotlin", ruleKey)).build());
         builder.addRule(newActiveRule().setRuleKey(RuleKey.of(MutationAnalysisRulesDefinition.REPOSITORY_KEY + ".java", ruleKey)).build());
      }
      return builder.build();
   }

   @NotNull
   private NewActiveRule.Builder newActiveRule() {
      return new NewActiveRule.Builder();
   }

   public ActiveRules createActiveRules(Rule... rules) {

      final ActiveRulesBuilder builder = new ActiveRulesBuilder();
      for(Rule rule : rules){
         final NewActiveRule.Builder activeRule = newActiveRule().setRuleKey(RuleKey.of(rule.getRepositoryKey(), rule.getKey()));
         rule.getParams().forEach(param -> activeRule.setParam(param.getKey(), param.getDefaultValue()));
         builder.addRule(activeRule.build());
      }
      return builder.build();
   }

   /**
    * @deprecated use createActiveRules
    * @param rules
    * @return
    */
   @Deprecated
   public RulesProfile createRulesProfile(Rule... rules) {

      final RulesProfile profile = createEmptyRulesProfile();
      profile.setActiveRules(Arrays.stream(rules).map(r -> {
         final ActiveRule ar = new ActiveRule(profile, r, MAJOR);
         r.getParams().forEach(p -> ar.setParameter(p.getKey(), p.getDefaultValue()));
         return ar;
      }).collect(Collectors.toList()));
      return profile;
   }

   public Rule createRule(String language, final String ruleKey) {
      return Rule.create(REPOSITORY_KEY + "." + language, ruleKey).setLanguage(language);
   }
   public Rule createRule(final String ruleKey) {
      return createRule(this.language, ruleKey);
   }
   public Rule createRule(String ruleKey, String key, String value) {

      final Rule r = Rule.create(REPOSITORY_KEY + "." + language, ruleKey);
      r.createParameter(key).setDefaultValue(value);
      return r;
   }

   public static Builder builder() {
      return new Builder();
   }

   /**
    * Builder for fluently create new immutable sensor harnesses
    */
   public static class Builder {

      private String language = "java";
      private TemporaryFolder tempFolder;
      private Path basePath;
      private String moduleName = "test-module";
      private Class<?> resourceResolver = SensorTestHarness.class;

      public Builder withLanguage(String language) {
         this.language = language;
         return this;
      }

      public Builder withBasePath(Path basePath) {
         this.basePath = basePath;
         return this;
      }

      public Builder withModuleName(String moduleName) {
         this.moduleName = moduleName;
         return this;
      }

      public Builder withResourceResolver(Class<?> resourceResolver) {
         this.resourceResolver = resourceResolver;
         return this;
      }

      public Builder withTempFolder(TemporaryFolder tempFolder) {
         this.tempFolder = tempFolder;
         this.basePath = tempFolder.getRoot().toPath();
         return this;
      }

      public Builder from(SensorTestHarness template) {
         this.tempFolder = template.folder.orElse(null);
         this.basePath = template.basePath;
         this.moduleName = template.moduleName;
         this.resourceResolver = template.resourceResolver;
         this.language = template.language;
         return this;
      }

      public SensorTestHarness build() {
         requireNonNull(language, "Language");
         requireNonNull(basePath, "Base path");
         requireNonNull(moduleName, "Module Name");
         requireNonNull(resourceResolver, "Resource resolver class");

         return new SensorTestHarness(this);
      }

      private static void requireNonNull(final Object arg, final String role) {
         if (arg == null) {
            throw new IllegalArgumentException(role + " must not be null");
         }
      }
   }

}
