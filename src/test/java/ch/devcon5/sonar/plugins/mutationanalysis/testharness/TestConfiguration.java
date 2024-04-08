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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.sonar.api.config.Configuration;

/**
 * Basic Implementation to be used in tests instead of using a mock
 */
public class TestConfiguration implements Configuration {

  private final Map<String, String> settings = new ConcurrentHashMap<>();

  public TestConfiguration() {}

  public TestConfiguration set(String key, Object value) {
    this.settings.put(key, String.valueOf(value));
    return this;
  }

  @Override
  public Optional<String> get(final String key) {
    return Optional.ofNullable(settings.get(key));
  }

  @Override
  public boolean hasKey(final String key) {
    return settings.containsKey(key);
  }

  @Override
  public String[] getStringArray(final String key) {
    return get(key).map(v -> v.split(",")).orElse(new String[0]);
  }

}
