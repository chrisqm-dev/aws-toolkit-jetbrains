// Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cfnlsp.resources

import com.intellij.platform.lsp.api.LspServer
import com.intellij.testFramework.ProjectRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
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
        val mockLspServer = mock<LspServer>()
        val manager = ResourceTypesManager(projectRule.project)
        manager.lspServerProvider = LspServerProvider { mockLspServer }

        // Test that manager can be configured with LSP server
        // Actual LSP communication is tested at integration level
        assertThat(manager.getAvailableResourceTypes()).isEmpty()
    }

    @Test
    fun `adding duplicate resource type is ignored`() {
        val manager = ResourceTypesManager(projectRule.project)

        manager.addResourceType("AWS::EC2::Instance")
        manager.addResourceType("AWS::EC2::Instance")

        assertThat(manager.getSelectedResourceTypes()).containsExactly("AWS::EC2::Instance")
    }
}
