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

package com.duckduckgo.feature.toggles.codegen

import com.duckduckgo.anvil.annotations.ContributesPluginPoint
import com.duckduckgo.anvil.annotations.ContributesRemoteFeature
import com.duckduckgo.anvil.annotations.PriorityKey
import com.duckduckgo.di.scopes.AppScope
import com.duckduckgo.feature.toggles.api.Toggle
import kotlin.reflect.KClass
import kotlin.reflect.full.functions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContributesActivePluginPointCodeGeneratorTest {

    @Test
    fun `generated plugins have right annotations`() {
        val clazz = Class
            .forName("com.duckduckgo.feature.toggles.codegen.BarActivePlugin_ActivePlugin")
            .kotlin

        assertNotNull(clazz.functions.find { it.name == "isActive" })
        assertTrue(clazz extends MyPlugin::class)

        val priorityAnnotation = clazz.java.getAnnotation(PriorityKey::class.java)!!
        assertNotNull(priorityAnnotation)
        assertEquals(100, priorityAnnotation.priority)
    }

    @Test
    fun `test generated bar remote features`() {
        val clazz = Class
            .forName("com.duckduckgo.feature.toggles.codegen.BarActivePlugin_ActivePlugin_RemoteFeature")

        assertNotNull(clazz.methods.find { it.name == "self" && it.returnType.kotlin == Toggle::class })
        assertNotNull(clazz.methods.find { it.name == "pluginBarActivePlugin" && it.returnType.kotlin == Toggle::class })

        assertNotNull(
            clazz.kotlin.functions.firstOrNull { it.name == "self" }!!.annotations.firstOrNull { it.annotationClass == Toggle.DefaultValue::class },
        )
        assertNotNull(
            clazz.kotlin.functions.firstOrNull { it.name == "pluginBarActivePlugin" }!!.annotations.firstOrNull { it.annotationClass == Toggle.DefaultValue::class },
        )
        assertTrue(clazz.kotlin.java.methods.find { it.name == "self" }!!.getAnnotation(Toggle.DefaultValue::class.java)!!.defaultValue)
        assertTrue(
            clazz.kotlin.java.methods.find { it.name == "pluginBarActivePlugin" }!!.getAnnotation(Toggle.DefaultValue::class.java)!!.defaultValue,
        )

        val featureAnnotation = clazz.kotlin.java.getAnnotation(ContributesRemoteFeature::class.java)!!
        assertEquals(AppScope::class, featureAnnotation.scope)
        assertEquals("pluginPointMyPlugin", featureAnnotation.featureName)
    }

    @Test
    fun `test generated foo remote features`() {
        val clazz = Class
            .forName("com.duckduckgo.feature.toggles.codegen.FooActivePlugin_ActivePlugin_RemoteFeature")

        assertNotNull(clazz.methods.find { it.name == "self" && it.returnType.kotlin == Toggle::class })
        assertNotNull(clazz.methods.find { it.name == "pluginFooActivePlugin" && it.returnType.kotlin == Toggle::class })

        assertNotNull(
            clazz.kotlin.functions.firstOrNull { it.name == "self" }!!.annotations.firstOrNull { it.annotationClass == Toggle.DefaultValue::class },
        )
        assertNotNull(
            clazz.kotlin.functions.firstOrNull { it.name == "pluginFooActivePlugin" }!!.annotations.firstOrNull { it.annotationClass == Toggle.DefaultValue::class },
        )
        assertTrue(clazz.kotlin.java.methods.find { it.name == "self" }!!.getAnnotation(Toggle.DefaultValue::class.java)!!.defaultValue)
        assertFalse(
            clazz.kotlin.java.methods.find { it.name == "pluginFooActivePlugin" }!!.getAnnotation(Toggle.DefaultValue::class.java)!!.defaultValue,
        )

        val featureAnnotation = clazz.kotlin.java.getAnnotation(ContributesRemoteFeature::class.java)!!
        assertEquals(AppScope::class, featureAnnotation.scope)
        assertEquals("pluginPointMyPlugin", featureAnnotation.featureName)
    }

    @Test
    fun `test generated plugin point`() {
        val clazz = Class
            .forName("com.duckduckgo.feature.toggles.codegen.Trigger_MyPlugin_ActivePluginPoint")

        val featureAnnotation = clazz.kotlin.java.getAnnotation(ContributesPluginPoint::class.java)!!
        assertEquals(AppScope::class, featureAnnotation.scope)
        assertEquals(MyPlugin::class, featureAnnotation.boundType)
    }

    @Test
    fun `test generated plugin point remote feature`() {
        val clazz = Class
            .forName("com.duckduckgo.feature.toggles.codegen.MyPlugin_ActivePluginPoint_RemoteFeature")

        assertNotNull(clazz.methods.find { it.name == "self" && it.returnType.kotlin == Toggle::class })

        assertNotNull(
            clazz.kotlin.functions.firstOrNull { it.name == "self" }!!.annotations.firstOrNull { it.annotationClass == Toggle.DefaultValue::class },
        )
        assertTrue(clazz.kotlin.java.methods.find { it.name == "self" }!!.getAnnotation(Toggle.DefaultValue::class.java)!!.defaultValue)

        val featureAnnotation = clazz.kotlin.java.getAnnotation(ContributesRemoteFeature::class.java)!!
        assertEquals(AppScope::class, featureAnnotation.scope)
        assertEquals("pluginPointMyPlugin", featureAnnotation.featureName)
    }

    @Test
    fun `test generated triggered plugin point`() {
        val clazz = Class
            .forName("com.duckduckgo.feature.toggles.codegen.Trigger_TriggeredMyPluginTrigger_ActivePluginPoint")

        val featureAnnotation = clazz.kotlin.java.getAnnotation(ContributesPluginPoint::class.java)!!
        assertEquals(AppScope::class, featureAnnotation.scope)
        assertEquals(TriggeredMyPlugin::class, featureAnnotation.boundType)
    }

    @Test
    fun `test generated triggered plugin point remote feature`() {
        val clazz = Class
            .forName("com.duckduckgo.feature.toggles.codegen.TriggeredMyPluginTrigger_ActivePluginPoint_RemoteFeature")

        assertNotNull(clazz.methods.find { it.name == "self" && it.returnType.kotlin == Toggle::class })

        assertNotNull(
            clazz.kotlin.functions.firstOrNull { it.name == "self" }!!.annotations.firstOrNull { it.annotationClass == Toggle.DefaultValue::class },
        )
        assertTrue(clazz.kotlin.java.methods.find { it.name == "self" }!!.getAnnotation(Toggle.DefaultValue::class.java)!!.defaultValue)

        val featureAnnotation = clazz.kotlin.java.getAnnotation(ContributesRemoteFeature::class.java)!!
        assertEquals(AppScope::class, featureAnnotation.scope)
        assertEquals("pluginPointTriggeredMyPlugin", featureAnnotation.featureName)
    }

    @Test
    fun `test generated triggered foo remote features`() {
        val clazz = Class
            .forName("com.duckduckgo.feature.toggles.codegen.FooActiveTriggeredMyPlugin_ActivePlugin_RemoteFeature")

        assertNotNull(clazz.methods.find { it.name == "self" && it.returnType.kotlin == Toggle::class })
        assertNotNull(clazz.methods.find { it.name == "pluginFooActiveTriggeredMyPlugin" && it.returnType.kotlin == Toggle::class })

        assertNotNull(
            clazz.kotlin.functions.firstOrNull { it.name == "self" }!!.annotations.firstOrNull { it.annotationClass == Toggle.DefaultValue::class },
        )
        assertNotNull(
            clazz.kotlin.functions.firstOrNull { it.name == "pluginFooActiveTriggeredMyPlugin" }!!.annotations.firstOrNull { it.annotationClass == Toggle.DefaultValue::class },
        )
        assertTrue(clazz.kotlin.java.methods.find { it.name == "self" }!!.getAnnotation(Toggle.DefaultValue::class.java)!!.defaultValue)
        assertFalse(
            clazz.kotlin.java.methods.find { it.name == "pluginFooActiveTriggeredMyPlugin" }!!.getAnnotation(Toggle.DefaultValue::class.java)!!.defaultValue,
        )

        val featureAnnotation = clazz.kotlin.java.getAnnotation(ContributesRemoteFeature::class.java)!!
        assertEquals(AppScope::class, featureAnnotation.scope)
        assertEquals("pluginPointTriggeredMyPlugin", featureAnnotation.featureName)
    }

    private infix fun KClass<*>.extends(other: KClass<*>): Boolean =
        other.java.isAssignableFrom(this.java)
}
