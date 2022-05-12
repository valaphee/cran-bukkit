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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("com.palantir.git-version") version "0.12.3"
    kotlin("jvm") version "1.6.21"
    id("net.linguica.maven-settings") version "0.5"
    signing
}

group = "com.valaphee"
val versionDetails: groovy.lang.Closure<com.palantir.gradle.gitversion.VersionDetails> by extra
val details = versionDetails()
version = "${details.lastTag}.${details.commitDistance}"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    compileOnly("com.tuinity:tuinity:1.16.5-R0.1-SNAPSHOT")
    implementation("com.valaphee:flow:0.0.1.0")
    implementation("com.valaphee:flow-meta:0.0.1.0")
    implementation("com.valaphee:flow-svc:0.0.1.0")
    implementation("io.github.classgraph:classgraph:4.8.146")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "16"
        targetCompatibility = "16"
    }

    withType<KotlinCompile> { kotlinOptions { jvmTarget = "16" } }

    processResources { filesMatching("/plugin.yml") { expand("project" to project) } }

    withType<Test> { useJUnitPlatform() }

    shadowJar { archiveName = "flow.jar" }
}

signing { useGpgCmd() }
