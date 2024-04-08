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

package ch.devcon5.sonar.plugins.mutationanalysis.sensors;

import java.util.Optional;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;

/**
 * Resolver that resolves any given Java or Kotlin class to its source class. If the class is a nested class,
 * then its parent is returned
 */
public class ResourceResolver {

  private final FileSystem fs;

  public ResourceResolver(final FileSystem fs) {
    this.fs = fs;
  }

  public Optional<InputFile> resolve(String classname) {
    return Optional.ofNullable(fs.inputFile(fs.predicates()
        .or(fs.predicates().matchesPathPattern("**/" + getPathToSourceFile(classname, "java")),
            fs.predicates().matchesPathPattern("**/" + getPathToSourceFile(classname, "kt")))));
  }

  private String getPathToSourceFile(String classname, String language) {
    final int nestedClass = classname.indexOf('$');
    final String mainClass;
    if (nestedClass != -1) {
      mainClass = classname.substring(0, nestedClass);
    } else {
      mainClass = classname;
    }
    return mainClass.trim().replace(".", "/") + "." + language;
  }

}
