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

package org.sonar.server.measure.custom.ws;

import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.measure.custom.db.CustomMeasureDto;
import org.sonar.core.metric.db.MetricDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.db.DbClient;
import org.sonar.server.user.UserSession;

import static org.sonar.server.measure.custom.ws.CustomMeasureValueDescription.measureValueDescription;

public class UpdateAction implements CustomMeasuresWsAction {
  public static final String ACTION = "update";
  public static final String PARAM_ID = "id";
  public static final String PARAM_VALUE = "value";
  public static final String PARAM_DESCRIPTION = "description";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final System2 system;
  private final CustomMeasureValidator validator;
  private final CustomMeasureJsonWriter customMeasureJsonWriter;

  public UpdateAction(DbClient dbClient, UserSession userSession, System2 system, CustomMeasureValidator validator, CustomMeasureJsonWriter customMeasureJsonWriter) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.system = system;
    this.validator = validator;
    this.customMeasureJsonWriter = customMeasureJsonWriter;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setPost(true)
      .setDescription("Update a custom measure. Value and/or description must be provided<br />" +
        "Requires 'Administer System' permission or 'Administer' permission on the project.")
      .setHandler(this)
      .setSince("5.2");

    action.createParam(PARAM_ID)
      .setRequired(true)
      .setDescription("id")
      .setExampleValue("42");

    action.createParam(PARAM_VALUE)
      .setExampleValue("true")
      .setDescription(measureValueDescription());

    action.createParam(PARAM_DESCRIPTION)
      .setExampleValue("Team size growing.");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    int id = request.mandatoryParamAsInt(PARAM_ID);
    String value = request.param(PARAM_VALUE);
    String description = request.param(PARAM_DESCRIPTION);
    checkParameters(value, description);

    DbSession dbSession = dbClient.openSession(true);
    try {
      CustomMeasureDto customMeasure = dbClient.customMeasureDao().selectById(dbSession, id);
      MetricDto metric = dbClient.metricDao().selectById(dbSession, customMeasure.getMetricId());
      ComponentDto component = dbClient.componentDao().selectByUuid(dbSession, customMeasure.getComponentUuid());

      checkPermissions(component);

      setValue(customMeasure, value, metric);
      setDescription(customMeasure, description);
      customMeasure.setUpdatedAt(system.now());
      dbClient.customMeasureDao().update(dbSession, customMeasure);
      dbSession.commit();

      JsonWriter json = response.newJsonWriter();
      customMeasureJsonWriter.write(json, customMeasure, metric, component);
      json.close();
    } finally {
      MyBatis.closeQuietly(dbSession);
    }
  }

  private void setValue(CustomMeasureDto customMeasure, @Nullable String value, MetricDto metric) {
    if (value != null) {
      validator.setMeasureValue(customMeasure, value, metric);
    }
  }

  private void setDescription(CustomMeasureDto customMeasure, @Nullable String description) {
    if (description != null) {
      customMeasure.setDescription(description);
    }
  }

  private void checkParameters(@Nullable String value, @Nullable String description) {
    if (value == null && description == null) {
      throw new IllegalArgumentException("Value or description must be provided.");
    }
  }

  private void checkPermissions(ComponentDto component) {
    if (userSession.hasGlobalPermission(GlobalPermissions.SYSTEM_ADMIN)) {
      return;
    }

    userSession.checkLoggedIn().checkProjectUuidPermission(UserRole.ADMIN, component.projectUuid());
  }
}
