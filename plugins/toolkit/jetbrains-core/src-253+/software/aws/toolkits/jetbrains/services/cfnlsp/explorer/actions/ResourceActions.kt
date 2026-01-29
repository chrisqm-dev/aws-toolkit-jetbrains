// Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cfnlsp.explorer.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import software.aws.toolkit.core.utils.getLogger
import software.aws.toolkit.core.utils.info
import software.aws.toolkit.core.utils.warn
import software.aws.toolkits.jetbrains.core.explorer.ExplorerTreeToolWindowDataKeys
import software.aws.toolkits.jetbrains.services.cfnlsp.explorer.nodes.ResourceTypeNode
import software.aws.toolkits.jetbrains.services.cfnlsp.resources.ResourceTypesManager
import software.aws.toolkits.jetbrains.services.cfnlsp.resources.ResourcesManager
import software.aws.toolkits.resources.AwsToolkitBundle.message

class RemoveResourceTypeAction : AnAction(
    message("cloudformation.resources.remove_type"),
    null,
    AllIcons.General.Remove
) {
    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        // Only enable if a ResourceTypeNode is selected
        val selectedNodes = e.getData(ExplorerTreeToolWindowDataKeys.SELECTED_NODES)
        val hasResourceTypeNode = selectedNodes?.filterIsInstance<ResourceTypeNode>()?.isNotEmpty() == true
        e.presentation.isEnabled = hasResourceTypeNode
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val resourceTypesManager = ResourceTypesManager.getInstance(project)
        val resourcesManager = ResourcesManager.getInstance(project)
        
        // Get the selected ResourceTypeNode
        val selectedNodes = e.getData(ExplorerTreeToolWindowDataKeys.SELECTED_NODES)
        val resourceTypeNode = selectedNodes?.filterIsInstance<ResourceTypeNode>()?.firstOrNull() ?: return
        
        // Remove the resource type
        resourceTypesManager.removeResourceType(resourceTypeNode.resourceType)
        resourcesManager.clear(resourceTypeNode.resourceType)
    }
}

class RefreshResourceTypeAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        e.presentation.text = message("cloudformation.resources.refresh_type")
        e.presentation.icon = AllIcons.Actions.Refresh
    }

    override fun actionPerformed(e: AnActionEvent) {
        LOG.info { "RefreshResourceTypeAction triggered" }
        val project = e.project ?: return
        val resourcesManager = ResourcesManager.getInstance(project)
        
        // Get the selected nodes using the correct data key
        val selectedNodes = e.getData(ExplorerTreeToolWindowDataKeys.SELECTED_NODES)

        // Find ResourceTypeNode in the selection
        val resourceTypeNode = selectedNodes?.filterIsInstance<ResourceTypeNode>()?.firstOrNull()

        if (resourceTypeNode != null) {
            LOG.info { "Reloading resource type: ${resourceTypeNode.resourceType}" }
            resourcesManager.reload(resourceTypeNode.resourceType)
        } else {
            LOG.warn { "No ResourceTypeNode found in selection" }
        }
    }
    companion object {
        private val LOG = getLogger<RefreshResourceTypeAction>()
    }
}

class RefreshAllLoadedResourcesAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        e.presentation.text = message("cloudformation.resources.refresh_all_loaded")
        e.presentation.icon = AllIcons.Actions.Refresh
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val resourcesManager = ResourcesManager.getInstance(project)
        
        // Get all currently loaded resource types and reload them
        val loadedTypes = resourcesManager.getLoadedResourceTypes()
        loadedTypes.forEach { resourceType ->
            resourcesManager.reload(resourceType)
        }
    }
}
