// Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cfnlsp.resources

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import software.aws.toolkit.core.utils.getLogger
import software.aws.toolkit.core.utils.info
import software.aws.toolkit.core.utils.warn
import software.aws.toolkits.jetbrains.services.cfnlsp.CfnLspServer
import software.aws.toolkits.jetbrains.services.cfnlsp.LspServerProvider
import software.aws.toolkits.jetbrains.services.cfnlsp.defaultLspServerProvider
import software.aws.toolkits.jetbrains.services.cfnlsp.protocol.ListResourcesParams
import software.aws.toolkits.jetbrains.services.cfnlsp.protocol.ResourceRequest
import software.aws.toolkits.jetbrains.services.cfnlsp.protocol.ResourceSummary

typealias ResourcesChangeListener = (String, List<String>) -> Unit

@Service(Service.Level.PROJECT)
internal class ResourcesManager(private val project: Project) : Disposable {
    internal var lspServerProvider: LspServerProvider = defaultLspServerProvider(project)
    
    private val resourcesByType = mutableMapOf<String, ResourceTypeData>()
    private val listeners = mutableListOf<ResourcesChangeListener>()

    private data class ResourceTypeData(
        val resourceIdentifiers: List<String>,
        val nextToken: String? = null,
        val loaded: Boolean = false
    )

    fun addListener(listener: ResourcesChangeListener) {
        listeners.add(listener)
    }

    fun getResourceIdentifiers(resourceType: String): List<String> =
        resourcesByType[resourceType]?.resourceIdentifiers ?: emptyList()

    fun getCachedResources(resourceType: String): List<String>? =
        resourcesByType[resourceType]?.resourceIdentifiers

    fun hasMore(resourceType: String): Boolean =
        resourcesByType[resourceType]?.nextToken != null

    fun isLoaded(resourceType: String): Boolean =
        resourcesByType[resourceType]?.loaded ?: false

    fun reload(resourceType: String) {
        loadResources(resourceType, loadMore = false)
    }

    fun loadMoreResources(resourceType: String) {
        val currentData = resourcesByType[resourceType]
        if (currentData?.nextToken == null) return
        loadResources(resourceType, loadMore = true)
    }

    fun searchResource(resourceType: String, identifier: String): CompletableFuture<Boolean> {
        val server = lspServerProvider.getServer()
        if (server == null) {
            return CompletableFuture.completedFuture(false)
        }

        val params = ListResourcesParams(
            resources = listOf(ResourceRequest(resourceType, null))
        )

        return server.sendRequest { lsp ->
            val cfnServer = lsp as? CfnLspServer ?: return@sendRequest CompletableFuture.completedFuture(false)
            cfnServer.listResources(params)
        }.thenApply { result ->
            val resourceSummary = result?.resources?.firstOrNull { it.typeName == resourceType }
            resourceSummary?.resourceIdentifiers?.contains(identifier) ?: false
        }
    }

    fun clear(resourceType: String? = null) {
        if (resourceType != null) {
            resourcesByType.remove(resourceType)
            notifyListeners(resourceType, emptyList())
        } else {
            val types = resourcesByType.keys.toList()
            resourcesByType.clear()
            types.forEach { type ->
                notifyListeners(type, emptyList())
            }
        }
    }

    private fun loadResources(resourceType: String, loadMore: Boolean) {
        val server = lspServerProvider.getServer()
        if (server == null) {
            LOG.warn { "No LSP server found for loading resources" }
            return
        }

        LOG.info { "Loading resources for type $resourceType (loadMore=$loadMore)" }

        val currentData = resourcesByType[resourceType]
        val nextToken = if (loadMore) currentData?.nextToken else null

        server.sendNotification { lsp ->
            val cfnServer = lsp as? CfnLspServer
            if (cfnServer == null) {
                LOG.warn { "LSP server is not CfnLspServer: ${lsp::class.java}" }
                return@sendNotification
            }

            val params = ListResourcesParams(
                resources = listOf(ResourceRequest(resourceType, nextToken))
            )

            cfnServer.listResources(params)
                .whenComplete { result, error ->
                    if (error != null) {
                        LOG.warn(error) { "Failed to load resources for $resourceType" }
                        if (!loadMore) {
                            resourcesByType[resourceType] = ResourceTypeData(
                                resourceIdentifiers = emptyList(),
                                nextToken = null,
                                loaded = true
                            )
                        }
                    } else if (result != null) {
                        val resourceSummary = result.resources.firstOrNull { it.typeName == resourceType }
                        if (resourceSummary != null) {
                            LOG.info { "Loaded ${resourceSummary.resourceIdentifiers.size} resources for $resourceType" }
                            
                            val existingResources = if (loadMore) currentData?.resourceIdentifiers ?: emptyList() else emptyList()
                            val allResources = existingResources + resourceSummary.resourceIdentifiers

                            resourcesByType[resourceType] = ResourceTypeData(
                                resourceIdentifiers = allResources,
                                nextToken = resourceSummary.nextToken,
                                loaded = true
                            )
                            
                            notifyListeners(resourceType, allResources)
                        } else {
                            LOG.info { "No resources found for $resourceType" }
                            resourcesByType[resourceType] = ResourceTypeData(
                                resourceIdentifiers = emptyList(),
                                nextToken = null,
                                loaded = true
                            )
                            notifyListeners(resourceType, emptyList())
                        }
                    }
                }
        }
    }

    private fun notifyListeners(resourceType: String, resources: List<String>) {
        listeners.forEach { it(resourceType, resources) }
    }

    override fun dispose() {
        listeners.clear()
    }

    companion object {
        private val LOG = getLogger<ResourcesManager>()
        fun getInstance(project: Project): ResourcesManager = project.service()
    }
}
