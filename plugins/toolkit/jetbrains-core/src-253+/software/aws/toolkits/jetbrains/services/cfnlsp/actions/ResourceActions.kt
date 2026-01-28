// Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cfnlsp.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import software.aws.toolkits.jetbrains.services.cfnlsp.resources.ResourceTypesManager
import software.aws.toolkits.jetbrains.services.cfnlsp.ui.ResourceTypeSelectionDialog
import software.aws.toolkits.resources.AwsToolkitBundle.message

class AddResourceTypeAction : AnAction(
    message("cloudformation.resources.add_type"),
    null,
    AllIcons.General.Add
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val resourceTypesManager = ResourceTypesManager.getInstance(project)
        
        // Load available types if not already loaded
        resourceTypesManager.loadAvailableTypes()
        
        val availableTypes = resourceTypesManager.getAvailableResourceTypes()
        val selectedTypes = resourceTypesManager.getSelectedResourceTypes()
        
        val unselectedTypes = availableTypes.filter { it !in selectedTypes }
        
        if (unselectedTypes.isEmpty()) {
            // TODO: Show message that all types are already added
            return
        }
        
        val dialog = ResourceTypeSelectionDialog(project, unselectedTypes, selectedTypes)
        if (dialog.showAndGet()) {
            dialog.selectedResourceTypes.forEach { type ->
                resourceTypesManager.addResourceType(type)
            }
        }
    }
}

class RemoveResourceTypeAction : AnAction(
    message("cloudformation.resources.remove_type"),
    null,
    AllIcons.General.Remove
) {
    override fun actionPerformed(e: AnActionEvent) {
        // TODO: Implement remove resource type
    }
}

class RefreshResourcesAction : AnAction(
    message("cloudformation.resources.refresh"),
    null,
    AllIcons.Actions.Refresh
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val resourcesManager = software.aws.toolkits.jetbrains.services.cfnlsp.resources.ResourcesManager.getInstance(project)
        resourcesManager.clear()
    }
}

class SearchResourceAction : AnAction(
    message("cloudformation.resources.search"),
    null,
    AllIcons.Actions.Search
) {
    override fun actionPerformed(e: AnActionEvent) {
        // TODO: Implement search resource
    }
}
