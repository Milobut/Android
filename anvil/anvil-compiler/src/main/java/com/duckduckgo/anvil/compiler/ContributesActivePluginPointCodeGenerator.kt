/*
 * Copyright (c) 2022 DuckDuckGo
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

package com.duckduckgo.anvil.compiler

import com.duckduckgo.anvil.annotations.ContributesActivePlugin
import com.duckduckgo.anvil.annotations.ContributesActivePluginPoint
import com.duckduckgo.anvil.annotations.ContributesPluginPoint
import com.duckduckgo.anvil.annotations.ContributesRemoteFeature
import com.duckduckgo.anvil.annotations.PriorityKey
import com.duckduckgo.feature.toggles.api.Toggle
import com.google.auto.service.AutoService
import com.squareup.anvil.annotations.ContributesMultibinding
import com.squareup.anvil.annotations.ContributesTo
import com.squareup.anvil.annotations.ExperimentalAnvilApi
import com.squareup.anvil.compiler.api.*
import com.squareup.anvil.compiler.internal.asClassName
import com.squareup.anvil.compiler.internal.buildFile
import com.squareup.anvil.compiler.internal.fqName
import com.squareup.anvil.compiler.internal.reference.*
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.ABSTRACT
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.KModifier.SUSPEND
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import dagger.Binds
import java.io.File
import javax.inject.Inject
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile

/**
 * This Anvil code generator generates Active Plugins, ie. those that can be controlled via remote feature flag.
 * Active plugins and Active plugin points are generated using the [ContributesActivePluginPoint] and [ContributesActivePlugin] annotations.
 *
 * For classes annotated with [ContributesActivePluginPoint], this generator will
 * - generate a regular plugin point
 * - generate a wrapper around the normal plugin point to handle the associated remote feature flag
 * - generate a remote feature flag that will control the plugin point
 * - generate the bindings so that users can depend on ActivePluginPoint<T>
 *
 * For classes annotated with [ContributesActivePlugin] this generator will:
 * - generate a binding to contribute the plugin into the associated plugin point, using [ContributesMultibinding] and [PriorityKey]
 * - generate a remote feature flag that will control the plugin
 *
 * The business logic generated will ensure that:
 * - disabling a given remote feature flag associated to plugins will de-activate such plugin, ie. won't be return in getPlugins()
 * - disabling the remote feature associated to the plugin point will make getPlugins() method to return empty list, regardless of whether the
 * plugin is active or not
 *
 */
@OptIn(ExperimentalAnvilApi::class)
@AutoService(CodeGenerator::class)
class ContributesActivePluginPointCodeGenerator : CodeGenerator {

    private val activePluginPointAnnotations = listOf(
        ContributesActivePlugin::class,
        ContributesActivePluginPoint::class,
    )

    override fun isApplicable(context: AnvilContext): Boolean = true

    override fun generateCode(codeGenDir: File, module: ModuleDescriptor, projectFiles: Collection<KtFile>): Collection<GeneratedFileWithSources> {
        return projectFiles.classAndInnerClassReferences(module)
            .toList()
            .filter { reference -> reference.isAnnotatedWith(activePluginPointAnnotations.map { it.fqName }) }
            .flatMap {
                listOf(
                    generateActivePluginsPointAndPlugins(it, codeGenDir, module),
                )
            }
            .toMutableList().apply {
                // this.addAll(generatePluginPointRemoteFeature(codeGenDir, module))
            }.toList()
    }

    private fun generateActivePluginsPointAndPlugins(
        vmClass: ClassReference.Psi,
        codeGenDir: File,
        module: ModuleDescriptor,
    ): GeneratedFileWithSources {
        return if (vmClass.isContributesActivePluginPoint()) {
            generatedActivePluginPoint(vmClass, codeGenDir, module)
        } else {
            generatedActivePlugin(vmClass, codeGenDir, module)
        }
    }

