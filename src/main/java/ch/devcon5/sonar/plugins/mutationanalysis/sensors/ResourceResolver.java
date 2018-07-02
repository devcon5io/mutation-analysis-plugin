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
 *
 */
public class ResourceResolver {

    private FileSystem fs;

    public ResourceResolver(final FileSystem fs) {
        this.fs = fs;
    }

    public Optional<InputFile> resolve(String classname){

        return Optional.ofNullable(fs.inputFile(fs.predicates().matchesPathPattern("**/" + getPathToSourceFile(classname))));
    }

    private String getPathToSourceFile(String classname){
        final int nestedClass = classname.lastIndexOf('$');
        final String mainClass;
        if(nestedClass != -1){
            mainClass = classname.substring(0, nestedClass);
        } else {
            mainClass = classname;
        }
        return mainClass.replaceAll("\\.","/")  + ".java";
    }

}
