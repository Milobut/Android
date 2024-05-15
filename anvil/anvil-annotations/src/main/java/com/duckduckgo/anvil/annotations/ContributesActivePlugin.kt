/*
 * Copyright (c) 2024 DuckDuckGo
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

package com.duckduckgo.anvil.annotations

import kotlin.reflect.KClass

/**
 * Anvil annotation to generate plugin points
 *
 * Usage:
 * ```kotlin
 * @ContributesActivePlugin(SomeDaggerScope::class)
 * class MyPluginImpl : MyPlugin {
 *
 * }
 *
 * interface MyPlugin : ActivePluginPoint.ActivePlugin {...}
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ContributesActivePlugin(
    /** The scope in which to include this contributed PluginPoint */
    val scope: KClass<*>,

    val boundType: KClass<*>,

    /**
     * The name of the remote feature flag that will guard the active plugins
     */
    val featureName: String,

    /**
     * The default value of the [featureName] feature flag.
     * By default they act as a kill-switch, ie. default enabled.
     */
    val defaultActiveValue: Boolean = true,

    /**
     * The priority for the plugin.
     * Lower priority values mean the associated plugin comes first in the list of plugins.
     */
    val priority: Int = 0,
)
