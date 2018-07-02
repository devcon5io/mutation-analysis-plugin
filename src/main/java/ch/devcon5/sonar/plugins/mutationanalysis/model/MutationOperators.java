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

import static javax.xml.xpath.XPathConstants.NODESET;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *
 */
public final class MutationOperators {
    
    private MutationOperators(){}

    /**
     * XPath used for finding mutagens and their attributes in the mutagen-def.xml file
     */
    private static final ThreadLocal<XPathFactory> XPATH_FACTORY = ThreadLocal.withInitial(XPathFactory::newInstance);
    /**
     * Default MutationOperator definition for an unknown {@link MutationOperator}
     */
    public static final MutationOperator UNKNOWN = new MutationOperator("UNKNOWN",
                                                                        "Unknown mutagen",
                                                                        "unknown.mutation.operator",
                                                                        "An unknown mutagen has been applied",
                                                                        null);
    /**
     * URL of the mutagen definitions.
     */
    private static final URL MUTAGEN_DEF = MutationOperator.class.getResource("mutagen-def.xml");
    /**
     * Contains all instances of {@link MutationOperator}s defined in the mutagen-def.xml
     */
    private static final Map<String, MutationOperator> INSTANCES;

    static {
        try (InputStream stream = MUTAGEN_DEF.openStream()) {
            final Map<String, MutationOperator> mutagens = new HashMap<>();
            final XPath xp = XPATH_FACTORY.get().newXPath();
            final NodeList mutagenNodes = (NodeList) xp.evaluate("//operator", new InputSource(stream), NODESET);
            for (int i = 0, len = mutagenNodes.getLength(); i < len; i++) {
                final Node mutagenNode = mutagenNodes.item(i);
                final MutationOperator mutationOperator = toMutagen(mutagenNode);
                mutagens.put(mutationOperator.getId(), mutationOperator);
            }
            INSTANCES = Collections.unmodifiableMap(mutagens);
        } catch (IOException | XPathExpressionException e) {
            throw new RuntimeException("Could not load mutagen definitions", e);
        }
    }

    /**
     * Converts a MutationOperator from the given {@link Node}
     *
     * @param mutagenNode
     *         the node to convert to {@link MutationOperator}
     *
     * @return a {@link MutationOperator} for the {@link Node}
     *
     * @throws XPathExpressionException
     */
    private static MutationOperator toMutagen(final Node mutagenNode) throws XPathExpressionException {

        final XPath xp = XPATH_FACTORY.get().newXPath();
        final String id = xp.evaluate("@id", mutagenNode);
        final String className = xp.evaluate("@class", mutagenNode);
        final String name = xp.evaluate("name", mutagenNode);
        final String violationDescription = xp.evaluate("violationDescription", mutagenNode).trim();
        final URL mutagenDescLoc = MutationOperator.class.getResource(xp.evaluate("operatorDescription/@classpath",
                                                                                  mutagenNode));

        return new MutationOperator(id, name, className, violationDescription, mutagenDescLoc);
    }

    /**
     * Finds the {@link MutationOperator} using the specified key. The key could be the ID of the MutationOperator, it's classname or an
     * extended classname, which is the classname with a suffix.
     *
     * @param mutagenKey
     *         the key to use for searching for the mutagen
     *
     * @return a matchin {@link MutationOperator} or an UNKNOWN mutagen
     */
    public static MutationOperator find(final String mutagenKey) {

        MutationOperator result = UNKNOWN;
        for (final MutationOperator mutationOperator : INSTANCES.values()) {
            if (mutagenKey.equals(mutationOperator.getId())
                    || mutagenKey.equals(mutationOperator.getClassName())
                    || mutagenKey.startsWith(mutationOperator.getClassName())) {
                result = mutationOperator;
                break;
            }
        }
        return result;
    }

    /**
     * Retrieves all defined {@link MutationOperator}s from the mutagen-def.xml.
     *
     * @return a collection of {@link MutationOperator}s
     */
    public static Collection<MutationOperator> allMutagens() {

        return Collections.unmodifiableCollection(INSTANCES.values());
    }
}
