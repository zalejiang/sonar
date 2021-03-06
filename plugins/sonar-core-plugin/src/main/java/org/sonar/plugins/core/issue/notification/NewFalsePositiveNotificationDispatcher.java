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

package org.sonar.plugins.core.issue.notification;

import com.google.common.base.Objects;
import com.google.common.collect.Multimap;
import org.sonar.api.issue.Issue;
import org.sonar.api.notifications.*;

import java.util.Collection;
import java.util.Map;

/**
 * This dispatcher means: "notify me when someone resolves an issue as false positive".
 *
 * @since 3.6
 */
public class NewFalsePositiveNotificationDispatcher extends NotificationDispatcher {

  public static final String KEY = "NewFalsePositiveIssue";

  private final NotificationManager notifications;

  public NewFalsePositiveNotificationDispatcher(NotificationManager notifications) {
    super("issue-changes");
    this.notifications = notifications;
  }

  @Override
  public String getKey() {
    return KEY;
  }

  public static NotificationDispatcherMetadata newMetadata() {
    return NotificationDispatcherMetadata.create(KEY)
      .setProperty(NotificationDispatcherMetadata.GLOBAL_NOTIFICATION, String.valueOf(true))
      .setProperty(NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION, String.valueOf(true));
  }

  @Override
  public void dispatch(Notification notification, Context context) {
    String newResolution = notification.getFieldValue("new.resolution");
    if (Objects.equal(newResolution, Issue.RESOLUTION_FALSE_POSITIVE)) {
      String author = notification.getFieldValue("changeAuthor");
      String projectKey = notification.getFieldValue("projectKey");
      Multimap<String, NotificationChannel> subscribedRecipients = notifications.findNotificationSubscribers(this, projectKey);
      notify(author, context, subscribedRecipients);
    }
  }

  private void notify(String author, Context context, Multimap<String, NotificationChannel> subscribedRecipients) {
    for (Map.Entry<String, Collection<NotificationChannel>> channelsByRecipients : subscribedRecipients.asMap().entrySet()) {
      String login = channelsByRecipients.getKey();
      // Do not notify the person that resolved the issue
      if (!Objects.equal(author, login)) {
        for (NotificationChannel channel : channelsByRecipients.getValue()) {
          context.addUser(login, channel);
        }
      }
    }
  }

}