    private fun generatedActivePluginPoint(vmClass: ClassReference.Psi, codeGenDir: File, module: ModuleDescriptor): GeneratedFileWithSources {
        val generatedPackage = vmClass.packageFqName.toString()
        val pluginPointClassFileName = "${vmClass.shortName}_ActivePluginPoint"
        val pluginPointClassName = "Trigger_${vmClass.shortName}_ActivePluginPoint"
        val pluginPointWrapperClassName = "${vmClass.shortName}_PluginPoint_ActiveWrapper"
        val pluginPointWrapperBindingModuleClassName = "${vmClass.shortName}_PluginPoint_ActiveWrapper_Binding_Module"
        val pluginPointRemoteFeatureClassName = "${vmClass.shortName}_ActivePluginPoint_RemoteFeature"
        val scope = vmClass.annotations.firstOrNull { it.fqName == ContributesActivePluginPoint::class.fqName }?.scopeOrNull(0)!!
        val pluginClassType = vmClass.pluginClassName(ContributesActivePluginPoint::class.fqName) ?: vmClass.asClassName()
        val featureName = "pluginPoint${pluginClassType.simpleName}"

        val content = FileSpec.buildFile(generatedPackage, pluginPointClassFileName) {
            // This is the normal plugin point
            addType(
                TypeSpec.interfaceBuilder(pluginPointClassName)
                    .addModifiers(PRIVATE)
                    .addAnnotation(
                        AnnotationSpec.builder(ContributesPluginPoint::class)
                            .addMember("scope = %T::class", scope.asClassName())
                            .addMember("boundType = %T::class", pluginClassType)
                            .build(),
                    )
                    .addAnnotation(
                        AnnotationSpec.builder(Suppress::class)
                            .addMember("%S", "unused")
                            .build(),
                    )
                    .build(),
            ).build()

            // Generate the feature flag that guards the plugin points
            addType(
                TypeSpec.interfaceBuilder(pluginPointRemoteFeatureClassName)
                    .addAnnotation(
                        AnnotationSpec.builder(ContributesRemoteFeature::class)
                            .addMember("scope = %T::class", scope.asClassName())
                            .addMember("featureName = %S", featureName)
                            .build(),
                    )
                    .addFunction(
                        FunSpec.builder("self")
                            .addModifiers(ABSTRACT)
                            .addAnnotation(
                                AnnotationSpec.builder(Toggle.DefaultValue::class)
                                    .addMember("defaultValue = %L", true)
                                    .build(),
                            )
                            .returns(Toggle::class)
                            .build(),
                    )
                    .build(),
            ).build()

            // This is the plugin point active wrapper. Depends on the normal plugin point above and wraps to allow "active" behavior, that is
            // just return the plugins that have the remote feature enabled.
            addType(
                TypeSpec.classBuilder(pluginPointWrapperClassName).apply {
                    primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addAnnotation(AnnotationSpec.builder(Inject::class).build())
                            .addParameter("toggle", ClassName(generatedPackage, pluginPointRemoteFeatureClassName))
                            .addParameter(
                                ParameterSpec.builder(
                                    "pluginPoint",
                                    pluginPointFqName.asClassName(module).parameterizedBy(
                                        pluginClassType.copy(
                                            annotations = listOf(AnnotationSpec.builder(JvmSuppressWildcards::class).build()),
                                        ),
                                    ),
                                ).build(),
                            )
                            .addParameter(ParameterSpec.builder("dispatcherProvider", dispatcherProviderFqName.asClassName(module)).build())
                            .build(),
                    )
                    addProperty(
                        PropertySpec.builder("toggle", ClassName(generatedPackage, pluginPointRemoteFeatureClassName), PRIVATE)
                            .initializer("toggle")
                            .build(),
                    )
                    addProperty(
                        PropertySpec.builder(
                            "pluginPoint",
                            pluginPointFqName.asClassName(module).parameterizedBy(
                                pluginClassType.copy(
                                    annotations = listOf(AnnotationSpec.builder(JvmSuppressWildcards::class).build()),
                                ),
                            ),
                            PRIVATE,
                        ).initializer("pluginPoint").build(),
                    )
                    addProperty(
                        PropertySpec.builder(
                            "dispatcherProvider",
                            dispatcherProviderFqName.asClassName(module),
                            PRIVATE,
                        )
                            .initializer("dispatcherProvider")
                            .build(),
                    )

                    addSuperinterface(
                        activePluginPointFqName
                            .asClassName(module)
                            .parameterizedBy(
                                pluginClassType.copy(annotations = listOf(AnnotationSpec.builder(JvmSuppressWildcards::class).build())),
                            ),
                    )

                    addFunction(
                        FunSpec.builder("getPlugins")
                            .addModifiers(OVERRIDE, SUSPEND)
                            .returns(Collection::class.fqName.asClassName(module).parameterizedBy(pluginClassType))
                            .addCode(
                                CodeBlock.of(
                                    """
                                        return kotlinx.coroutines.withContext(dispatcherProvider.io()) {
                                            if (toggle.self().isEnabled()) {
                                                pluginPoint.getPlugins().filter { it.isActive() }
                                            } else {
                                                emptyList()
                                            }
                                        }
                                    """.trimIndent(),
                                ),
                            )
                            .build(),
                    )
                }.build(),
            )

            // Finally we're gonna create the dagger binding module, which will bind the plugin point wrapper type to the ActivePluginPoint<T> type
            addType(
                TypeSpec.classBuilder(pluginPointWrapperBindingModuleClassName)
                    .addAnnotation(AnnotationSpec.builder(dagger.Module::class).build())
                    .addAnnotation(
                        AnnotationSpec
                            .builder(ContributesTo::class).addMember("scope = %T::class", scope.asClassName())
                            .build(),
                    )
                    .addModifiers(ABSTRACT)
                    .addFunction(
                        FunSpec.builder("binds$pluginPointWrapperClassName")
                            .addModifiers(ABSTRACT)
                            .addAnnotation(Binds::class.asClassName())
                            .addParameter(
                                ParameterSpec.builder(
                                    "pluginPoint",
                                    ClassName(generatedPackage, pluginPointWrapperClassName),
                                    // FqName(pluginPointWrapperClassName).asClassName(module)
                                ).build(),
                            )
                            .returns(
                                activePluginPointFqName
                                    .asClassName(module)
                                    .parameterizedBy(
                                        pluginClassType.copy(annotations = listOf(AnnotationSpec.builder(JvmSuppressWildcards::class).build())),
                                    ),
                            )
                            .build(),
                    )
                    .build(),
            )
        }

