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

import androidx.lifecycle.LifecycleOwner
import com.duckduckgo.anvil.annotations.ContributesActivePlugin
import com.duckduckgo.anvil.annotations.ContributesActivePluginPoint
import com.duckduckgo.app.di.AppCoroutineScope
import com.duckduckgo.app.lifecycle.MainProcessLifecycleObserver
import com.duckduckgo.common.utils.plugins.ActivePluginPoint
import com.duckduckgo.di.scopes.AppScope
import com.squareup.anvil.annotations.ContributesMultibinding
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import logcat.logcat

@ContributesActivePluginPoint(
    scope = AppScope::class,
)
interface MyPlugin : ActivePluginPoint.ActivePlugin {
    fun doSomething()
}

@ContributesActivePlugin(
    scope = AppScope::class,
    boundType = MyPlugin::class,
    defaultActiveValue = false,
)
class FooActivePlugin @Inject constructor() : MyPlugin {
    override fun doSomething() {
        logcat { "Aitor Foo" }
    }
}

@ContributesActivePlugin(
    scope = AppScope::class,
    boundType = MyPlugin::class,
    priority = 100,
)
class BarActivePlugin @Inject constructor() : MyPlugin {
    override fun doSomething() {
        logcat { "Aitor Bar" }
    }
}

@ContributesActivePlugin(
    scope = AppScope::class,
    boundType = MyPlugin::class,
    priority = 50,
)
class BazActivePlugin @Inject constructor() : MyPlugin {
    override fun doSomething() {
        logcat { "Aitor Baz" }
    }
}

// ////
// @ContributesPluginPoint(
//     scope = AppScope::class,
//     boundType = MyPlugin::class,
// )
// @Suppress("unused")
// private interface TriggerMyPluginGenerationPP

@ContributesMultibinding(AppScope::class)
class UserOfThePluginPoint @Inject constructor(
    private val pp: ActivePluginPoint<@JvmSuppressWildcards MyPlugin>,
    @AppCoroutineScope private val coroutineScope: CoroutineScope,
) : MainProcessLifecycleObserver {

    override fun onResume(owner: LifecycleOwner) {
        coroutineScope.launch {
            pp.getPlugins().forEach { it.doSomething() }
        }
    }
}
