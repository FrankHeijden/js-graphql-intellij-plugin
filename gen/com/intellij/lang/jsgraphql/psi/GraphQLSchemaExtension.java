// This is a generated file. Not intended for manual editing.
package com.intellij.lang.jsgraphql.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.lang.jsgraphql.psi.impl.GraphQLDirectivesAware;

public interface GraphQLSchemaExtension extends GraphQLTypeSystemDefinition, GraphQLDirectivesAware {

  @Nullable
  GraphQLOperationTypeDefinitions getOperationTypeDefinitions();

  @NotNull
  List<GraphQLDirective> getDirectives();

}
