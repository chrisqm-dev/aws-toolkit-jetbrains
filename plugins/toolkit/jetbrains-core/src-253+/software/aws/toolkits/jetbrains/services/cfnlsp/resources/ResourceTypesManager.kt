// Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cfnlsp.resources

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import software.aws.toolkit.core.utils.getLogger
import software.aws.toolkit.core.utils.info
import software.aws.toolkit.core.utils.warn
import software.aws.toolkits.jetbrains.services.cfnlsp.CfnLspServer
import software.aws.toolkits.jetbrains.services.cfnlsp.LspServerProvider
import software.aws.toolkits.jetbrains.services.cfnlsp.defaultLspServerProvider

@Service(Service.Level.PROJECT)
@State(name = "cfnResourceTypes", storages = [Storage("aws.xml", roamingType = RoamingType.DISABLED)])
internal class ResourceTypesManager(private val project: Project) : PersistentStateComponent<ResourceTypesManager.State> {
    internal var lspServerProvider: LspServerProvider = defaultLspServerProvider(project)
    
    private var state = State()
    private var availableTypes: List<String> = emptyList()
    private val listeners = mutableListOf<ResourceTypesChangeListener>()

    override fun getState(): State = state
    override fun loadState(state: State) { this.state = state }

    fun addListener(listener: ResourceTypesChangeListener) {
        listeners.add(listener)
    }

    fun getAvailableResourceTypes(): List<String> = availableTypes.toList()

    fun getSelectedResourceTypes(): Set<String> = state.selectedTypes.toSet()

    fun addResourceType(typeName: String) {
        if (typeName !in state.selectedTypes) {
            state.selectedTypes.add(typeName)
            notifyListeners()
        }
    }

    fun removeResourceType(typeName: String) {
        if (state.selectedTypes.remove(typeName)) {
            notifyListeners()
        }
    }

    fun loadAvailableTypes() {
        val server = lspServerProvider.getServer() ?: return

        LOG.info { "Loading available resource types" }
        server.sendNotification { lsp ->
            val cfnServer = lsp as? CfnLspServer ?: return@sendNotification
            cfnServer.listResourceTypes()
                .whenComplete { result, error ->
                    if (error != null) {
                        LOG.warn(error) { "Failed to load resource types" }
                    } else if (result != null) {
                        LOG.info { "Loaded ${result.resourceTypes.size} resource types" }
                        availableTypes = result.resourceTypes
                        notifyListeners()
                    }
                }
        }
    }

    private fun notifyListeners() {
        listeners.forEach { it() }
    }

    data class State(
        var selectedTypes: MutableSet<String> = mutableSetOf()
    )

    companion object {
        private val LOG = getLogger<ResourceTypesManager>()
        fun getInstance(project: Project): ResourceTypesManager = project.service()
    }
}

typealias ResourceTypesChangeListener = () -> Unit
