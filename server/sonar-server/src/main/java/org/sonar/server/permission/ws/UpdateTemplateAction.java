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

import java.util.Date;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Permissions.PermissionTemplate;
import org.sonarqube.ws.Permissions.UpdatePermissionTemplateResponse;

import static com.google.common.base.Objects.firstNonNull;
import static java.lang.String.format;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdminUser;
import static org.sonar.server.permission.ws.PermissionRequestValidator.MSG_TEMPLATE_NAME_NOT_BLANK;
import static org.sonar.server.permission.ws.PermissionRequestValidator.MSG_TEMPLATE_WITH_SAME_NAME;
import static org.sonar.server.permission.ws.PermissionRequestValidator.validateProjectPattern;
import static org.sonar.server.permission.ws.Parameters.PARAM_TEMPLATE_DESCRIPTION;
import static org.sonar.server.permission.ws.Parameters.PARAM_TEMPLATE_KEY;
import static org.sonar.server.permission.ws.Parameters.PARAM_TEMPLATE_NAME;
import static org.sonar.server.permission.ws.Parameters.PARAM_TEMPLATE_PATTERN;
import static org.sonar.server.permission.ws.Parameters.createTemplateDescriptionParameter;
import static org.sonar.server.permission.ws.Parameters.createTemplateKeyParameter;
import static org.sonar.server.permission.ws.Parameters.createTemplateProjectKeyPatternParameter;
import static org.sonar.server.permission.ws.PermissionTemplateDtoToPermissionTemplateResponse.toPermissionTemplateResponse;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class UpdateTemplateAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final System2 system;
  private final PermissionDependenciesFinder finder;

  public UpdateTemplateAction(DbClient dbClient, UserSession userSession, System2 system, PermissionDependenciesFinder finder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.system = system;
    this.finder = finder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("update_template")
      .setDescription("Update a permission template.<br />" +
        "It requires administration permissions to access.")
      .setResponseExample(getClass().getResource("update_template-example.json"))
      .setSince("5.2")
      .setPost(true)
      .setHandler(this);

    createTemplateKeyParameter(action);

    action.createParam(PARAM_TEMPLATE_NAME)
      .setDescription("Name")
      .setExampleValue("Financial Service Permissions");

    createTemplateProjectKeyPatternParameter(action);
    createTemplateDescriptionParameter(action);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    checkGlobalAdminUser(userSession);

    String key = wsRequest.mandatoryParam(PARAM_TEMPLATE_KEY);
    String nameParam = wsRequest.param(PARAM_TEMPLATE_NAME);
    String descriptionParam = wsRequest.param(PARAM_TEMPLATE_DESCRIPTION);
    String projectPatternParam = wsRequest.param(PARAM_TEMPLATE_PATTERN);

    DbSession dbSession = dbClient.openSession(false);
    try {
      PermissionTemplateDto templateToUpdate = getAndBuildTemplateToUpdate(dbSession, key, nameParam, descriptionParam, projectPatternParam);
      validateTemplate(dbSession, templateToUpdate);
      PermissionTemplateDto updatedTemplate = updateTemplate(dbSession, templateToUpdate);

      UpdatePermissionTemplateResponse response = buildResponse(updatedTemplate);
      writeProtobuf(response, wsRequest, wsResponse);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private void validateTemplate(DbSession dbSession, PermissionTemplateDto templateToUpdate) {
    validateTemplateNameForUpdate(dbSession, templateToUpdate.getName(), templateToUpdate.getId());
    validateProjectPattern(templateToUpdate.getKeyPattern());
  }

  private PermissionTemplateDto getAndBuildTemplateToUpdate(DbSession dbSession, String key, @Nullable String newName, @Nullable String newDescription,
    @Nullable String newProjectKeyPattern) {
    PermissionTemplateDto templateToUpdate = finder.getTemplate(dbSession, key);
    templateToUpdate.setName(firstNonNull(newName, templateToUpdate.getName()));
    templateToUpdate.setDescription(firstNonNull(newDescription, templateToUpdate.getDescription()));
    templateToUpdate.setKeyPattern(firstNonNull(newProjectKeyPattern, templateToUpdate.getKeyPattern()));
    templateToUpdate.setUpdatedAt(new Date(system.now()));

    return templateToUpdate;
  }

  private PermissionTemplateDto updateTemplate(DbSession dbSession, PermissionTemplateDto templateToUpdate) {
    return dbClient.permissionTemplateDao().update(dbSession, templateToUpdate);
  }

  private static UpdatePermissionTemplateResponse buildResponse(PermissionTemplateDto permissionTemplate) {
    PermissionTemplate permissionTemplateBuilder = toPermissionTemplateResponse(permissionTemplate);
    return UpdatePermissionTemplateResponse.newBuilder().setPermissionTemplate(permissionTemplateBuilder).build();
  }

  private void validateTemplateNameForUpdate(DbSession dbSession, String templateName, long templateId) {
    checkRequest(!templateName.isEmpty(), MSG_TEMPLATE_NAME_NOT_BLANK);

    PermissionTemplateDto permissionTemplateWithSameName = dbClient.permissionTemplateDao().selectByName(dbSession, templateName);
    checkRequest(permissionTemplateWithSameName == null || permissionTemplateWithSameName.getId() == templateId,
      format(MSG_TEMPLATE_WITH_SAME_NAME, templateName));
  }
}
