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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestUtils {

  /**
   * SLF4J Logger for this class
   */
  private static final Logger LOG = LoggerFactory.getLogger(TestUtils.class);

  /**
   * Creates a temporary file with content from a classpath resource in a path relative to a {@link File}.
   *
   * @param folder the {@link File} folder in which the file should be created
   * @param filePath the path to the file relative to the root of the temporary folder
   * @param resource the absolute path to the classpath resource.
   * @return the created temporary file
   * @throws IOException if the temporary file could not be created
   */
  public static File tempFileFromResource(final Path folder, final String filePath, final String resource) throws IOException {
    return tempFileFromResource(folder, filePath, TestUtils.class, resource);
  }

  /**
   * Creates a temporary file with content from a classpath resource in a path relative to a {@link File}.
   *
   * @param folder the {@link File} directory in which the file should be created
   * @param filePath the path to the file relative to the root of the temporary folder
   * @param baseClass the class that is used to resolve the classpath resource.
   * @param resource the path to the classpath resource relative to the class. The content of the resource is written to
   * the temporary file
   * @return the created file
   * @throws IOException if the temporary file could not be created
   */
  public static File tempFileFromResource(final Path folder, final String filePath, final Class<?> baseClass, final String resource) throws IOException {
    final File tempFile = newTempFile(folder, filePath);
    final URL url = baseClass.getResource(resource);
    assertNotNull(url, "Resource " + resource + " not found");
    copyResourceToFile(url, tempFile);
    return tempFile;
  }

  /**
   * Copies the content from the URL to the specified file
   *
   * @param resource the location of the resource
   * @param tempFile the file to write the data from the resource to
   * @throws IOException the resource could not be copied to the file location
   */
  public static void copyResourceToFile(final URL resource, final File tempFile) throws IOException {
    IOUtils.copy(resource.openStream(), Files.newOutputStream(tempFile.toPath()));
    LOG.info("Created temp file {}", tempFile.getAbsolutePath());
  }

  /**
   * Creates a new temporary file in the {@link File} folder. The file may be specified as path relative to the root of
   * the temporary folder
   *
   * @param folder the temporary folder in which to create the new file
   * @param filePath the name of the file or a relative path to the file to be created
   * @return the {@link File} reference to the newly created file
   * @throws IOException the new temporary file could not be created at the specified location
   */
  public static File newTempFile(final Path folder, final String filePath) throws IOException {
    String path;
    String filename;
    final int lastPathSeparator = filePath.lastIndexOf('/');
    if (lastPathSeparator != -1) {
      path = filePath.substring(0, lastPathSeparator);
      filename = filePath.substring(lastPathSeparator + 1);
    } else {
      path = null;
      filename = filePath;
    }
    return createTempFile(folder, path, filename);
  }

  /**
   * Creates a temporary file in the specified path relative to the {@link File} folder
   *
   * @param folder the {@link File} folder that provides the root for the file
   * @param path the relative to the file, without the filename itself
   * @param filename the name of the file to create
   * @return the {@link File} reference to the new file
   * @throws IOException if the temporary file could not be created
   */
  public static File createTempFile(final Path folder, final String path, final String filename) throws IOException {
    File tempFile;
    if (path != null) {
      File newFolder;
      Path newPath = folder.resolve(path);
      if (!Files.exists(newPath)) {
        newFolder = Files.createDirectories(newPath).toFile();
      } else {
        newFolder = newPath.toFile();
      }
      tempFile = Files.createFile(newFolder.toPath().resolve(filename)).toFile();
    } else {
      tempFile = Files.createFile(folder.resolve(filename)).toFile();
    }
    return tempFile;
  }

  public static <G> G assertContains(List<G> container, Consumer<G> filter) {
    return container.stream().filter(m -> {
          try {
            filter.accept(m);
            return true;
          } catch (AssertionError e) {
            return false;
          }
        }).findFirst()
        .orElseThrow(() -> new AssertionError("No element found matching assertions, elements: " + container));
  }

  public static <G> void assertNotContains(List<G> container, Consumer<G> filter) {
    container.stream().filter(m -> {
      try {
        filter.accept(m);
        return false;
      } catch (AssertionError e) {
        return true;
      }
    }).findFirst().orElseThrow(() -> new AssertionError("No element found matching assertions"));
  }

}
