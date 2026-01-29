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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

@Service(Service.Level.PROJECT)
@State(name = "cfnResourceTypes", storages = [Storage("aws.xml", roamingType = RoamingType.DISABLED)])
internal class ResourceTypesManager(private val project: Project) : PersistentStateComponent<ResourceTypesManager.State> {
    internal var lspServerProvider: LspServerProvider = defaultLspServerProvider(project)

    private var state = State()
    private var availableTypes: List<String> = emptyList()
    private var typesLoaded: Boolean = false
    private val listeners = mutableListOf<ResourceTypesChangeListener>()

    override fun getState(): State = state
    override fun loadState(state: State) { this.state = state }

    fun addListener(listener: ResourceTypesChangeListener) {
        listeners.add(listener)
    }

    fun getAvailableResourceTypes(): List<String> = availableTypes.toList()

    fun areTypesLoaded(): Boolean = typesLoaded

    fun getSelectedResourceTypes(): Set<String> = state.selectedTypes.toSet()

    fun addResourceType(typeName: String) {
        if (typeName !in state.selectedTypes) {
            state.selectedTypes.add(typeName)
            notifyListeners()
        }
    }

    fun removeResourceType(typeName: String) {
        if (typeName in state.selectedTypes) {
            val server = lspServerProvider.getServer()
            if (server != null) {
                LOG.info { "Removing resource type from LSP server: $typeName" }
                CoroutineScope(Dispatchers.IO).future {
                    try {
                        server.sendRequest { (it as CfnLspServer).removeResourceType(typeName) }
                        LOG.info { "Successfully removed resource type: $typeName" }
                        // Only remove from local state and notify if LSP call succeeded
                        state.selectedTypes.remove(typeName)
                        notifyListeners()
                    } catch (e: Exception) {
                        LOG.warn(e) { "Failed to remove resource type from LSP server: $typeName" }
                        // Don't remove from local state or notify if LSP call failed
                    }
                }
            } else {
                LOG.warn { "No LSP server available to remove resource type: $typeName" }
                // Don't remove if no server available
            }
        }
    }

    fun loadAvailableTypes(): CompletableFuture<Unit> {
        val server = lspServerProvider.getServer()
        if (server == null) {
            return CompletableFuture.completedFuture(Unit)
        }

        LOG.info { "Loading available resource types" }
        
        return CoroutineScope(Dispatchers.IO).future {
            val result = server.sendRequest { (it as CfnLspServer).listResourceTypes() }
            
            if (result != null) {
                LOG.info { "Loaded ${result.resourceTypes.size} resource types" }
                availableTypes = result.resourceTypes
                typesLoaded = true
            } else {
                LOG.warn { "Failed to load resource types - null result" }
            }
        }
    }

    private fun notifyListeners() {
        listeners.forEach { it() }
    }

    data class State(
        var selectedTypes: MutableSet<String> = mutableSetOf(),
    )

    companion object {
        private val LOG = getLogger<ResourceTypesManager>()
        fun getInstance(project: Project): ResourceTypesManager = project.service()
    }
}

typealias ResourceTypesChangeListener = () -> Unit
