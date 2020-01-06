/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.intellij.kubernetes.model

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.fabric8.kubernetes.api.model.DoneableNamespace
import io.fabric8.kubernetes.api.model.HasMetadata
import io.fabric8.kubernetes.api.model.Namespace
import io.fabric8.kubernetes.api.model.NamespaceList
import io.fabric8.kubernetes.client.NamespacedKubernetesClient
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation
import io.fabric8.kubernetes.client.dsl.Resource
import org.jboss.tools.intellij.kubernetes.model.mocks.Mocks.NAMESPACE1
import org.jboss.tools.intellij.kubernetes.model.mocks.Mocks.NAMESPACE2
import org.jboss.tools.intellij.kubernetes.model.mocks.Mocks.NAMESPACE3
import org.jboss.tools.intellij.kubernetes.model.mocks.Mocks.cluster
import org.jboss.tools.intellij.kubernetes.model.mocks.Mocks.clusterFactory
import org.jboss.tools.intellij.kubernetes.model.mocks.Mocks.namespaceProvider
import org.junit.Before
import org.junit.Test

typealias NamespaceListOperation = NonNamespaceOperation<Namespace, NamespaceList, DoneableNamespace, Resource<Namespace, DoneableNamespace>>

class KubernetesResourceModelTest {

    private lateinit var client: NamespacedKubernetesClient
    private lateinit var resourceChange: IResourceChangeObservable
    private lateinit var cluster: ICluster
    private lateinit var clusterFactory: (IResourceChangeObservable) -> ICluster
    private lateinit var provider: NamespaceProvider
    private lateinit var model: IKubernetesResourceModel

    @Before
    fun before() {
        client = mock()
        provider = namespaceProvider()
        cluster = cluster(client, provider)
        resourceChange = mock()
        clusterFactory = clusterFactory(cluster)
        model = KubernetesResourceModel(resourceChange, clusterFactory)
    }

    @Test
    fun `getAllNamespaces should return all namespaces in cluster`() {
        // given
        val namespaces = listOf(NAMESPACE1, NAMESPACE2, NAMESPACE3)
        doReturn(namespaces)
            .whenever(cluster).getAllNamespaces()
        // when
        model.getAllNamespaces()
        // then
        verify(cluster, times(1)).getAllNamespaces()
    }

    @Test
    fun `getNamespace(name) should return namespace from cluster`() {
        // given
        val name = NAMESPACE2.metadata.name
        doReturn(NAMESPACE2)
            .whenever(cluster).getNamespace(name)
        // when
        model.getNamespace(name)
        // then
        verify(cluster, times(1)).getNamespace(name)
    }

    @Test
    fun `getResources(name) should return all resources of a kind in the given namespace`() {
        // given
        // when
        val kind = HasMetadata::class.java
        model.getResources("anyNamespace", kind)
        // then
        verify(provider, times(1)).getResources(kind)
    }

    @Test
    fun `clear should create new cluster`() {
        // given
        // reset cluster factory invocation - to create the new cluster - that happens when model is instantiated
        reset(clusterFactory)
        // when
        model.clear()
        // then
        verify(clusterFactory, times(1)).invoke(any())
    }

    @Test
    fun `clear should close existing cluster`() {
        // given
        // when
        model.clear()
        // then
        verify(cluster, times(1)).close()
    }

    @Test
    fun `clear should notify client change`() {
        // given
        // when
        model.clear()
        // then
        verify(resourceChange, times(1)).fireModified(client)
    }

    @Test
    fun `clear resource should clear namespace provider`() {
        // given
        // when
        model.clear(mock<HasMetadata>())
        // then
        verify(provider, times(1)).clear()
    }

    @Test
    fun `clear inexistent resource should not clear namespace provider`() {
        // given no namespace provider returned
        doReturn(null)
            .whenever(cluster).getNamespaceProvider(any<HasMetadata>())
        // when
        model.clear(mock<HasMetadata>())
        // then
        verify(provider, never()).clear()
    }

    @Test
    fun `clear resource should fire namespace provider change`() {
        // given
        // when
        val resource = mock<HasMetadata>()
        model.clear(resource)
        // then
        verify(resourceChange, times(1)).fireModified(resource)
    }
}
