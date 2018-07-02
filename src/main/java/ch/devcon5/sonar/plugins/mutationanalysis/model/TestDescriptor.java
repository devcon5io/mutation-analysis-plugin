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

package ch.devcon5.sonar.plugins.mutationanalysis.model;

import java.util.Objects;

public class TestDescriptor {

   private final String className;
   private final String methodName;
   private final String spec;

   public TestDescriptor(final String spec) {
      this.spec = spec;

      final int parenthesis = spec.indexOf('(');
      final int methodSeparator = spec.lastIndexOf('.', parenthesis);
      final int nestedSeparator = spec.indexOf('$');

      if(nestedSeparator != -1){
         this.className = spec.substring(0, nestedSeparator);
         if(methodSeparator != -1){
            this.methodName = spec.substring(methodSeparator + 1, parenthesis);
         } else {
            this.methodName = "unknown";
         }
      } else
      if(methodSeparator != -1){
         this.className = spec.substring(0, methodSeparator);
         this.methodName = spec.substring(methodSeparator + 1, parenthesis);

      } else {
         className = spec;
         methodName = "unknown";
      }

   }

   public String getClassName() {
      return className;
   }

   public String getMethodName() {
      return methodName;
   }

   public String getSpec() {
      return spec;
   }

   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder("TestDescriptor{");
      sb.append("class='").append(className).append("', method='").append(methodName);
      sb.append("'}");
      return sb.toString();
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      final TestDescriptor that = (TestDescriptor) o;
      return Objects.equals(className, that.className);
   }

   @Override
   public int hashCode() {

      return Objects.hash(className);
   }
}
