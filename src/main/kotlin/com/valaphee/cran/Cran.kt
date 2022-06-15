/*
 * Copyright (c) 2022, Valaphee.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.valaphee.cran

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.guice.GuiceAnnotationIntrospector
import com.fasterxml.jackson.module.guice.GuiceInjectableValues
import com.fasterxml.jackson.module.kotlin.addDeserializer
import com.fasterxml.jackson.module.kotlin.addSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Provides
import com.google.inject.Singleton
import com.hazelcast.config.Config
import com.hazelcast.config.MapConfig
import com.hazelcast.config.MapStoreConfig
import com.hazelcast.config.SerializerConfig
import com.hazelcast.core.EntryEvent
import com.hazelcast.core.EntryListener
import com.hazelcast.core.Hazelcast
import com.hazelcast.map.MapEvent
import com.hazelcast.nio.ObjectDataInput
import com.hazelcast.nio.ObjectDataOutput
import com.hazelcast.nio.serialization.StreamSerializer
import com.valaphee.cran.graph.GraphEnv
import com.valaphee.cran.graph.GraphLookup
import com.valaphee.cran.graph.GraphStore
import com.valaphee.cran.impl.Implementation
import com.valaphee.cran.node.math.vector.DoubleVectorDeserializer
import com.valaphee.cran.node.math.vector.DoubleVectorSerializer
import com.valaphee.cran.node.math.vector.IntVectorDeserializer
import com.valaphee.cran.node.math.vector.IntVectorSerializer
import com.valaphee.cran.spec.Spec
import io.github.classgraph.ClassGraph
import jdk.incubator.vector.DoubleVector
import jdk.incubator.vector.IntVector
import kotlinx.coroutines.asCoroutineDispatcher
import org.apache.logging.log4j.LogManager
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.concurrent.Executors
import java.util.zip.GZIPInputStream

/**
 * @author Kevin Ludwig
 */
class Cran : JavaPlugin() {
    override fun onLoad() {
        injector = Guice.createInjector(object : AbstractModule() {
            @Provides
            @Singleton
            fun objectMapper(injector: Injector) = jacksonObjectMapper().registerModule(
                SimpleModule()
                    .addSerializer(IntVector::class   , IntVectorSerializer   ).addDeserializer(IntVector::class   , IntVectorDeserializer   )
                    .addSerializer(DoubleVector::class, DoubleVectorSerializer).addDeserializer(DoubleVector::class, DoubleVectorDeserializer)
            ).apply {
                val guiceAnnotationIntrospector = GuiceAnnotationIntrospector()
                setAnnotationIntrospectors(AnnotationIntrospectorPair(guiceAnnotationIntrospector, serializationConfig.annotationIntrospector), AnnotationIntrospectorPair(guiceAnnotationIntrospector, deserializationConfig.annotationIntrospector))
                injectableValues = GuiceInjectableValues(injector)
            }
        })

        val objectMapper = injector.getInstance(ObjectMapper::class.java)

        val hazelcast = Hazelcast.newHazelcastInstance(Config().apply {
            properties["hazelcast.logging.type"] = "log4j2"
            serializationConfig.apply {
                addSerializerConfig(SerializerConfig().setTypeClass(Spec.Node::class.java).setImplementation(object : StreamSerializer<Spec.Node> {
                    override fun getTypeId() = 1

                    override fun write(out: ObjectDataOutput, `object`: Spec.Node) {
                        objectMapper.writeValue(out, `object`)
                    }

                    override fun read(`in`: ObjectDataInput) = objectMapper.readValue(`in`, Spec.Node::class.java)
                }))
                addSerializerConfig(SerializerConfig().setTypeClass(GraphEnv::class.java).setImplementation(object : StreamSerializer<GraphEnv> {
                    override fun getTypeId() = 2

                    override fun write(out: ObjectDataOutput, `object`: GraphEnv) {
                        objectMapper.writeValue(out, `object`)
                    }

                    override fun read(`in`: ObjectDataInput) = objectMapper.readValue(`in`, GraphEnv::class.java)
                }))
            }
            mapConfigs["graphs"] = MapConfig().apply {
                mapStoreConfig.apply {
                    isEnabled = true
                    initialLoadMode = MapStoreConfig.InitialLoadMode.EAGER
                    implementation = GraphStore()
                }
            }
        })
        val nodeSpecs = hazelcast.getMap<String, Spec.Node>("node_specs")
        val nodeImpls = mutableListOf<Implementation>()
        val graphs = hazelcast.getMap<String, GraphEnv>("graphs")

        ClassGraph().scan().use {
            val spec = it.getResourcesMatchingWildcard("**.spec.json").urLs.map { objectMapper.readValue<Spec>(it).also { it.nodes.onEach { log.info("Built-in node '{}' found", it.name) } } }.reduce { acc, spec -> acc + spec }
            nodeSpecs.putAll(spec.nodes.onEach { log.info("Built-in node '{}' found", it.name) }.associateBy { it.name })
            spec.nodesImpls[""]?.let { nodeImpls.addAll(it.mapNotNull { Class.forName(it).kotlin.objectInstance as Implementation? }) }
            graphs.putAll(it.getResourcesMatchingWildcard("**.gph").urLs.map { objectMapper.readValue<GraphEnv>(it).also { log.info("Built-in graph '{}' found", it.name) } }.associateBy { it.name })
        }
        val graphLookup = object : GraphLookup {
            override fun getGraph(name: String) = graphs[name]
        }
        val coroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

        File("data").walk().forEach {
            if (it.isFile && it.extension == "gph") it.inputStream().use {
                objectMapper.readValue<GraphEnv>(GZIPInputStream(it)).also {
                    graphs[it.name] = it
                    it.run(objectMapper, nodeImpls, graphLookup, coroutineDispatcher)

                    log.info("Graph '{}' found", it.name)
                }
            }
        }


        graphs.addEntryListener(object : EntryListener<String, GraphEnv> {
            override fun entryAdded(event: EntryEvent<String, GraphEnv>) {
                event.value.run(objectMapper, nodeImpls, graphLookup, coroutineDispatcher)
            }

            override fun entryUpdated(event: EntryEvent<String, GraphEnv>) {
                event.oldValue.shutdown()
                event.value.run(objectMapper, nodeImpls, graphLookup, coroutineDispatcher)
            }

            override fun entryRemoved(event: EntryEvent<String, GraphEnv>) {
                event.value.shutdown()
            }

            override fun entryEvicted(event: EntryEvent<String, GraphEnv>) {
                event.value.shutdown()
            }

            override fun entryExpired(event: EntryEvent<String, GraphEnv>) {
                event.value.shutdown()
            }

            override fun mapCleared(event: MapEvent) = Unit

            override fun mapEvicted(event: MapEvent) = Unit
        }, true)
    }

    companion object {
        private val log = LogManager.getLogger(Cran::class.java)
        lateinit var injector: Injector
    }
}
