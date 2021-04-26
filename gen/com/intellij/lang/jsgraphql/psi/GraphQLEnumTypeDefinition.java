// This is a generated file. Not intended for manual editing.
package com.intellij.lang.jsgraphql.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.lang.jsgraphql.psi.impl.GraphQLDirectivesAware;
import  com.intellij.lang.jsgraphql.psi.impl.GraphQLTypeNameDefinitionOwner;

public interface GraphQLEnumTypeDefinition extends GraphQLTypeDefinition, GraphQLDirectivesAware, GraphQLTypeNameDefinitionOwner {

  @Nullable
  GraphQLDescription getDescription();

  @Nullable
  GraphQLEnumValueDefinitions getEnumValueDefinitions();

  @Nullable
  GraphQLTypeNameDefinition getTypeNameDefinition();

  @NotNull
  List<GraphQLDirective> getDirectives();

}
