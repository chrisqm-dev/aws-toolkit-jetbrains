// Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cfnlsp.explorer

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.platform.lsp.api.LspServerManagerListener
import com.intellij.platform.lsp.api.LspServerState
import software.aws.toolkits.jetbrains.ToolkitPlaces
import software.aws.toolkits.jetbrains.core.credentials.CredentialManager
import software.aws.toolkits.jetbrains.core.credentials.ToolkitConnection
import software.aws.toolkits.jetbrains.core.credentials.ToolkitConnectionManagerListener
import software.aws.toolkits.jetbrains.core.explorer.AbstractExplorerTreeToolWindow
import software.aws.toolkits.jetbrains.core.gettingstarted.requestCredentialsForExplorer
import software.aws.toolkits.jetbrains.services.cfnlsp.resources.ResourceLoader
import software.aws.toolkits.jetbrains.services.cfnlsp.resources.ResourceTypesManager
import software.aws.toolkits.jetbrains.services.cfnlsp.server.CfnLspServerDescriptor
import software.aws.toolkits.jetbrains.services.cfnlsp.server.CfnLspServerSupportProvider
import software.aws.toolkits.jetbrains.services.cfnlsp.stacks.ChangeSetsManager
import software.aws.toolkits.jetbrains.services.cfnlsp.stacks.StacksManager
import software.aws.toolkits.jetbrains.ui.CenteredInfoPanel
import software.aws.toolkits.resources.AwsToolkitBundle.message

@Service(Service.Level.PROJECT)
internal class CloudFormationToolWindow(private val project: Project) : AbstractExplorerTreeToolWindow(
    CloudFormationTreeStructure(project),
    initialTreeExpandDepth = 1
) {
    override val actionPlace = ToolkitPlaces.CFN_TOOL_WINDOW

    init {
        val toolbarGroup = ActionManager.getInstance().getAction("aws.toolkit.cloudformation.toolbar")
        toolbar = ActionManager.getInstance().createActionToolbar(actionPlace, toolbarGroup as ActionGroup, true).apply {
            targetComponent = this@CloudFormationToolWindow
        }.component

        StacksManager.getInstance(project).addListener {
            runInEdt { redrawContent() }
        }
        ChangeSetsManager.getInstance(project).addListener {
            runInEdt { redrawContent() }
        }
        ResourceLoader.getInstance(project).addListener { _, _ ->
            runInEdt { redrawContent() }
        }
        ResourceTypesManager.getInstance(project).addListener {
            runInEdt { redrawContent() }
        }
        subscribeToConnectionChanges()
        updateContent()
        ensureLspServerStartedAndLoadData()
    }

    private fun subscribeToConnectionChanges() {
        project.messageBus.connect(this).subscribe(
            ToolkitConnectionManagerListener.TOPIC,
            object : ToolkitConnectionManagerListener {
                override fun activeConnectionChanged(newConnection: ToolkitConnection?) {
                    runInEdt { updateContent() }
                }
            }
        )
    }

    private fun updateContent() {
        if (CredentialManager.getInstance().getCredentialIdentifiers().isEmpty()) {
            setContent(
                CenteredInfoPanel().apply {
                    addLine(message("cloudformation.explorer.sign_in"))
                    addDefaultActionButton(message("gettingstarted.explorer.new.setup")) {
                        requestCredentialsForExplorer(project)
                    }
                }
            )
        } else {
            setContent(this.tree)
        }
    }

    private fun ensureLspServerStartedAndLoadData() {
        // Fix for "Nothing to Show" issue: Wait for LSP server to be ready before tree loads data
        val lspManager = LspServerManager.getInstance(project)
        val descriptor = CfnLspServerDescriptor.getInstance(project)
        
        // Ensure server is started
        lspManager.ensureServerStarted(CfnLspServerSupportProvider::class.java, descriptor)
        
        // Check if server is already running
        val servers = lspManager.getServersForProvider(CfnLspServerSupportProvider::class.java)
        if (servers.any { it.state == LspServerState.Running }) {
            // Server already ready - tree will load data naturally
            return
        } else {
            // Wait for server to be ready, then let tree load naturally
            @Suppress("UnstableApiUsage")
            lspManager.addLspServerManagerListener(object : LspServerManagerListener {
                override fun serverStateChanged(lspServer: LspServer) {
                    if (lspServer.descriptor::class == descriptor::class && 
                        lspServer.state == LspServerState.Running) {
                        runInEdt { 
                            // Server is ready - trigger tree refresh to load data
                            redrawContent()
                        }
                    }
                }
            }, this)
        }
    }

    companion object {
        fun getInstance(project: Project): CloudFormationToolWindow = project.service()
    }
}
