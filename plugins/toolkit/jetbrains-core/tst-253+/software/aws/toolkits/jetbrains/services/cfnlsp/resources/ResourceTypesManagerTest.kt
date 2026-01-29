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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
        val mockLspServer = mock<LspServer>()
        val manager = ResourceTypesManager(projectRule.project, this)
        
        // Mock the sendRequest to return resource types
        val mockResult = ResourceTypesResult(listOf("AWS::EC2::Instance", "AWS::S3::Bucket"))
        whenever(mockLspServer.sendRequest(any<(Any) -> CompletableFuture<Any>>())).thenReturn(mockResult)
        
        manager.lspServerProvider = LspServerProvider { mockLspServer }

        manager.loadAvailableTypes()
        
        // Wait for the coroutine to complete
        testScheduler.advanceUntilIdle()

        // Verify that sendRequest was called
        verify(mockLspServer).sendRequest(any<(Any) -> CompletableFuture<Any>>())
        
        // Verify the types were loaded
        assertThat(manager.getAvailableResourceTypes()).containsExactlyInAnyOrder("AWS::EC2::Instance", "AWS::S3::Bucket")
        assertThat(manager.areTypesLoaded()).isTrue()
    }

    @Test
    fun `removeResourceType sends request to LSP server`() = runTest {
        val mockLspServer = mock<LspServer>()
        val manager = ResourceTypesManager(projectRule.project, this)
        
        // Mock the sendRequest to return success
        whenever(mockLspServer.sendRequest(any<(Any) -> CompletableFuture<Any>>())).thenReturn(null)
        
        manager.lspServerProvider = LspServerProvider { mockLspServer }
        
        // Add a resource type first
        manager.addResourceType("AWS::EC2::Instance")
        assertThat(manager.getSelectedResourceTypes()).containsExactly("AWS::EC2::Instance")

        // Remove the resource type
        manager.removeResourceType("AWS::EC2::Instance")
        
        // Wait for the coroutine to complete
        testScheduler.advanceUntilIdle()

        // Verify that sendRequest was called
        verify(mockLspServer).sendRequest(any<(Any) -> CompletableFuture<Any>>())
    }

    @Test
    fun `adding duplicate resource type is ignored`() {
        val manager = ResourceTypesManager(projectRule.project)

        manager.addResourceType("AWS::EC2::Instance")
        manager.addResourceType("AWS::EC2::Instance")

        assertThat(manager.getSelectedResourceTypes()).containsExactly("AWS::EC2::Instance")
    }

    @Test
    fun `loadAvailableTypes does nothing when no LSP server available`() {
        val manager = ResourceTypesManager(projectRule.project)
        manager.lspServerProvider = LspServerProvider { null }

        val future = manager.loadAvailableTypes()

        assertThat(future).isCompleted()
        assertThat(manager.areTypesLoaded()).isFalse()
        assertThat(manager.getAvailableResourceTypes()).isEmpty()
    }
}
