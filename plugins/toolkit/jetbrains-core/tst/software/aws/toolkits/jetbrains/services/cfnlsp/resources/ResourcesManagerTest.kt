// Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cfnlsp.resources

import com.intellij.testFramework.ProjectRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import software.aws.toolkits.jetbrains.services.cfnlsp.CfnLspServer
import software.aws.toolkits.jetbrains.services.cfnlsp.protocol.ListResourcesParams
import software.aws.toolkits.jetbrains.services.cfnlsp.protocol.ListResourcesResult
import software.aws.toolkits.jetbrains.services.cfnlsp.protocol.ResourceSummary
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
    fun `loads resources from LSP server`() {
        val mockLspServer = mock<CfnLspServer>()
        val manager = ResourcesManager(projectRule.project)
        manager.lspServerProvider = { mockLspServer }
        
        val resourceSummary = ResourceSummary(
            typeName = "AWS::EC2::Instance",
            resourceIdentifiers = listOf("i-1234567890abcdef0", "i-0987654321fedcba0"),
            nextToken = null
        )
        
        whenever(mockLspServer.listResources(any<ListResourcesParams>())).thenReturn(
            CompletableFuture.completedFuture(ListResourcesResult(listOf(resourceSummary)))
        )
        
        manager.reload("AWS::EC2::Instance")
        
        assertThat(manager.isLoaded("AWS::EC2::Instance")).isTrue()
        assertThat(manager.getResourceIdentifiers("AWS::EC2::Instance")).containsExactly(
            "i-1234567890abcdef0", "i-0987654321fedcba0"
        )
        assertThat(manager.hasMore("AWS::EC2::Instance")).isFalse()
    }

    @Test
    fun `handles pagination with nextToken`() {
        val mockLspServer = mock<CfnLspServer>()
        val manager = ResourcesManager(projectRule.project)
        manager.lspServerProvider = { mockLspServer }
        
        val resourceSummary = ResourceSummary(
            typeName = "AWS::EC2::Instance",
            resourceIdentifiers = listOf("i-1234567890abcdef0"),
            nextToken = "next-page-token"
        )
        
        whenever(mockLspServer.listResources(any<ListResourcesParams>())).thenReturn(
            CompletableFuture.completedFuture(ListResourcesResult(listOf(resourceSummary)))
        )
        
        manager.reload("AWS::EC2::Instance")
        
        assertThat(manager.hasMore("AWS::EC2::Instance")).isTrue()
    }

    @Test
    fun `clear removes cached data`() {
        val mockLspServer = mock<CfnLspServer>()
        val manager = ResourcesManager(projectRule.project)
        manager.lspServerProvider = { mockLspServer }
        
        val resourceSummary = ResourceSummary(
            typeName = "AWS::EC2::Instance",
            resourceIdentifiers = listOf("i-1234567890abcdef0"),
            nextToken = null
        )
        
        whenever(mockLspServer.listResources(any<ListResourcesParams>())).thenReturn(
            CompletableFuture.completedFuture(ListResourcesResult(listOf(resourceSummary)))
        )
        
        manager.reload("AWS::EC2::Instance")
        assertThat(manager.isLoaded("AWS::EC2::Instance")).isTrue()
        
        manager.clear("AWS::EC2::Instance")
        assertThat(manager.isLoaded("AWS::EC2::Instance")).isFalse()
        assertThat(manager.getCachedResources("AWS::EC2::Instance")).isNull()
    }
}
