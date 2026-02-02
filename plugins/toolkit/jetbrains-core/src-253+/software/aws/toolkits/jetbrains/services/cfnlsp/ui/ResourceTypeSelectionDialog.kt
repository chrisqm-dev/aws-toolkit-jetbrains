// Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cfnlsp.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CheckBoxList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import software.aws.toolkit.core.utils.getLogger
import software.aws.toolkit.core.utils.info
import software.aws.toolkits.resources.AwsToolkitBundle.message
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

internal class ResourceTypeSelectionDialog(
    project: Project,
    private val availableTypes: List<String>,
    private val selectedTypes: Set<String> = emptySet(),
) : DialogWrapper(project) {

    var selectedResourceTypes: List<String> = emptyList()
        private set

    private val typesList = CheckBoxList<String>()
    private val searchField = JBTextField()
    private val allUnselectedTypes = availableTypes.filter { it !in selectedTypes }

    init {
        title = message("cloudformation.resources.dialog.title")
        init()
        setupList()
        setupSearch()
    }

    private fun setupList() {
        allUnselectedTypes.forEach { type ->
            typesList.addItem(type, type, false)
        }

        LOG.info { "Set up list" }
    }

    private fun setupSearch() {
        searchField.emptyText.text = "Search resource types..."
        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = filterList()
            override fun removeUpdate(e: DocumentEvent?) = filterList()
            override fun changedUpdate(e: DocumentEvent?) = filterList()
        })

        LOG.info { "Set up search bar" }
    }

    private fun filterList() {
        val searchText = searchField.text.lowercase()
        typesList.clear()
        
        val filteredTypes = if (searchText.isEmpty()) {
            allUnselectedTypes
        } else {
            allUnselectedTypes.filter { it.lowercase().contains(searchText) }
        }
        
        filteredTypes.forEach { type ->
            typesList.addItem(type, type, false)
        }
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())

        panel.add(searchField, BorderLayout.NORTH)
        panel.add(JBScrollPane(typesList), BorderLayout.CENTER)
        panel.preferredSize = Dimension(400, 300)

        return panel
    }

    override fun doOKAction() {
        selectedResourceTypes = typesList.getCheckedItems()
        super.doOKAction()
    }

    companion object {
        private val LOG = getLogger<ResourceTypeSelectionDialog>()
    }
}
