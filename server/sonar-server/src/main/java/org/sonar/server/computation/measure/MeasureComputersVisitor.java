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

package org.sonar.server.computation.measure;

import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.SettingsRepository;
import org.sonar.server.computation.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.measure.api.MeasureComputerImplementationContext;
import org.sonar.server.computation.metric.MetricRepository;

import static org.sonar.server.computation.component.ComponentVisitor.Order.PRE_ORDER;

public class MeasureComputersVisitor extends TypeAwareVisitorAdapter {

  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;
  private final SettingsRepository settings;

  private final MeasureComputersHolder measureComputersHolder;

  public MeasureComputersVisitor(MetricRepository metricRepository, MeasureRepository measureRepository, SettingsRepository settings,
    MeasureComputersHolder measureComputersHolder) {
    super(CrawlerDepthLimit.FILE, PRE_ORDER);
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
    this.settings = settings;
    this.measureComputersHolder = measureComputersHolder;
  }

  @Override
  public void visitAny(org.sonar.server.computation.component.Component component) {
    for (MeasureComputer computer : measureComputersHolder.getMeasureComputers()) {
      MeasureComputerImplementationContext measureComputerContext = new MeasureComputerImplementationContext(component, computer, settings, measureRepository, metricRepository);
      computer.getImplementation().compute(measureComputerContext);
    }
  }
}
