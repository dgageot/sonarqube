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

package org.sonar.server.computation.measure.api;

import com.google.common.base.Optional;
import java.util.Collections;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.ce.measure.Component;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.SettingsRepository;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricImpl;
import org.sonar.server.computation.metric.MetricRepositoryRule;

import static com.google.common.collect.ImmutableSet.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.COMMENT_LINES_KEY;
import static org.sonar.api.measures.CoreMetrics.NCLOC_KEY;
import static org.sonar.server.computation.component.ReportComponent.builder;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;

public class MeasureComputerImplementationContextTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static final String INT_METRIC_KEY = "int_metric_key";
  private static final String DOUBLE_METRIC_KEY = "double_metric_key";
  private static final String LONG_METRIC_KEY = "long_metric_key";
  private static final String STRING_METRIC_KEY = "string_metric_key";

  private static final int PROJECT_REF = 1;
  private static final int FILE_1_REF = 12341;
  private static final String FILE_1_KEY = "fileKey";
  private static final int FILE_2_REF = 12342;

  private static final org.sonar.server.computation.component.Component FILE_1 = builder(org.sonar.server.computation.component.Component.Type.FILE, FILE_1_REF).setKey(FILE_1_KEY).build();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule()
    .setRoot(builder(org.sonar.server.computation.component.Component.Type.PROJECT, PROJECT_REF).setKey("project")
      .addChildren(
        FILE_1,
        builder(org.sonar.server.computation.component.Component.Type.FILE, FILE_2_REF).setKey("fileKey2").build()
      ).build());

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(1, CoreMetrics.NCLOC)
    .add(new MetricImpl(2, INT_METRIC_KEY, "int metric", Metric.MetricType.INT))
    .add(new MetricImpl(3, DOUBLE_METRIC_KEY, "double metric", Metric.MetricType.FLOAT))
    .add(new MetricImpl(4, LONG_METRIC_KEY, "long metric", Metric.MetricType.MILLISEC))
    .add(new MetricImpl(5, STRING_METRIC_KEY, "string metric", Metric.MetricType.STRING))
    ;

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  SettingsRepository settingsRepository = mock(SettingsRepository.class);

  @Test
  public void get_component() throws Exception {
    MeasureComputer.Implementation.Context underTest = newContext(FILE_1_REF);
    assertThat(underTest.getComponent().getType()).isEqualTo(Component.Type.FILE);
  }

  @Test
  public void get_string_settings() throws Exception {
    org.sonar.api.config.Settings serverSettings = new org.sonar.api.config.Settings();
    serverSettings.setProperty("prop", "value");
    when(settingsRepository.getSettings(FILE_1)).thenReturn(serverSettings);

    MeasureComputer.Implementation.Context underTest = newContext(FILE_1_REF);
    assertThat(underTest.getSettings().getString("prop")).isEqualTo("value");
    assertThat(underTest.getSettings().getString("unknown")).isNull();
  }

  @Test
  public void get_string_array_settings() throws Exception {
    org.sonar.api.config.Settings serverSettings = new org.sonar.api.config.Settings();
    serverSettings.setProperty("prop", "1,3.4,8,50");
    when(settingsRepository.getSettings(FILE_1)).thenReturn(serverSettings);

    MeasureComputer.Implementation.Context underTest = newContext(FILE_1_REF);
    assertThat(underTest.getSettings().getStringArray("prop")).containsExactly("1", "3.4", "8", "50");
    assertThat(underTest.getSettings().getStringArray("unknown")).isEmpty();
  }

  @Test
  public void get_measure() throws Exception {
    measureRepository.addRawMeasure(FILE_1_REF, NCLOC_KEY, newMeasureBuilder().create(10));

    MeasureComputer.Implementation.Context underTest = newContext(FILE_1_REF, of(NCLOC_KEY), of(COMMENT_LINES_KEY));
    assertThat(underTest.getMeasure(NCLOC_KEY).getIntValue()).isEqualTo(10);
  }

  @Test
  public void fail_with_IAE_when_get_measure_is_called_on_metric_not_in_input_list() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Only metrics in [another metric] can be used to load measures");

    MeasureComputer.Implementation.Context underTest = newContext(PROJECT_REF, of("another metric"), of("debt"));
    underTest.getMeasure(NCLOC_KEY);
  }

  @Test
  public void get_children_measures() throws Exception {
    measureRepository.addRawMeasure(FILE_1_REF, NCLOC_KEY, newMeasureBuilder().create(10));
    measureRepository.addRawMeasure(FILE_2_REF, NCLOC_KEY, newMeasureBuilder().create(12));

    MeasureComputer.Implementation.Context underTest = newContext(PROJECT_REF, of(NCLOC_KEY), of(COMMENT_LINES_KEY));
    assertThat(underTest.getChildrenMeasures(NCLOC_KEY)).hasSize(2);
    assertThat(underTest.getChildrenMeasures(NCLOC_KEY)).extracting("intValue").containsOnly(10, 12);
  }

  @Test
  public void get_children_measures_when_one_child_has_no_value() throws Exception {
    measureRepository.addRawMeasure(FILE_1_REF, NCLOC_KEY, newMeasureBuilder().create(10));
    // No data on file 2

    MeasureComputer.Implementation.Context underTest = newContext(PROJECT_REF, of(NCLOC_KEY), of(COMMENT_LINES_KEY));
    assertThat(underTest.getChildrenMeasures(NCLOC_KEY)).extracting("intValue").containsOnly(10);
  }

  @Test
  public void not_fail_to_get_children_measures_on_output_metric() throws Exception {
    measureRepository.addRawMeasure(FILE_1_REF, INT_METRIC_KEY, newMeasureBuilder().create(10));

    MeasureComputer.Implementation.Context underTest = newContext(PROJECT_REF, of(NCLOC_KEY), of(INT_METRIC_KEY));
    assertThat(underTest.getChildrenMeasures(INT_METRIC_KEY)).hasSize(1);
    assertThat(underTest.getChildrenMeasures(INT_METRIC_KEY)).extracting("intValue").containsOnly(10);
  }

  @Test
  public void fail_with_IAE_when_get_children_measures_is_called_on_metric_not_in_input_list() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Only metrics in [another metric] can be used to load measures");

    MeasureComputer.Implementation.Context underTest = newContext(PROJECT_REF, of("another metric"), of("debt"));
    underTest.getChildrenMeasures(NCLOC_KEY);
  }

  @Test
  public void add_int_measure_create_measure_of_type_int_with_right_value() throws Exception {
    MeasureComputer.Implementation.Context underTest = newContext(PROJECT_REF, of(NCLOC_KEY), of(INT_METRIC_KEY));
    underTest.addMeasure(INT_METRIC_KEY, 10);

    Optional<Measure> measure =  measureRepository.getAddedRawMeasure(PROJECT_REF, INT_METRIC_KEY);
    assertThat(measure).isPresent();
    assertThat(measure.get().getIntValue()).isEqualTo(10);
  }

  @Test
  public void add_double_measure_create_measure_of_type_double_with_right_value() throws Exception {
    MeasureComputer.Implementation.Context underTest = newContext(PROJECT_REF, of(NCLOC_KEY), of(DOUBLE_METRIC_KEY));
    underTest.addMeasure(DOUBLE_METRIC_KEY, 10d);

    Optional<Measure> measure =  measureRepository.getAddedRawMeasure(PROJECT_REF, DOUBLE_METRIC_KEY);
    assertThat(measure).isPresent();
    assertThat(measure.get().getDoubleValue()).isEqualTo(10d);
  }

  @Test
  public void add_long_measure_create_measure_of_type_long_with_right_value() throws Exception {
    MeasureComputer.Implementation.Context underTest = newContext(PROJECT_REF, of(NCLOC_KEY), of(LONG_METRIC_KEY));
    underTest.addMeasure(LONG_METRIC_KEY, 10L);

    Optional<Measure> measure =  measureRepository.getAddedRawMeasure(PROJECT_REF, LONG_METRIC_KEY);
    assertThat(measure).isPresent();
    assertThat(measure.get().getLongValue()).isEqualTo(10L);
  }

  @Test
  public void add_string_measure_create_measure_of_type_string_with_right_value() throws Exception {
    MeasureComputer.Implementation.Context underTest = newContext(PROJECT_REF, of(NCLOC_KEY), of(STRING_METRIC_KEY));
    underTest.addMeasure(STRING_METRIC_KEY, "data");

    Optional<Measure> measure =  measureRepository.getAddedRawMeasure(PROJECT_REF, STRING_METRIC_KEY);
    assertThat(measure).isPresent();
    assertThat(measure.get().getStringValue()).isEqualTo("data");
  }

  @Test
  public void fail_with_IAE_when_add_measure_is_called_on_metric_not_in_output_list() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Only metrics in [int_metric_key] can be used to add measures");

    MeasureComputer.Implementation.Context underTest = newContext(PROJECT_REF, of(NCLOC_KEY), of(INT_METRIC_KEY));
    underTest.addMeasure(DOUBLE_METRIC_KEY, 10);
  }

  @Test
  public void fail_with_unsupported_operation_when_adding_measure_that_already_exists() throws Exception {
    thrown.expect(UnsupportedOperationException.class);
    thrown.expectMessage("A measure on metric 'int_metric_key' already exists on component 'fileKey'");

    measureRepository.addRawMeasure(FILE_1_REF, INT_METRIC_KEY, newMeasureBuilder().create(20));

    MeasureComputer.Implementation.Context underTest = newContext(FILE_1_REF, of(NCLOC_KEY), of(INT_METRIC_KEY));
    underTest.addMeasure(INT_METRIC_KEY, 10);
  }

  private MeasureComputer.Implementation.Context newContext(int componentRef) {
    return newContext(componentRef, Collections.<String>emptySet(), Collections.<String>emptySet());
  }

  private MeasureComputer.Implementation.Context newContext(int componentRef, final Set<String> inputMetrics, final Set<String> outputMetrics) {
    MeasureComputer measureComputer = new MeasureComputer() {
      @Override
      public Set<String> getInputMetrics() {
        return inputMetrics;
      }

      @Override
      public Set<String> getOutputMetrics() {
        return outputMetrics;
      }

      @Override
      public Implementation getImplementation() {
        return null;
      }
    };
    return new MeasureComputerImplementationContext(treeRootHolder.getComponentByRef(componentRef), measureComputer, settingsRepository, measureRepository, metricRepository);
  }
}
