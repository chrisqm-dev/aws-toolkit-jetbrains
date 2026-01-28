// Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cfnlsp.explorer.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import icons.AwsIcons
import software.aws.toolkits.jetbrains.core.explorer.nodes.ActionGroupOnRightClick
import software.aws.toolkits.jetbrains.services.cfnlsp.resources.ResourcesManager

internal class ResourceTypeNode(
    nodeProject: Project,
    private val resourceType: String,
    private val resourcesManager: ResourcesManager
) : AbstractTreeNode<String>(nodeProject, resourceType), ActionGroupOnRightClick {
    
    override fun actionGroupName(): String = "aws.toolkit.cloudformation.resources.type.actions"
    
    override fun update(presentation: PresentationData) {
        presentation.addText(resourceType, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        presentation.setIcon(AwsIcons.Resources.CLOUDFORMATION_STACK)
        
        val resources = resourcesManager.getCachedResources(resourceType)
        if (resources != null) {
            presentation.addText(" (${resources.size})", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        }
    }
    
    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        if (!resourcesManager.isLoaded(resourceType)) {
            resourcesManager.reload(resourceType)
            return emptyList()
        }
        
        val resources = resourcesManager.getResourceIdentifiers(resourceType)
        
        if (resources.isEmpty()) {
            return listOf(NoResourcesNode(project, resourceType))
        }
        
        val nodes = resources.map { identifier ->
            ResourceNode(project, resourceType, identifier)
        }
        
        return if (resourcesManager.hasMore(resourceType)) {
            nodes + LoadMoreResourcesNode(project, resourceType, resourcesManager)
        } else {
            nodes
        }
    }
}

internal class NoResourcesNode(
    nodeProject: Project,
    private val resourceType: String
) : AbstractTreeNode<String>(nodeProject, "no-resources") {
    
    override fun update(presentation: PresentationData) {
        presentation.addText("No $resourceType resources found", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        presentation.setIcon(AwsIcons.General.SETTINGS)
    }
    
    override fun getChildren(): Collection<AbstractTreeNode<*>> = emptyList()
    override fun isAlwaysLeaf(): Boolean = true
}

internal class LoadMoreResourcesNode(
    nodeProject: Project,
    private val resourceType: String,
    private val resourcesManager: ResourcesManager
) : AbstractTreeNode<String>(nodeProject, "load-more") {
    
    override fun update(presentation: PresentationData) {
        presentation.addText("Load More...", SimpleTextAttributes.LINK_ATTRIBUTES)
        presentation.setIcon(AwsIcons.General.REFRESH)
    }
    
    override fun onDoubleClick(): Boolean {
        resourcesManager.loadMoreResources(resourceType)
        return true
    }
    
    override fun getChildren(): Collection<AbstractTreeNode<*>> = emptyList()
    override fun isAlwaysLeaf(): Boolean = true
}

internal class ResourceNode(
    nodeProject: Project,
    private val resourceType: String,
    private val resourceIdentifier: String
) : AbstractTreeNode<String>(nodeProject, resourceIdentifier), ActionGroupOnRightClick {
    
    override fun actionGroupName(): String = "aws.toolkit.cloudformation.resources.resource.actions"
    
    override fun update(presentation: PresentationData) {
        presentation.addText(resourceIdentifier, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        presentation.setIcon(AwsIcons.Resources.GENERIC)
        presentation.tooltip = "$resourceType: $resourceIdentifier"
    }
    
    override fun getChildren(): Collection<AbstractTreeNode<*>> = emptyList()
    override fun isAlwaysLeaf(): Boolean = true
}

internal class AddResourceTypeNode(
    nodeProject: Project,
    private val resourceTypesManager: software.aws.toolkits.jetbrains.services.cfnlsp.resources.ResourceTypesManager
) : AbstractTreeNode<String>(nodeProject, "add-resource-type") {
    
    override fun update(presentation: PresentationData) {
        presentation.addText("Add Resource Type...", SimpleTextAttributes.LINK_ATTRIBUTES)
        presentation.setIcon(AwsIcons.General.ADD)
    }
    
    override fun onDoubleClick(): Boolean {
        val resourceTypesManager = this.resourceTypesManager
        
        // Load available types if not already loaded
        resourceTypesManager.loadAvailableTypes()
        
        val availableTypes = resourceTypesManager.getAvailableResourceTypes()
        val selectedTypes = resourceTypesManager.getSelectedResourceTypes()
        
        val unselectedTypes = availableTypes.filter { it !in selectedTypes }
        
        if (unselectedTypes.isEmpty()) {
            // TODO: Show message that all types are already added
            return true
        }
        
        val dialog = software.aws.toolkits.jetbrains.services.cfnlsp.ui.ResourceTypeSelectionDialog(project, unselectedTypes, selectedTypes)
        if (dialog.showAndGet()) {
            dialog.selectedResourceTypes.forEach { type ->
                resourceTypesManager.addResourceType(type)
            }
        }
        return true
    }
    
    override fun getChildren(): Collection<AbstractTreeNode<*>> = emptyList()
    override fun isAlwaysLeaf(): Boolean = true
}
