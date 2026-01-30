// Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cfnlsp.resources

import com.intellij.platform.lsp.api.LspServer
import com.intellij.testFramework.ProjectRule
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.aws.toolkits.jetbrains.services.cfnlsp.LspServerProvider
import java.util.concurrent.CompletableFuture

class ResourcesManagerTest {

    @get:Rule
    val projectRule = ProjectRule()

    @Test
    fun `initially has no cached resources`() {
        val manager = ResourcesManager(projectRule.project)

        assertThat(manager.getCachedResources("AWS::EC2::Instance")).isNull()
        assertThat(manager.isLoaded("AWS::EC2::Instance")).isFalse()
    }

    @Test
    fun `reload sends request to LSP server`() = runTest {
        val mockLspServer = mock<LspServer>()
        val manager = ResourcesManager(projectRule.project, this)
        
        // Mock the sendRequest suspend function to return a result
        val mockResult = mock<software.aws.toolkits.jetbrains.services.cfnlsp.protocol.ListResourcesResult>()
        whenever(mockLspServer.sendRequest(any<(Any) -> CompletableFuture<Any>>())).thenReturn(mockResult)

        // Set the provider AFTER creating the manager
        manager.lspServerProvider = LspServerProvider { mockLspServer }

        // Clear any existing state
        manager.clear("AWS::EC2::Instance")

        manager.reload("AWS::EC2::Instance")
        
        // Wait for the coroutine to complete
        testScheduler.advanceUntilIdle()

        // Verify that sendRequest was called with a lambda function
        verify(mockLspServer).sendRequest(any<(Any) -> CompletableFuture<Any>>())
    }

    @Test
    fun `reload does nothing when no LSP server available`() {
        val manager = ResourcesManager(projectRule.project)
        manager.lspServerProvider = LspServerProvider { null }

        manager.reload("AWS::EC2::Instance")

        assertThat(manager.isLoaded("AWS::EC2::Instance")).isFalse()
    }

    @Test
    fun `clear resets state`() {
        val manager = ResourcesManager(projectRule.project)

        manager.clear("AWS::EC2::Instance")

        assertThat(manager.isLoaded("AWS::EC2::Instance")).isFalse()
        assertThat(manager.getCachedResources("AWS::EC2::Instance")).isNull()
    }

    @Test
    fun `loadMoreResources does nothing when no LSP server`() {
        val manager = ResourcesManager(projectRule.project)
        manager.lspServerProvider = LspServerProvider { null }

        // This should not throw an exception
        manager.loadMoreResources("AWS::EC2::Instance")

        // Verify state remains unchanged
        assertThat(manager.isLoaded("AWS::EC2::Instance")).isFalse()
    }

    @Test
    fun `searchResource returns completed future when no LSP server`() {
        val manager = ResourcesManager(projectRule.project)
        manager.lspServerProvider = LspServerProvider { null }

        val future = manager.searchResource("AWS::EC2::Instance", "testResource")

        // Should complete immediately with false when no server
        assertThat(future.isDone).isTrue()
        assertThat(future.get()).isFalse()
    }

    @Test
    fun `searchResource creates future when LSP server available`() = runTest {
        val mockLspServer = mock<LspServer>()
        val manager = ResourcesManager(projectRule.project, this)
        
        manager.lspServerProvider = LspServerProvider { mockLspServer }

        val future = manager.searchResource("AWS::EC2::Instance", "testResource")

        // The future should be created (may not be done yet due to async nature)
        assertThat(future).isNotNull()
    }

    @Test
    fun `searchResource adds found resource to cache`() = runTest {
        val mockLspServer = mock<LspServer>()
        val manager = ResourcesManager(projectRule.project, this)
        
        // Mock successful search result with resource data
        val mockResourceSummary = mock<software.aws.toolkits.jetbrains.services.cfnlsp.protocol.ResourceSummary>()
        val mockResult = mock<software.aws.toolkits.jetbrains.services.cfnlsp.protocol.SearchResourceResult>()
        whenever(mockResult.found).thenReturn(true)
        whenever(mockResult.resource).thenReturn(mockResourceSummary)
        whenever(mockLspServer.sendRequest(any<(Any) -> CompletableFuture<Any>>())).thenReturn(mockResult)

        manager.lspServerProvider = LspServerProvider { mockLspServer }

        // Initially no resources cached
        assertThat(manager.getCachedResources("AWS::EC2::Instance")).isNull()

        manager.searchResource("AWS::EC2::Instance", "testResource")
        testScheduler.advanceUntilIdle()

        // Resource should now be in cache
        val cachedResources = manager.getResourceIdentifiers("AWS::EC2::Instance")
        assertThat(cachedResources).contains("testResource")
        assertThat(manager.isLoaded("AWS::EC2::Instance")).isTrue()
    }
}
