package com.intellij.lang.jsgraphql.ide.config.scope

import com.intellij.lang.jsgraphql.ide.config.model.GraphQLProjectConfig
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope

class GraphQLConfigSchemaScope(
    project: Project,
    baseScope: GlobalSearchScope,
    config: GraphQLProjectConfig
) : GraphQLConfigScope(project, baseScope, config) {

    override fun match(file: VirtualFile): Boolean {
        if (!super.match(file)) {
            return false
        }
        if (config.isDefault && config.isRootEmpty) {
            return true
        }
        return config.matchesSchema(file)
    }
}
