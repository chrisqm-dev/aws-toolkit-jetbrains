// Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.cfnlsp.protocol

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ResourceProtocolTest {

    @Test
    fun `ResourceTypesResult contains expected data`() {
        val result = ResourceTypesResult(
            resourceTypes = listOf("AWS::EC2::Instance", "AWS::S3::Bucket")
        )

        assertThat(result.resourceTypes).containsExactly("AWS::EC2::Instance", "AWS::S3::Bucket")
    }

    @Test
    fun `ResourceSummary contains expected data`() {
        val summary = ResourceSummary(
            typeName = "AWS::EC2::Instance",
            resourceIdentifiers = listOf("i-1234567890abcdef0", "i-0987654321fedcba0"),
            nextToken = "next-page-token"
        )

        assertThat(summary.typeName).isEqualTo("AWS::EC2::Instance")
        assertThat(summary.resourceIdentifiers).containsExactly("i-1234567890abcdef0", "i-0987654321fedcba0")
        assertThat(summary.nextToken).isEqualTo("next-page-token")
    }

    @Test
    fun `ListResourcesParams handles optional resources`() {
        val paramsWithResources = ListResourcesParams(
            resources = listOf(
                ResourceRequest("AWS::EC2::Instance", "token1"),
                ResourceRequest("AWS::S3::Bucket", null)
            )
        )

        assertThat(paramsWithResources.resources).hasSize(2)
        assertThat(paramsWithResources.resources!![0].resourceType).isEqualTo("AWS::EC2::Instance")
        assertThat(paramsWithResources.resources!![0].nextToken).isEqualTo("token1")
        assertThat(paramsWithResources.resources!![1].nextToken).isNull()

        val paramsWithoutResources = ListResourcesParams(resources = null)
        assertThat(paramsWithoutResources.resources).isNull()
    }

    @Test
    fun `ResourceStatePurpose enum has expected values`() {
        assertThat(ResourceStatePurpose.values()).containsExactly(
            ResourceStatePurpose.IMPORT,
            ResourceStatePurpose.CLONE
        )
    }

    @Test
    fun `SearchResourceResult handles optional resource`() {
        val foundResult = SearchResourceResult(
            found = true,
            resource = ResourceSummary("AWS::EC2::Instance", listOf("i-123"), null)
        )

        assertThat(foundResult.found).isTrue()
        assertThat(foundResult.resource).isNotNull()

        val notFoundResult = SearchResourceResult(found = false, resource = null)
        assertThat(notFoundResult.found).isFalse()
        assertThat(notFoundResult.resource).isNull()
    }
}
