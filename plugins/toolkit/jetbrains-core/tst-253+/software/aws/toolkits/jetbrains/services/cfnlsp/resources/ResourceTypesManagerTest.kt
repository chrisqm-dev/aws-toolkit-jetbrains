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
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.aws.toolkits.jetbrains.services.cfnlsp.CfnLspServer
import software.aws.toolkits.jetbrains.services.cfnlsp.LspServerProvider
import software.aws.toolkits.jetbrains.services.cfnlsp.protocol.ResourceTypesResult
import java.util.concurrent.CompletableFuture

class ResourceTypesManagerTest {

    @get:Rule
    val projectRule = ProjectRule()

    @Test
    fun `initially has no selected resource types`() {
        val manager = ResourceTypesManager(projectRule.project)

        assertThat(manager.getSelectedResourceTypes()).isEmpty()
        assertThat(manager.areTypesLoaded()).isFalse()
        assertThat(manager.getAvailableResourceTypes()).isEmpty()
    }

    @Test
    fun `can add resource types`() {
        val manager = ResourceTypesManager(projectRule.project)

        manager.addResourceType("AWS::EC2::Instance")
        assertThat(manager.getSelectedResourceTypes()).containsExactly("AWS::EC2::Instance")

        manager.addResourceType("AWS::S3::Bucket")
        assertThat(manager.getSelectedResourceTypes()).containsExactlyInAnyOrder(
            "AWS::EC2::Instance",
            "AWS::S3::Bucket"
        )
    }

    @Test
    fun `loads available types from LSP server`() = runTest {
        val mockCfnServer = mock<CfnLspServer>()
        val mockLspServer = mock<LspServer>()
        val manager = ResourceTypesManager(projectRule.project, this)
        
        val mockResult = ResourceTypesResult(listOf("AWS::EC2::Instance", "AWS::S3::Bucket"))
        whenever(mockCfnServer.listResourceTypes()).thenReturn(CompletableFuture.completedFuture(mockResult))
        whenever(mockLspServer.sendRequest(any<(Any) -> CompletableFuture<Any>>())).thenAnswer { invocation ->
            val lambda = invocation.getArgument<(CfnLspServer) -> CompletableFuture<*>>(0)
            val future = lambda.invoke(mockCfnServer)
            future.get()
        }
        
        manager.lspServerProvider = LspServerProvider { mockLspServer }

        manager.loadAvailableTypes()
        testScheduler.advanceUntilIdle()

        verify(mockCfnServer).listResourceTypes()
        assertThat(manager.getAvailableResourceTypes()).containsExactlyInAnyOrder("AWS::EC2::Instance", "AWS::S3::Bucket")
        assertThat(manager.areTypesLoaded()).isTrue()
    }

    @Test
    fun `removeResourceType sends request to LSP server`() = runTest {
        val mockCfnServer = mock<CfnLspServer>()
        val mockLspServer = mock<LspServer>()
        val manager = ResourceTypesManager(projectRule.project, this)
        
        whenever(mockCfnServer.removeResourceType(any())).thenReturn(CompletableFuture.completedFuture(null))
        whenever(mockLspServer.sendRequest(any<(Any) -> CompletableFuture<Any>>())).thenAnswer { invocation ->
            val lambda = invocation.getArgument<(CfnLspServer) -> CompletableFuture<*>>(0)
            val future = lambda.invoke(mockCfnServer)
            future.get()
        }
        
        manager.lspServerProvider = LspServerProvider { mockLspServer }
        
        manager.addResourceType("AWS::EC2::Instance")
        assertThat(manager.getSelectedResourceTypes()).containsExactly("AWS::EC2::Instance")

        manager.removeResourceType("AWS::EC2::Instance")
        testScheduler.advanceUntilIdle()

        verify(mockCfnServer).removeResourceType("AWS::EC2::Instance")
        assertThat(manager.getSelectedResourceTypes()).isEmpty()
    }

    @Test
    fun `adding duplicate resource type is ignored`() {
        val manager = ResourceTypesManager(projectRule.project)

        manager.addResourceType("AWS::EC2::Instance")
        manager.addResourceType("AWS::EC2::Instance")

        assertThat(manager.getSelectedResourceTypes()).containsExactly("AWS::EC2::Instance")
    }

    @Test
    fun `removeResourceType handles LSP server exception gracefully`() = runTest {
        val mockCfnServer = mock<CfnLspServer>()
        val mockLspServer = mock<LspServer>()
        val manager = ResourceTypesManager(projectRule.project, this)
        
        whenever(mockCfnServer.removeResourceType(any())).thenReturn(CompletableFuture.failedFuture(RuntimeException("Test exception")))
        whenever(mockLspServer.sendRequest(any<(Any) -> CompletableFuture<Any>>())).thenAnswer { invocation ->
            val lambda = invocation.getArgument<(CfnLspServer) -> CompletableFuture<*>>(0)
            val future = lambda.invoke(mockCfnServer)
            try {
                future.get()
            } catch (e: Exception) {
                throw e.cause ?: e
            }
        }
        
        manager.lspServerProvider = LspServerProvider { mockLspServer }
        
        manager.addResourceType("AWS::EC2::Instance")
        assertThat(manager.getSelectedResourceTypes()).containsExactly("AWS::EC2::Instance")

        manager.removeResourceType("AWS::EC2::Instance")
        testScheduler.advanceUntilIdle()

        verify(mockCfnServer).removeResourceType("AWS::EC2::Instance")
        // Should not remove from state when LSP call fails
        assertThat(manager.getSelectedResourceTypes()).containsExactly("AWS::EC2::Instance")
    }

