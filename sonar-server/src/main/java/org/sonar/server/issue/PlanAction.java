/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.server.issue;

import com.google.common.base.Strings;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.condition.IsUnResolved;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.server.user.UserSession;

import java.util.List;
import java.util.Map;


public class PlanAction extends Action implements ServerComponent {

  public static final String KEY = "plan";

  private final ActionPlanService actionPlanService;
  private final IssueUpdater issueUpdater;

  public PlanAction(ActionPlanService actionPlanService, IssueUpdater issueUpdater) {
    super(KEY);
    this.actionPlanService = actionPlanService;
    this.issueUpdater = issueUpdater;
    super.setConditions(new IsUnResolved());
  }

  @Override
  public boolean verify(Map<String, Object> properties, List<Issue> issues, UserSession userSession) {
    String actionPlanKey = planKey(properties);
    if (!Strings.isNullOrEmpty(actionPlanKey)) {
      ActionPlan actionPlan = actionPlanService.findByKey(actionPlanKey, userSession);
      if (actionPlan == null) {
        throw new IllegalArgumentException("Unknown action plan: " + actionPlanKey);
      }
      verifyIssuesAreAllRelatedOnActionPlanProject(issues, actionPlan);
    }
    return true;
  }

  @Override
  public boolean execute(Map<String, Object> properties, Context context) {
    return issueUpdater.plan((DefaultIssue) context.issue(), planKey(properties), context.issueChangeContext());
  }

  private String planKey(Map<String, Object> properties) {
    return (String) properties.get("plan");
  }

  private void verifyIssuesAreAllRelatedOnActionPlanProject(List<Issue> issues, ActionPlan actionPlan) {
    String projectKey = actionPlan.projectKey();
    for (Issue issue : issues) {
      DefaultIssue defaultIssue = (DefaultIssue) issue;
      String issueProjectKey = defaultIssue.projectKey();
      if (issueProjectKey == null || !issueProjectKey.equals(projectKey)) {
        throw new IllegalArgumentException("Issues are not all related to the action plan project: " + projectKey);
      }
    }
  }

}