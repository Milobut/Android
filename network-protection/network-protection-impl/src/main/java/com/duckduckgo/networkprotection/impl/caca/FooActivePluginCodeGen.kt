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

package com.duckduckgo.networkprotection.impl.caca

// code gen
// private class MyActivePluginPointWrapper constructor(
//     private val pp: PluginPoint<@JvmSuppressWildcards MyPlugin>,
//     private val dispatcherProvider: DispatcherProvider,
// ) : ActivePluginPoint<MyPlugin> {
//     override suspend fun getPlugins(): Collection<MyPlugin> {
//         return withContext(dispatcherProvider.io()) {
//             pp.getPlugins().filter { it.isActive() }
//         }
//     }
// }

// code gen
// @Module
// @ContributesTo(AppScope::class)
// object MyActivePluginPointWrapperBindingModule {
//     @Provides
//     @SingleInstanceIn(AppScope::class)
//     fun bindMyActivePluginPointWrapper(
//         pp: PluginPoint<@JvmSuppressWildcards MyPlugin>,
//         dispatcherProvider: DispatcherProvider,
//     ): ActivePluginPoint<@JvmSuppressWildcards MyPlugin> {
//         return MyActivePluginPointWrapper(pp, dispatcherProvider)
//     }
// }

// code gen
// @ContributesRemoteFeature(
//     scope = AppScope::class,
//     featureName = "myPluginPoint",
// )
// interface MyPluginPointFeatureToggle {
//     @Toggle.DefaultValue(defaultValue = true)
//     fun self(): Toggle
//
//     @Toggle.DefaultValue(defaultValue = true)
//     fun fooActivePlugin(): Toggle
// }

// code gen
// @ContributesMultibinding(
//     scope = AppScope::class,
//     boundType = MyPlugin::class,
// )
// class MyPluginWrapper @Inject constructor(
//     fooActivePlugin: FooActivePlugin,
//     private val toggle: MyPluginPointFeatureToggle,
// ) : MyPlugin by fooActivePlugin {
//     override suspend fun isActive(): Boolean {
//         return toggle.fooActivePlugin().isEnabled()
//     }
// }

// @Module
// @ContributesTo(AppScope::class)
// object FooActivePlugin_Binding_Module {
//
//     @Provides
//     fun provideFooActivePlugin(): FooActivePlugin {
//         return FooActivePlugin()
//     }
// }
