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

package com.valaphee.cran.node.command

import com.fasterxml.jackson.annotation.JsonProperty
import com.valaphee.cran.node.Node
import com.valaphee.cran.node.Und
import com.valaphee.cran.spec.NodeDecl
import com.valaphee.cran.spec.Out

/**
 * @author Kevin Ludwig
 */
@NodeDecl("Command/Commands")
class Commands(
    type: String,
    @get:Out ("" , Und) @get:JsonProperty("out") val out: Int,
) : Node(type) {
    init {
        out requires nothing
    }
}
