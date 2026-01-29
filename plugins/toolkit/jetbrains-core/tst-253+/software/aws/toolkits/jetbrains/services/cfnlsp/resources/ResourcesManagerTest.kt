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
        val mockLspServer = mock<LspServer>()
        val manager = ResourcesManager(projectRule.project)
        manager.lspServerProvider = LspServerProvider { null }

        manager.loadMoreResources("AWS::EC2::Instance")

        verify(mockLspServer, never()).sendNotification(any())
    }
}
