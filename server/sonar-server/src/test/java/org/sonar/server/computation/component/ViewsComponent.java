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
package org.sonar.server.computation.component;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * Implementation of {@link Component} to unit test views components.
 */
public class ViewsComponent implements Component {
  private final Type type;
  private final String key;
  @CheckForNull
  private final String uuid;
  @CheckForNull
  private final String name;
  private final List<Component> children;
  @CheckForNull
  private final ProjectViewAttributes projectViewAttributes;

  private ViewsComponent(Type type, String key, @Nullable String uuid, @Nullable String name,
    List<Component> children,
    @Nullable ProjectViewAttributes projectViewAttributes) {
    checkArgument(type.isViewsType(), "Component type must be a Views type");
    this.type = type;
    this.key = requireNonNull(key);
    this.uuid = uuid;
    this.name = name;
    this.children = ImmutableList.copyOf(children);
    this.projectViewAttributes = projectViewAttributes;
  }

  public static Builder builder(Type type, String key) {
    return new Builder(type, key);
  }

  public static Builder builder(Type type, int key) {
    return new Builder(type, String.valueOf(key));
  }

  public static final class Builder {
    private final Type type;
    private final String key;
    @CheckForNull
    private String uuid;
    @CheckForNull
    private String name;
    private List<Component> children = new ArrayList<>();
    @CheckForNull
    private ProjectViewAttributes projectViewAttributes;

    private Builder(Type type, String key) {
      this.type = type;
      this.key = key;
    }

    public Builder setUuid(@Nullable String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder setName(@Nullable String name) {
      this.name = name;
      return this;
    }

    public Builder setProjectViewAttributes(@Nullable ProjectViewAttributes projectViewAttributes) {
      this.projectViewAttributes = projectViewAttributes;
      return this;
    }

    public Builder addChildren(Component... c) {
      for (Component viewsComponent : c) {
        checkArgument(viewsComponent.getType().isViewsType());
      }
      this.children.addAll(asList(c));
      return this;
    }

    public ViewsComponent build() {
      return new ViewsComponent(type, key, uuid, name, children, projectViewAttributes);
    }
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public String getUuid() {
    return uuid;
  }

  @Override
  public String getKey() {
    return key;
  }

  @Override
  public String getName() {
    checkState(this.name != null, "No name has been set");
    return this.name;
  }

  @Override
  public List<Component> getChildren() {
    return children;
  }

  @Override
  public ReportAttributes getReportAttributes() {
    throw new IllegalStateException("A component of type " + type + " does not have report attributes");
  }

  @Override
  public FileAttributes getFileAttributes() {
    throw new IllegalStateException("A component of type " + type + " does not have file attributes");
  }

  @Override
  public ProjectViewAttributes getProjectViewAttributes() {
    checkState(this.type != Type.PROJECT_VIEW || this.projectViewAttributes != null, "A ProjectViewAttribute object should have been set");
    return this.projectViewAttributes;
  }

  @Override
  public String toString() {
    return "ViewsComponent{" +
      "type=" + type +
      ", key='" + key + '\'' +
      ", uuid='" + uuid + '\'' +
      ", name='" + name + '\'' +
      ", children=" + children +
      ", projectViewAttributes=" + projectViewAttributes +
      '}';
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ViewsComponent that = (ViewsComponent) o;
    return key.equals(that.key);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key);
  }
}
