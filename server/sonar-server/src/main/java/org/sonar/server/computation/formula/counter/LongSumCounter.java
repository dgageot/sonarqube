/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.formula.counter;

import com.google.common.base.Optional;
import org.sonar.server.computation.formula.LeafAggregateContext;
import org.sonar.server.computation.measure.Measure;

/**
 * Simple counter that do the sum of an integer measure
 */
public class LongSumCounter implements SumCounter<Long, LongSumCounter> {

  private final String metricKey;

  private long value = 0;
  private boolean initialized = false;

  public LongSumCounter(String metricKey) {
    this.metricKey = metricKey;
  }

  @Override
  public void aggregate(LongSumCounter counter) {
    if (counter.getValue().isPresent()) {
      addValue(counter.getValue().get());
    }
  }

  @Override
  public void aggregate(LeafAggregateContext context) {
    Optional<Measure> measureOptional = context.getMeasure(metricKey);
    if (measureOptional.isPresent()) {
      addValue(measureOptional.get().getLongValue());
    }
  }

  private void addValue(long newValue) {
    initialized = true;
    value += newValue;
  }

  @Override
  public Optional<Long> getValue() {
    if (initialized) {
      return Optional.of(value);
    }
    return Optional.absent();
  }
}
