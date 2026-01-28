// Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cfnlsp.resources

import com.intellij.testFramework.ProjectRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import software.aws.toolkits.jetbrains.services.cfnlsp.CfnLspServer
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
    fun `can add and remove resource types`() {
        val manager = ResourceTypesManager(projectRule.project)
        
        manager.addResourceType("AWS::EC2::Instance")
        assertThat(manager.getSelectedResourceTypes()).containsExactly("AWS::EC2::Instance")
        
        manager.addResourceType("AWS::S3::Bucket")
        assertThat(manager.getSelectedResourceTypes()).containsExactlyInAnyOrder(
            "AWS::EC2::Instance", 
            "AWS::S3::Bucket"
        )
        
        manager.removeResourceType("AWS::EC2::Instance")
        assertThat(manager.getSelectedResourceTypes()).containsExactly("AWS::S3::Bucket")
    }

    @Test
    fun `loads available types from LSP server`() {
        val mockLspServer = mock<CfnLspServer>()
        val manager = ResourceTypesManager(projectRule.project)
        manager.lspServerProvider = { mockLspServer }
        
        val expectedTypes = listOf("AWS::EC2::Instance", "AWS::S3::Bucket", "AWS::Lambda::Function")
        whenever(mockLspServer.listResourceTypes()).thenReturn(
            CompletableFuture.completedFuture(ResourceTypesResult(expectedTypes))
        )
        
        manager.loadAvailableTypes()
        
        assertThat(manager.getAvailableResourceTypes()).containsExactlyElementsOf(expectedTypes)
    }

    @Test
    fun `adding duplicate resource type is ignored`() {
        val manager = ResourceTypesManager(projectRule.project)
        
        manager.addResourceType("AWS::EC2::Instance")
        manager.addResourceType("AWS::EC2::Instance")
        
        assertThat(manager.getSelectedResourceTypes()).containsExactly("AWS::EC2::Instance")
    }
}
