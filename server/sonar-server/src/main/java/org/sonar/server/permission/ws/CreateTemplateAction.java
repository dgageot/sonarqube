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

package org.sonar.server.permission.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Permissions;
import org.sonarqube.ws.Permissions.PermissionTemplate;

import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdminUser;
import static org.sonar.server.permission.ws.PermissionRequestValidator.MSG_TEMPLATE_NAME_NOT_BLANK;
import static org.sonar.server.permission.ws.PermissionRequestValidator.MSG_TEMPLATE_WITH_SAME_NAME;
import static org.sonar.server.permission.ws.PermissionRequestValidator.validateProjectPattern;
import static org.sonar.server.permission.ws.Parameters.PARAM_TEMPLATE_DESCRIPTION;
import static org.sonar.server.permission.ws.Parameters.PARAM_TEMPLATE_NAME;
import static org.sonar.server.permission.ws.Parameters.PARAM_TEMPLATE_PATTERN;
import static org.sonar.server.permission.ws.Parameters.createTemplateDescriptionParameter;
import static org.sonar.server.permission.ws.Parameters.createTemplateProjectKeyPatternParameter;
import static org.sonar.server.permission.ws.PermissionTemplateDtoBuilder.create;
import static org.sonar.server.permission.ws.PermissionTemplateDtoToPermissionTemplateResponse.toPermissionTemplateResponse;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class CreateTemplateAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final System2 system;

  public CreateTemplateAction(DbClient dbClient, UserSession userSession, System2 system) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.system = system;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("create_template")
      .setDescription("Create a permission template.<br />" +
        "It requires administration permissions to access.")
      .setResponseExample(getClass().getResource("create_template-example.json"))
      .setSince("5.2")
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_TEMPLATE_NAME)
      .setRequired(true)
      .setDescription("Name")
      .setExampleValue("Financial Service Permissions");

    createTemplateProjectKeyPatternParameter(action);
    createTemplateDescriptionParameter(action);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    String name = wsRequest.mandatoryParam(PARAM_TEMPLATE_NAME);
    String description = wsRequest.param(PARAM_TEMPLATE_DESCRIPTION);
    String projectPattern = wsRequest.param(PARAM_TEMPLATE_PATTERN);

    DbSession dbSession = dbClient.openSession(false);
    try {
      checkGlobalAdminUser(userSession);
      validateTemplateNameForCreation(dbSession, name);
      validateProjectPattern(projectPattern);

      PermissionTemplateDto permissionTemplate = insertTemplate(dbSession, name, description, projectPattern);

      Permissions.CreatePermissionTemplateResponse response = buildResponse(permissionTemplate);
      writeProtobuf(response, wsRequest, wsResponse);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private void validateTemplateNameForCreation(DbSession dbSession, String templateName) {
    checkRequest(!templateName.isEmpty(), MSG_TEMPLATE_NAME_NOT_BLANK);

    PermissionTemplateDto permissionTemplateWithSameName = dbClient.permissionTemplateDao().selectByName(dbSession, templateName);
    checkRequest(permissionTemplateWithSameName == null, String.format
      (MSG_TEMPLATE_WITH_SAME_NAME, templateName));
  }

  private PermissionTemplateDto insertTemplate(DbSession dbSession, String name, String description, String projectPattern) {
    PermissionTemplateDto template = dbClient.permissionTemplateDao().insert(dbSession, create(system)
      .setName(name)
      .setDescription(description)
      .setProjectKeyPattern(projectPattern)
      .toDto());
    dbSession.commit();
    return template;
  }

  private static Permissions.CreatePermissionTemplateResponse buildResponse(PermissionTemplateDto permissionTemplateDto) {
    PermissionTemplate permissionTemplateBuilder = toPermissionTemplateResponse(permissionTemplateDto);
    return Permissions.CreatePermissionTemplateResponse.newBuilder().setPermissionTemplate(permissionTemplateBuilder).build();
  }
}
