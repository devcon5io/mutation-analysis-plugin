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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;

public class LogRecordingAppender extends AbstractAppender implements AutoCloseable {

  private final List<LogEvent> events = new CopyOnWriteArrayList<>();

  public LogRecordingAppender() {
    super("listAppender", null, null);
    getLoggerConfig().addAppender(this, Level.ALL, null);
    this.start();
  }

  @Override
  public void append(final LogEvent logEvent) {
    events.add(logEvent.toImmutable());
  }

  public List<LogEvent> getEvents() {
    return events;
  }

  private LoggerConfig getLoggerConfig() {
    final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
    return loggerContext.getConfiguration().getLoggerConfig("");
  }

  @Override
  public void close() throws Exception {
    getLoggerConfig().removeAppender(getName());
    this.stop();
  }

}
