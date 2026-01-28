// Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cfnlsp.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CheckBoxList
import com.intellij.ui.components.JBScrollPane
import software.aws.toolkits.resources.AwsToolkitBundle.message
import java.awt.Dimension
import javax.swing.JComponent

internal class ResourceTypeSelectionDialog(
    project: Project,
    private val availableTypes: List<String>,
    private val selectedTypes: Set<String> = emptySet()
) : DialogWrapper(project) {
    
    var selectedResourceTypes: List<String> = emptyList()
        private set
    
    private val typesList = CheckBoxList<String>()
    
    init {
        title = message("cloudformation.resources.dialog.title")
        init()
        setupList()
    }
    
    private fun setupList() {
        val unselectedTypes = availableTypes.filter { it !in selectedTypes }
        unselectedTypes.forEach { type ->
            typesList.addItem(type, type, false)
        }
    }
    
    override fun createCenterPanel(): JComponent {
        return JBScrollPane(typesList).apply {
            preferredSize = Dimension(400, 300)
        }
    }
    
    override fun doOKAction() {
        selectedResourceTypes = typesList.getCheckedItems()
        super.doOKAction()
    }
}
