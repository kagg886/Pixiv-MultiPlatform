package top.kagg886.compose.installer.windows.task

import org.gradle.api.DefaultTask
import org.w3c.dom.Element
import top.kagg886.compose.installer.windows.top.kagg886.compose.installer.windows.util.*
import top.kagg886.compose.installer.windows.top.kagg886.compose.installer.windows.util.componentBuilder
import top.kagg886.compose.installer.windows.top.kagg886.compose.installer.windows.util.createNameUUID
import top.kagg886.compose.installer.windows.top.kagg886.compose.installer.windows.util.directoryBuilder
import top.kagg886.compose.installer.windows.top.kagg886.compose.installer.windows.util.registryBuilder
import top.kagg886.compose.installer.windows.top.kagg886.compose.installer.windows.util.shortcutBuilder
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

open class EditWxsTask : DefaultTask() {
    fun editWixTask(
        wixFile: File,
        appName: String,
        shortcutName: String,
        version: String,
        iconPath: String,
        manufacturer: String
    ) {
        val dbf = DocumentBuilderFactory.newInstance()
        val doc = dbf.newDocumentBuilder().parse(wixFile)
        doc.documentElement.normalize()

        // 设置 Product 节点
        //<Product Codepage="" Id="" Language="" Manufacturer="" Name="" UpgradeCode="" Version="1.0">
        val productElement = doc.documentElement.getElementsByTagName("Product").item(0) as Element

        // 设置升级码, 用于升级,大版本更新时，可能需要修改这个值
        // 如果要修改这个值，可能还需要修改安装位置，如果不修改安装位置，两个版本会安装在同一个位置
        // 这段代码和 MajorUpgrade 相关，如果 UpgradeCode 一直保持不变，安装新版的时候会自动卸载旧版本。
        val upgradeCode = createNameUUID("v1")
        productElement.apply {
            setAttribute("Manufacturer", manufacturer)
            setAttribute("Codepage", "936")
            // 这个 Name 属性会出现在安装引导界面
            // 控制面板-程序列表里也是这个名字
            setAttribute("Name", shortcutName)
            setAttribute("Version", version)
            setAttribute("UpgradeCode", upgradeCode)
        }


        // 设置 Package 节点
        // <Package Compressed="" InstallerVersion="" Languages="" Manufacturer="" Platform="x64"/>
        val packageElement = productElement.getElementsByTagName("Package").item(0) as Element
        packageElement.apply {
            setAttribute("Compressed", "yes")
            setAttribute("InstallerVersion", "200")
            setAttribute("Languages", "1033")
            setAttribute("Manufacturer", manufacturer)
            setAttribute("Platform", "x64")
        }

        //  <Directory Id="TARGETDIR" Name="SourceDir">
        val targetDirectory = doc.documentElement.getElementsByTagName("Directory").item(0) as Element

        // 桌面文件夹
        // <Directory Id="DesktopFolder" Name="Desktop" />
        val desktopFolderElement = directoryBuilder(doc, id = "DesktopFolder").apply {
            setAttributeNode(doc.createAttribute("Name").also { it.value = "Desktop" })
        }
        val desktopGuid = createNameUUID("DesktopShortcutComponent")
        val desktopComponent = componentBuilder(doc, id = "DesktopShortcutComponent", guid = desktopGuid)
        val desktopReg = registryBuilder(
            doc,
            id = "DesktopShortcutReg",
            productCode = "[ProductCode]",
            manufacturer = manufacturer,
            appName = appName
        )
        // <Shortcut Advertise="no" Directory="DesktopFolder" Target = "[INSTALLDIR]${project.name}.exe" Icon="icon.ico" IconIndex="0" Id="DesktopShortcut" Name="$shortcutName" WorkingDirectory="INSTALLDIR"/>
        val desktopShortcut = shortcutBuilder(
            doc,
            id = "DesktopShortcut",
            directory = "DesktopFolder",
            workingDirectory = "INSTALLDIR",
            name = shortcutName,
            target = "[INSTALLDIR]${appName}.exe",
            icon = "icon.ico"
        )
        //   <RemoveFile Id="DesktopShortcut" On="uninstall" Name="shortcutName.lnk" Directory="DesktopFolder"/>
        val removeDesktopShortcut = doc.createElement("RemoveFile").apply {
            setAttributeNode(doc.createAttribute("Id").also { it.value = "DesktopShortcut" })
            setAttributeNode(doc.createAttribute("On").also { it.value = "uninstall" })
            setAttributeNode(doc.createAttribute("Name").also { it.value = "$shortcutName.lnk" })
            setAttributeNode(doc.createAttribute("Directory").also { it.value = "DesktopFolder" })
        }
        desktopComponent.appendChild(desktopShortcut)
        desktopComponent.appendChild(desktopReg)
        desktopComponent.appendChild(removeDesktopShortcut)
        desktopFolderElement.appendChild(desktopComponent)
        targetDirectory.appendChild(desktopFolderElement)

        // 开始菜单文件夹
        val programMenuFolderElement = directoryBuilder(doc, id = "ProgramMenuFolder", name = "Programs")
        val programeMenuDir = directoryBuilder(doc, id = "ProgramMenuDir", name = shortcutName)
        val menuGuid = createNameUUID("programMenuDirComponent")
        val programMenuDirComponent = componentBuilder(doc, id = "programMenuDirComponent", guid = menuGuid)
        val startMenuShortcut = shortcutBuilder(
            doc,
            id = "startMenuShortcut",
            directory = "ProgramMenuDir",
            workingDirectory = "INSTALLDIR",
            name = shortcutName,
            target = "[INSTALLDIR]${appName}.exe",
            icon = "icon.ico"
        )
        val uninstallShortcut = shortcutBuilder(
            doc,
            id = "uninstallShortcut",
            name = "卸载$shortcutName",
            directory = "ProgramMenuDir",
            target = "[System64Folder]msiexec.exe",
            arguments = "/x [ProductCode]",
        )
        val removeFolder = removeFolderBuilder(doc, id = "CleanUpShortCut", directory = "ProgramMenuDir")
        val pRegistryValue = registryBuilder(
            doc, id = "ProgramMenuShortcutReg",
            productCode = "[ProductCode]",
            manufacturer = manufacturer,
            appName = appName
        )

        programMenuFolderElement.appendChild(programeMenuDir)
        programeMenuDir.appendChild(programMenuDirComponent)
        programMenuDirComponent.appendChild(startMenuShortcut)
        programMenuDirComponent.appendChild(uninstallShortcut)
        programMenuDirComponent.appendChild(removeFolder)
        programMenuDirComponent.appendChild(pRegistryValue)

        //<Component Guid="*" Id="RemoveShortcutComponent" Win64="yes">
        //  <RemoveFile Id="RemoveMenuShortcut" On="uninstall" Name="shortcutName.lnk" Directory="ProgramMenuDir"/>
        //  <RegistryValue Id="RemoveMenuShortcutReg" Key="Software\manufacturer" KeyPath="yes" Name="ProductCode" Root="HKCU" Type="string" Value="[ProductCode]"/>
        //</Component>
        val removeShortcutComponent =
            componentBuilder(doc, id = "RemoveShortcutComponent", guid = createNameUUID("RemoveShortcutComponent"))
        val removeMenuShortcut = doc.createElement("RemoveFile").apply {
            setAttributeNode(doc.createAttribute("Id").also { it.value = "RemoveMenuShortcut" })
            setAttributeNode(doc.createAttribute("On").also { it.value = "uninstall" })
            setAttributeNode(doc.createAttribute("Name").also { it.value = "*.lnk" })
            setAttributeNode(doc.createAttribute("Directory").also { it.value = "ProgramMenuDir" })
        }
        val removeMenuShortcutReg = registryBuilder(
            doc, id = "RemoveMenuShortcutReg",
            productCode = "[ProductCode]",
            manufacturer = manufacturer,
            appName = appName
        )
        removeShortcutComponent.appendChild(removeMenuShortcut)
        removeShortcutComponent.appendChild(removeMenuShortcutReg)


        targetDirectory.appendChild(programMenuFolderElement)
        targetDirectory.appendChild(removeShortcutComponent)

        // 设置所有组件的架构为 64 位
        val components = doc.documentElement.getElementsByTagName("Component")
        for (i in 0 until components.length) {
            val component = components.item(i) as Element
            val win64 = doc.createAttribute("Win64")
            win64.value = "yes"
            component.setAttributeNode(win64)
        }

        // 添加 ProgramFiles64Folder 节点
        val programFilesElement = doc.createElement("Directory")
        val idAttr = doc.createAttribute("Id")
        idAttr.value = "ProgramFiles64Folder"
        programFilesElement.setAttributeNode(idAttr)
        targetDirectory.appendChild(programFilesElement)
        val installDir = targetDirectory.getElementsByTagName("Directory").item(0)
        // 移除 installDir 节点
        val removedNode = targetDirectory.removeChild(installDir)
        // 将 installDir 节点添加到 programFilesElement 节点
        programFilesElement.appendChild(removedNode)
        // 设置安装目录的 Id 为 INSTALLDIR，快捷方式需要引用这个 Id
        val installDirElement = programFilesElement.getElementsByTagName("Directory").item(0) as Element
        installDirElement.setAttribute("Id", "INSTALLDIR")

        // 设置 Feature 节点
        val featureElement = doc.getElementsByTagName("Feature").item(0) as Element
        featureElement.setAttribute("Id", "Complete")
        featureElement.setAttribute("Title", appName)

        // 设置 UI
        // 添加 <Property Id="WIXUI_INSTALLDIR" Value="INSTALLDIR" />
        doc.createElement("Property").apply {
            setAttributeNode(doc.createAttribute("Id").also { it.value = "WIXUI_INSTALLDIR" })
            setAttributeNode(doc.createAttribute("Value").also { it.value = "INSTALLDIR" })
        }.also { productElement.appendChild(it) }


        //<UI>
        //  <UIRef Id="WixUI_InstallDir" />
        //</UI>
        val uiElement = doc.createElement("UI")
        productElement.appendChild(uiElement)
        doc.createElement("UIRef").apply {
            setAttributeNode(doc.createAttribute("Id").also { it.value = "WixUI_InstallDir" })
        }.also { uiElement.appendChild(it) }

        //  添加 Icon, 这个 Icon 会显示在控制面板的应用程序列表
        //  <Icon Id="icon.ico" SourceFile="$iconPath"/>
        //  <Property Id="ARPPRODUCTICON" Value="icon.ico" />
        if (iconPath.isNotEmpty()) {
            doc.createElement("Icon").apply {
                setAttributeNode(doc.createAttribute("Id").also { it.value = "icon.ico" })
                setAttributeNode(doc.createAttribute("SourceFile").also { it.value = iconPath })
            }.also { productElement.appendChild(it) }

            doc.createElement("Property").apply {
                setAttributeNode(doc.createAttribute("Id").also { it.value = "ARPPRODUCTICON" })
                setAttributeNode(doc.createAttribute("Value").also { it.value = "icon.ico" })
            }.also { productElement.appendChild(it) }

        }



        // 安装新版时，自动卸载旧版本，已经安装新版，再安装旧版本，提示用户先卸载新版。
        // 这段逻辑要和 UpgradeCode 一起设置，如果 UpgradeCode 一直保持不变，安装新版的时候会自动卸载旧版本。
        // 如果 UpgradeCode 改变了，可能会安装两个版本
        // <MajorUpgrade AllowSameVersionUpgrades="yes" DowngradeErrorMessage="A newer version of [ProductName] is already installed." AllowSameVersionUpgrades="yes"/>
        doc.createElement("MajorUpgrade").apply {
            val errorMessage = "新版的[ProductName]已经安装，如果要安装旧版本，请先把新版本卸载。"
            setAttributeNode(doc.createAttribute("AllowSameVersionUpgrades").also { it.value = "yes" })
            setAttributeNode(doc.createAttribute("DowngradeErrorMessage").also { it.value = errorMessage })
        }.also { productElement.appendChild(it) }


        // 设置 fragment 节点
        val fragmentElement = doc.getElementsByTagName("Fragment").item(0) as Element
        val componentGroup = fragmentElement.getElementsByTagName("ComponentGroup").item(0) as Element
        val programMenuDirRef = componentRefBuilder(doc, "programMenuDirComponent")
        val desktopShortcuRef = componentRefBuilder(doc, "DesktopShortcutComponent")
        val removeShortcutRef = componentRefBuilder(doc, "RemoveShortcutComponent")
        componentGroup.appendChild(desktopShortcuRef)
        componentGroup.appendChild(programMenuDirRef)
        componentGroup.appendChild(removeShortcutRef)

        generateXml(doc, wixFile)
    }
}
