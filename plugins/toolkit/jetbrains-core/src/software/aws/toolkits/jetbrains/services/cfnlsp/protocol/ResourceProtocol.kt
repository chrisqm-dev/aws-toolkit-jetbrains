// Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cfnlsp.protocol

// Resource Types
data class ResourceTypesResult(
    val resourceTypes: List<String>
)

// Resource Listing
data class ResourceRequest(
    val resourceType: String,
    val nextToken: String? = null
)

data class ListResourcesParams(
    val resources: List<ResourceRequest>? = null
)

data class ResourceSummary(
    val typeName: String,
    val resourceIdentifiers: List<String>,
    val nextToken: String? = null
)

data class ListResourcesResult(
    val resources: List<ResourceSummary>
)
