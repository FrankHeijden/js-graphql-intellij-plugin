// This is a generated file. Not intended for manual editing.
package com.intellij.lang.jsgraphql.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.lang.jsgraphql.psi.impl.GraphQLDirectivesAware;
import com.intellij.lang.jsgraphql.psi.impl.GraphQLTypeNameExtensionOwner;

public interface GraphQLInputObjectTypeExtensionDefinition extends GraphQLTypeExtension, GraphQLDirectivesAware, GraphQLTypeNameExtensionOwner {

  @Nullable
  GraphQLInputObjectValueDefinitions getInputObjectValueDefinitions();

  @Nullable
  GraphQLTypeName getTypeName();

  @NotNull
  List<GraphQLDirective> getDirectives();

}
