package top.kagg886.compose.installer.windows

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import top.kagg886.compose.installer.windows.config.BuildExtension
import top.kagg886.compose.installer.windows.task.CompileWXSTask
import top.kagg886.compose.installer.windows.task.EditWxsTask
import top.kagg886.compose.installer.windows.task.HarvestTask
import top.kagg886.compose.installer.windows.task.LightTask

class PluginMain : Plugin<Project> {
    override fun apply(project: Project) {
        val appDir = project.layout.projectDirectory.dir("build/compose/binaries/main-release/app/")
        val wixPath = project.rootDir.resolve("build").resolve("wix311")

        val extension = project.extensions.create("configureComposeWindowsInstaller", BuildExtension::class.java)

        val harvestTask: TaskProvider<HarvestTask> = project.tasks.register("harvest", HarvestTask::class.java)
        val editWxsTask: TaskProvider<EditWxsTask> = project.tasks.register("editWxs", EditWxsTask::class.java)
        val compileWxsTask:TaskProvider<CompileWXSTask> = project.tasks.register("compileWxs", CompileWXSTask::class.java)
        val lightTask:TaskProvider<LightTask> = project.tasks.register("light", LightTask::class.java)


        project.afterEvaluate {
            harvestTask.configure {
                group = "compose wix"
                description = "Generates WiX authoring from application image"
                dependsOn("createReleaseDistributable")
                workingDir(appDir)

                commandLine(
                    wixPath.resolve("heat.exe"),
                    "dir",
                    "./${extension.appName}",
                    "-nologo",
                    "-cg",
                    "DefaultFeature",
                    "-gg",
                    "-sfrag",
                    "-sreg",
                    "-template",
                    "product",
                    "-out",
                    "${extension.appName}.wxs",
                    "-var",
                    "var.SourceDir"
                )
            }
            editWxsTask.configure {
                group = "compose wix"
                description = "Edit the WXS File"
                dependsOn(harvestTask)

                doLast {
                    editWixTask(
                        shortcutName = extension.shortcutName,
                        iconPath = extension.iconFile.absolutePath,
                        manufacturer = extension.manufacturer,
                        wixFile = appDir.dir("${extension.appName}.wxs").asFile,
                        appName = extension.appName,
                        version = extension.appVersion
                    )
                }
            }
            compileWxsTask.configure {
                group = "compose wix"
                description = "Compile WXS file to WIXOBJ"
                dependsOn(editWxsTask)
                workingDir(appDir)

                commandLine(wixPath.resolve("candle.exe"), "${extension.appName}.wxs","-nologo", "-dSourceDir=.\\${extension.appName}")
            }
            lightTask.configure {
                group = "compose wix"
                description = "Linking the .wixobj file and creating a MSI"
                dependsOn(compileWxsTask)
                workingDir(appDir)

                commandLine(
                    wixPath.resolve("light.exe"),
                    "-ext",
                    "WixUIExtension",
                    "-cultures:zh-CN",
                    "-spdb",
                    "-nologo",
                    "${extension.appName}.wixobj",
                    "-o",
                    "windows.msi")
            }
        }
    }
}