        return createGeneratedFile(codeGenDir, generatedPackage, pluginPointClassFileName, content, setOf(vmClass.containingFileAsJavaFile))
    }

    private fun generatedActivePlugin(vmClass: ClassReference.Psi, codeGenDir: File, module: ModuleDescriptor): GeneratedFileWithSources {
        val scope = vmClass.annotations.firstOrNull { it.fqName == ContributesActivePlugin::class.fqName }?.scopeOrNull(0)!!
        val boundType = vmClass.annotations.firstOrNull { it.fqName == ContributesActivePlugin::class.fqName }?.boundTypeOrNull()!!
        val featureDefaultValue = vmClass.annotations.firstOrNull {
            it.fqName == ContributesActivePlugin::class.fqName
        }?.defaultActiveValueOrNull() ?: true
        // the parent feature name is taken from the plugin interface name implemented by this class
        val parentFeatureName = "pluginPoint${boundType.shortName}"
        val featureName = "plugin${vmClass.shortName}"
        val generatedPackage = vmClass.packageFqName.toString()
        val pluginClassName = "${vmClass.shortName}_ActivePlugin"
        val pluginRemoteFeatureClassName = "${vmClass.shortName}_ActivePlugin_RemoteFeature"
        val pluginPriority = vmClass.annotations.firstOrNull { it.fqName == ContributesActivePlugin::class.fqName }?.priorityOrNull()

        val content = FileSpec.buildFile(generatedPackage, pluginClassName) {
            // First create the class that will contribute the active plugin.
            // We do expect that the plugins are define using the "ContributesActivePlugin" annotation but are also injected
            // using @Inject in the constructor, as the concrete plugin type is use as delegate.
            addType(
                TypeSpec.classBuilder(pluginClassName).apply {
                    addAnnotation(
                        AnnotationSpec.builder(ContributesMultibinding::class)
                            .addMember("scope = %T::class", scope.asClassName())
                            .addMember("boundType = %T::class", boundType.asClassName())
                            .build(),
                    )
                    // If the active plugin defined a priority then add the right annotation
                    pluginPriority?.let {
                        addAnnotation(
                            AnnotationSpec.builder(PriorityKey::class)
                                .addMember("%L", it)
                                .build(),
                        )
                    }

                    // primary constructor and parameters. We need the active plugin and the remote feature toggle
                    primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addAnnotation(AnnotationSpec.builder(Inject::class).build())
                            .addParameter("activePlugin", vmClass.asClassName())
                            .addParameter("toggle", ClassName(generatedPackage, pluginRemoteFeatureClassName))
                            .build(),
                    )
                    addProperty(
                        PropertySpec.builder("activePlugin", vmClass.asClassName(), PRIVATE)
                            .initializer("activePlugin")
                            .build(),
                    )
                    addProperty(
                        PropertySpec.builder("toggle", ClassName(generatedPackage, pluginRemoteFeatureClassName), PRIVATE)
                            .initializer("toggle")
                            .build(),
                    )

                    addSuperinterface(
                        boundType.asClassName(),
                        delegate = CodeBlock.of("activePlugin"),
                    )

                    addFunction(
                        FunSpec.builder("isActive")
                            .addModifiers(OVERRIDE, SUSPEND)
                            .returns(Boolean::class)
                            .addCode(CodeBlock.of("return toggle.$featureName().isEnabled()"))
                            .build(),
                    )
                }.build(),
            ).build()

            // Now generate the feature flag that guards the plugin
            addType(
                TypeSpec.interfaceBuilder(pluginRemoteFeatureClassName)
                    .addAnnotation(
                        AnnotationSpec.builder(ContributesRemoteFeature::class)
                            .addMember("scope = %T::class", scope.asClassName())
                            .addMember("featureName = %S", parentFeatureName)
                            .build(),
                    )
                    .addFunction(
                        FunSpec.builder("self")
                            .addModifiers(ABSTRACT)
                            .addAnnotation(
                                AnnotationSpec.builder(Toggle.DefaultValue::class)
                                    // The parent feature toggle is the one guarding the plugin point, for convention is default enabled.
                                    .addMember("defaultValue = %L", true)
                                    .build(),
                            )
                            .returns(Toggle::class)
                            .build(),
                    )
                    .addFunction(
                        FunSpec.builder(featureName)
                            .addModifiers(ABSTRACT)
                            .addAnnotation(
                                AnnotationSpec.builder(Toggle.DefaultValue::class)
                                    .addMember("defaultValue = %L", featureDefaultValue)
                                    .build(),
                            )
                            .returns(Toggle::class)
                            .build(),
                    )
                    .build(),
            ).build()
        }

        return createGeneratedFile(codeGenDir, generatedPackage, pluginClassName, content, setOf(vmClass.containingFileAsJavaFile))
    }

    private fun ClassReference.Psi.isContributesActivePlugin(): Boolean {
        return this.annotations.firstOrNull { it.fqName == ContributesActivePlugin::class.fqName } != null
    }

    private fun ClassReference.Psi.isContributesActivePluginPoint(): Boolean {
        return this.annotations.firstOrNull { it.fqName == ContributesActivePluginPoint::class.fqName } != null
    }

    @OptIn(ExperimentalAnvilApi::class)
    private fun AnnotationReference.defaultActiveValueOrNull(): Boolean? = argumentAt("defaultActiveValue", 2)?.value()

    @OptIn(ExperimentalAnvilApi::class)
    private fun AnnotationReference.priorityOrNull(): Int? = argumentAt("priority", 3)?.value()

    private fun ClassReference.Psi.pluginClassName(
        fqName: FqName,
    ): ClassName? {
        return annotations
            .first { it.fqName == fqName }
            .argumentAt(name = "boundType", index = 1)
            ?.annotation
            ?.boundTypeOrNull()
            ?.asClassName()
    }

    companion object {
        private val pluginPointFqName = FqName("com.duckduckgo.common.utils.plugins.PluginPoint")
        private val dispatcherProviderFqName = FqName("com.duckduckgo.common.utils.DispatcherProvider")
        private val activePluginPointFqName = FqName("com.duckduckgo.common.utils.plugins.ActivePluginPoint")
    }
}
