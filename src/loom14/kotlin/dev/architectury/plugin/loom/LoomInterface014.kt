package dev.architectury.plugin.loom

import dev.architectury.plugin.ModLoader
import net.fabricmc.loom.LoomGradleExtension
import net.fabricmc.loom.build.nesting.NestableJarGenerationTask
import net.fabricmc.loom.configuration.ide.RunConfig
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.util.ModPlatform
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.jvm.tasks.Jar
import java.io.File
import java.nio.file.Path
import java.util.function.Consumer

class LoomInterface014(private val project: Project) : LoomInterface {
    private val extension: LoomGradleExtension
        get() = LoomGradleExtension.get(project)

    override val allMixinMappings: Collection<File>
        get() = emptyList()

    override val tinyMappingsWithSrg: Path?
        get() = null

    override val refmapName: String
        get() = extension.mixin.defaultRefmapName.get()

    override var generateSrgTiny: Boolean
        get() = extension.shouldGenerateSrgTiny()
        set(value) {
            extension.setGenerateSrgTiny(value)
        }

    override val generateTransformerPropertiesInTask = true

    override val disableObfuscation: Boolean
        get() = extension.disableObfuscation()

    override fun settingsPostEdit(action: (config: LoomInterface.LoomRunConfig) -> Unit) {
        extension.settingsPostEdit.add(Consumer { c -> action(LoomRunConfigImpl(c)) })
    }

    override fun setIdeConfigGenerated() {
        extension.runConfigs.forEach { it.isIdeConfigGenerated = true }
        extension.runConfigs.whenObjectAdded { it.isIdeConfigGenerated = true }
        extension.addTaskBeforeRun("\$PROJECT_DIR\$/${project.name}:classes")
    }

    override fun setRemapJarInput(task: Jar, archiveFile: Provider<RegularFile>) {
        task as RemapJarTask
        task.inputFile.set(archiveFile)
    }

    override fun addNestedJars(task: Jar) {
        val jarTask = project.tasks.named(task.name, Jar::class.java)
        val nestedJars = project.fileTree(
            project.tasks.named("processIncludeJars", NestableJarGenerationTask::class.java)
                .flatMap(NestableJarGenerationTask::getOutputDirectory)
        ).matching {
            it.include("*.jar")
        }

        extension.nestJars(jarTask, nestedJars)
    }

    class LoomRunConfigImpl(private val config: RunConfig) : LoomInterface.LoomRunConfig {
        override var mainClass: String
            get() = config.mainClass
            set(value) {
                config.mainClass = value
            }

        override fun addVmArg(vmArg: String) {
            config.vmArgs.add(vmArg)
        }

        override fun escape(arg: String): String = arg
    }
}