    @Test
    fun `removeResourceType does nothing for non-existent type`() = runTest {
        val mockLspServer = mock<LspServer>()
        val manager = ResourceTypesManager(projectRule.project, this)
        manager.lspServerProvider = LspServerProvider { mockLspServer }

        manager.removeResourceType("AWS::EC2::Instance")
        testScheduler.advanceUntilIdle()

        verify(mockLspServer, never()).sendRequest(any<(Any) -> CompletableFuture<Any>>())
        assertThat(manager.getSelectedResourceTypes()).isEmpty()
    }

    @Test
    fun `loadAvailableTypes handles null result gracefully`() = runTest {
        val mockCfnServer = mock<CfnLspServer>()
        val mockLspServer = mock<LspServer>()
        val manager = ResourceTypesManager(projectRule.project, this)
        
        whenever(mockCfnServer.listResourceTypes()).thenReturn(CompletableFuture.completedFuture(null))
        whenever(mockLspServer.sendRequest(any<(Any) -> CompletableFuture<Any>>())).thenAnswer { invocation ->
            val lambda = invocation.getArgument<(CfnLspServer) -> CompletableFuture<*>>(0)
            val future = lambda.invoke(mockCfnServer)
            future.get()
        }
        
        manager.lspServerProvider = LspServerProvider { mockLspServer }

        manager.loadAvailableTypes()
        testScheduler.advanceUntilIdle()

        verify(mockCfnServer).listResourceTypes()
        assertThat(manager.getAvailableResourceTypes()).isEmpty()
        assertThat(manager.areTypesLoaded()).isFalse()
    }

    @Test
    fun `listeners are notified when resource types change`() {
        val manager = ResourceTypesManager(projectRule.project)
        var notificationCount = 0
        val listener: ResourceTypesChangeListener = { notificationCount++ }

        manager.addListener(listener)

        manager.addResourceType("AWS::EC2::Instance")
        assertThat(notificationCount).isEqualTo(1)

        manager.addResourceType("AWS::S3::Bucket")
        assertThat(notificationCount).isEqualTo(2)

        // Adding duplicate should not notify
        manager.addResourceType("AWS::EC2::Instance")
        assertThat(notificationCount).isEqualTo(2)
    }

    @Test
    fun `listeners are notified when resource types are removed successfully`() = runTest {
        val mockCfnServer = mock<CfnLspServer>()
        val mockLspServer = mock<LspServer>()
        val manager = ResourceTypesManager(projectRule.project, this)
        
        whenever(mockCfnServer.removeResourceType(any())).thenReturn(CompletableFuture.completedFuture(null))
        whenever(mockLspServer.sendRequest(any<(Any) -> CompletableFuture<Any>>())).thenAnswer { invocation ->
            val lambda = invocation.getArgument<(CfnLspServer) -> CompletableFuture<*>>(0)
            val future = lambda.invoke(mockCfnServer)
            future.get()
        }
        
        manager.lspServerProvider = LspServerProvider { mockLspServer }
        
        var notificationCount = 0
        val listener: ResourceTypesChangeListener = { notificationCount++ }
        manager.addListener(listener)

        manager.addResourceType("AWS::EC2::Instance")
        assertThat(notificationCount).isEqualTo(1)

        manager.removeResourceType("AWS::EC2::Instance")
        testScheduler.advanceUntilIdle()

        assertThat(notificationCount).isEqualTo(2)
    }

    @Test
    fun `listeners are not notified when resource type removal fails`() = runTest {
        val mockCfnServer = mock<CfnLspServer>()
        val mockLspServer = mock<LspServer>()
        val manager = ResourceTypesManager(projectRule.project, this)
        
        whenever(mockCfnServer.removeResourceType(any())).thenReturn(CompletableFuture.failedFuture(RuntimeException("Test exception")))
        whenever(mockLspServer.sendRequest(any<(Any) -> CompletableFuture<Any>>())).thenAnswer { invocation ->
            val lambda = invocation.getArgument<(CfnLspServer) -> CompletableFuture<*>>(0)
            val future = lambda.invoke(mockCfnServer)
            try {
                future.get()
            } catch (e: Exception) {
                throw e.cause ?: e
            }
        }
        
        manager.lspServerProvider = LspServerProvider { mockLspServer }
        
        var notificationCount = 0
        val listener: ResourceTypesChangeListener = { notificationCount++ }
        manager.addListener(listener)

        manager.addResourceType("AWS::EC2::Instance")
        assertThat(notificationCount).isEqualTo(1)

        manager.removeResourceType("AWS::EC2::Instance")
        testScheduler.advanceUntilIdle()

        // Should not notify when removal fails
        assertThat(notificationCount).isEqualTo(1)
    }

    @Test
    fun `state persistence works correctly`() {
        val manager = ResourceTypesManager(projectRule.project)
        
        manager.addResourceType("AWS::EC2::Instance")
        manager.addResourceType("AWS::S3::Bucket")
        
        val state = manager.state
        assertThat(state.selectedTypes).containsExactlyInAnyOrder("AWS::EC2::Instance", "AWS::S3::Bucket")
        
        // Simulate loading state
        val newManager = ResourceTypesManager(projectRule.project)
        newManager.loadState(state)
        
        assertThat(newManager.getSelectedResourceTypes()).containsExactlyInAnyOrder("AWS::EC2::Instance", "AWS::S3::Bucket")
    }
}
