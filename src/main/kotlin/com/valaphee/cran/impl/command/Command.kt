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

package com.valaphee.cran.impl.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.BoolArgumentType.bool
import com.mojang.brigadier.arguments.BoolArgumentType.getBool
import com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg
import com.mojang.brigadier.arguments.DoubleArgumentType.getDouble
import com.mojang.brigadier.arguments.FloatArgumentType.floatArg
import com.mojang.brigadier.arguments.FloatArgumentType.getFloat
import com.mojang.brigadier.arguments.IntegerArgumentType.getInteger
import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import com.mojang.brigadier.arguments.StringArgumentType.string
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.tree.CommandNode
import com.valaphee.cran.Scope
import com.valaphee.cran.impl.Implementation
import com.valaphee.cran.node.Node
import com.valaphee.cran.node.command.Commands
import com.valaphee.cran.node.command.SendMessage
import com.valaphee.cran.node.command.argument.BooleanArgument
import com.valaphee.cran.node.command.argument.DoubleArgument
import com.valaphee.cran.node.command.argument.FloatArgument
import com.valaphee.cran.node.command.argument.GreedyStringArgument
import com.valaphee.cran.node.command.argument.IntegerArgument
import com.valaphee.cran.node.command.argument.LiteralArgument
import com.valaphee.cran.node.command.argument.StringArgument
import com.valaphee.cran.spec.NodeImpl
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.minecraft.server.v1_16_R3.CommandListenerWrapper
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_16_R3.CraftServer
import kotlin.coroutines.coroutineContext

/**
 * @author Kevin Ludwig
 */
@NodeImpl
object Command : Implementation {
    override fun initialize(node: Node, scope: Scope) = when (node) {
        is Commands -> {
            scope.dataPath(node.out).set { (Bukkit.getServer() as CraftServer).server.commandDispatcher.a() }

            true
        }
        is LiteralArgument -> {
            scope.dataPath(node.out).set(literal<CommandListenerWrapper>(node.name).apply {
                scope.controlPathOrNull(node.outExecute)?.let { outExecute ->
                    executes {
                        runBlocking { withContext(CommandContext(it)) { outExecute() } }

                        0
                    }
                }
            }.build().also {
                runBlocking { scope.dataPath(node.`in`).get() }.let { `in` ->
                    @Suppress("UNCHECKED_CAST")
                    if (`in` !is CommandDispatcher<*>) (`in` as CommandNode<CommandListenerWrapper>).addChild(it)
                }
            })

            true
        }
        is BooleanArgument -> {
            scope.dataPath(node.out).set(argument<CommandListenerWrapper, Boolean>(node.name, bool()).apply {
                scope.controlPathOrNull(node.outExecute)?.let { outExecute ->
                    executes {
                        runBlocking { withContext(CommandContext(it)) { outExecute() } }

                        0
                    }
                }
            }.build().also {
                @Suppress("UNCHECKED_CAST")
                (runBlocking { scope.dataPath(node.`in`).get() } as CommandNode<CommandListenerWrapper>).addChild(it)
            })
            scope.dataPath(node.outValue).set { getBool(coroutineContext.commandContext.commandContext, node.name) }

            true
        }
        is IntegerArgument -> {
            scope.dataPath(node.out).set(argument<CommandListenerWrapper, Int>(node.name, integer()).apply {
                scope.controlPathOrNull(node.outExecute)?.let { outExecute ->
                    executes {
                        runBlocking { withContext(CommandContext(it)) { outExecute() } }

                        0
                    }
                }
            }.build().also {
                @Suppress("UNCHECKED_CAST")
                (runBlocking { scope.dataPath(node.`in`).get() } as CommandNode<CommandListenerWrapper>).addChild(it)
            })
            scope.dataPath(node.outValue).set { getInteger(coroutineContext.commandContext.commandContext, node.name) }

            true
        }
        is FloatArgument -> {
            scope.dataPath(node.out).set(argument<CommandListenerWrapper, Float>(node.name, floatArg()).apply {
                scope.controlPathOrNull(node.outExecute)?.let { outExecute ->
                    executes {
                        runBlocking { withContext(CommandContext(it)) { outExecute() } }

                        0
                    }
                }
            }.build().also {
                @Suppress("UNCHECKED_CAST")
                (runBlocking { scope.dataPath(node.`in`).get() } as CommandNode<CommandListenerWrapper>).addChild(it)
            })
            scope.dataPath(node.outValue).set { getFloat(coroutineContext.commandContext.commandContext, node.name) }

            true
        }
        is DoubleArgument -> {
            scope.dataPath(node.out).set(argument<CommandListenerWrapper, Double>(node.name, doubleArg()).apply {
                scope.controlPathOrNull(node.outExecute)?.let { outExecute ->
                    executes {
                        runBlocking { withContext(CommandContext(it)) { outExecute() } }

                        0
                    }
                }
            }.build().also {
                @Suppress("UNCHECKED_CAST")
                (runBlocking { scope.dataPath(node.`in`).get() } as CommandNode<CommandListenerWrapper>).addChild(it)
            })
            scope.dataPath(node.outValue).set { getDouble(coroutineContext.commandContext.commandContext, node.name) }

            true
        }
        is StringArgument -> {
            scope.dataPath(node.out).set(argument<CommandListenerWrapper, String>(node.name, string()).apply {
                scope.controlPathOrNull(node.outExecute)?.let { outExecute ->
                    executes {
                        runBlocking { withContext(CommandContext(it)) { outExecute() } }

                        0
                    }
                }
            }.build().also {
                @Suppress("UNCHECKED_CAST")
                (runBlocking { scope.dataPath(node.`in`).get() } as CommandNode<CommandListenerWrapper>).addChild(it)
            })
            scope.dataPath(node.outValue).set { getString(coroutineContext.commandContext.commandContext, node.name) }

            true
        }
        is GreedyStringArgument -> {
            scope.dataPath(node.out).set(argument<CommandListenerWrapper, String>(node.name, greedyString()).apply {
                scope.controlPathOrNull(node.outExecute)?.let { outExecute ->
                    executes {
                        runBlocking { withContext(CommandContext(it)) { outExecute() } }

                        0
                    }
                }
            }.build().also {
                @Suppress("UNCHECKED_CAST")
                (runBlocking { scope.dataPath(node.`in`).get() } as CommandNode<CommandListenerWrapper>).addChild(it)
            })
            scope.dataPath(node.outValue).set { getString(coroutineContext.commandContext.commandContext, node.name) }

            true
        }
        is SendMessage -> {
            val inMessage = scope.dataPath(node.inMessage)
            val out = scope.controlPath(node.out)

            scope.controlPath(node.`in`).define {
                coroutineContext.commandContext.commandContext.source.bukkitSender.sendMessage(inMessage.get().toString())
                out()
            }

            true
        }
        else -> false
    }

    override fun postInitialize(node: Node, scope: Scope) = when (node) {
        is LiteralArgument -> {
            runBlocking {
                @Suppress("UNCHECKED_CAST")
                (scope.dataPath(node.`in`).get() as CommandDispatcher<CommandListenerWrapper>).root.addChild(scope.dataPath(node.out).get() as CommandNode<CommandListenerWrapper>)
            }

            true
        }
        else -> false
    }
}
