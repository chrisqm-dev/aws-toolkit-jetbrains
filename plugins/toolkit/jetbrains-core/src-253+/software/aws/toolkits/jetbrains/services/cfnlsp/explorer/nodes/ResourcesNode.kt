// Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cfnlsp.explorer.nodes

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import software.aws.toolkits.jetbrains.core.explorer.devToolsTab.nodes.ActionGroupOnRightClick
import software.aws.toolkits.jetbrains.services.cfnlsp.resources.ResourceTypesManager
import software.aws.toolkits.jetbrains.services.cfnlsp.resources.ResourcesManager
import software.aws.toolkits.resources.AwsToolkitBundle.message

internal class ResourcesNode(
    nodeProject: Project,
    private val resourceTypesManager: ResourceTypesManager,
    private val resourcesManager: ResourcesManager,
) : AbstractTreeNode<String>(nodeProject, "resources"), ActionGroupOnRightClick {

    override fun actionGroupName(): String = "aws.toolkit.cloudformation.resources.actions"

    override fun update(presentation: PresentationData) {
        val selectedCount = resourceTypesManager.getSelectedResourceTypes().size
        val countText = if (selectedCount > 0) " ($selectedCount)" else ""
        presentation.addText(message("cloudformation.explorer.resources"), SimpleTextAttributes.REGULAR_ATTRIBUTES)
        presentation.addText(countText, SimpleTextAttributes.GRAY_ATTRIBUTES)
        presentation.setIcon(AllIcons.Nodes.Folder)
    }

    override fun isAlwaysShowPlus(): Boolean = true

    override fun getChildren(): Collection<AbstractTreeNode<*>> {
        val nodes = mutableListOf<AbstractTreeNode<*>>()

        nodes.add(AddResourceTypeNode(project, resourceTypesManager))

        resourceTypesManager.getSelectedResourceTypes().forEach { typeName ->
            nodes.add(ResourceTypeNode(project, typeName, resourcesManager))
        }

        return nodes
    }
}
