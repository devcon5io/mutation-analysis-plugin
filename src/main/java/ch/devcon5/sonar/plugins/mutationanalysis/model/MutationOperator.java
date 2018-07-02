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

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.internal.apachecommons.io.IOUtils;
import org.sonar.api.rules.Rule;

/**
 * Representation of a PIT MutationOperator. The mutagens are defined in a classpath located file name
 * <code>mutagen-def.xml</code> in the same package. To get an instance of a defined mutagen, use the find() method. As
 * key for finding a mutagen, the ID, classname or a name that contains the classname as prefix can be used. The mutagen
 * itself contains a violation description as well a description of the mutagen itself that is copied from the
 * documentation at <a href="http://pitest.org/quickstart/mutators">pitest.org/quickstart/mutators</a>
 */
public final class MutationOperator {

    private static final Logger LOG = LoggerFactory.getLogger(MutationOperator.class);

    private final String id;
    private final URL mutagenDescLoc;
    private final String violationDesc;
    private final String name;
    private final String className;

    MutationOperator(final String id,
                     final String name,
                     final String className,
                     final String violationDesc,
                     final URL mutagenDescriptionLocation) {

        requireNonNull(id, "Id must not be null");
        requireNonNull(name, "name must not be null");
        requireNonNull(violationDesc, "violation description must not be null");
        this.id = id;
        this.name = name;
        this.className = className;
        this.violationDesc = violationDesc;
        this.mutagenDescLoc = mutagenDescriptionLocation;
    }



    /**
     * The ID of the mutagen. The ID is a String that uniquely defines the MutationOperator, written uppercase, like {@code
     * ARGUMENT_PROPAGATION}.
     *
     * @return the {@link MutationOperator} id as a String
     */
    public String getId() {

        return id;
    }

    /**
     * An URL pointing to the description of the {@link MutationOperator}. The {@link MutationOperator} descriptions are stored in the
     * classpath as resource, for each {@link MutationOperator} a separate description. The URLs are specified in the {@code
     * mutagen-def.xml} in the classpath. The resources itself are html fragments that can be embedded in a website.
     *
     * @return URL pointing to the resource containing the description.
     */
    public Optional<URL> getMutagenDescriptionLocation() {

        return Optional.ofNullable(mutagenDescLoc);
    }

    /**
     * The violation description used for the {@link MutationOperator} specific {@link Rule} that are violated if the mutation
     * caused by the {@link MutationOperator} is not killed.
     *
     * @return the string description the violation.
     */
    public String getViolationDescription() {

        return violationDesc;
    }

    /**
     * The name of the MutationOperator. Unlike the Id, it is more a display name.
     *
     * @return the name as a String
     */
    public String getName() {

        return name;
    }

    /**
     * The fully qualified classname of the {@link MutationOperator} class.
     *
     * @return the classname
     */
    public String getClassName() {

        return className;
    }

    /**
     * The description of the {@link MutationOperator}. The method loads the content defined in the resource that is referred to
     * by the description URL.
     *
     * @return the description as a string
     */
    public String getMutagenDescription() {

        return getMutagenDescriptionLocation().map(u -> {
            try {
                return IOUtils.toString(u);
            } catch (IOException e) {
                LOG.warn("Cannot read mutagen description for mutagen {}", id, e);
                return "No description";
            }
        }).orElse("");
    }

    @Override
    public int hashCode() {

        return 31 + id.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MutationOperator other = (MutationOperator) obj;
        if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

}
