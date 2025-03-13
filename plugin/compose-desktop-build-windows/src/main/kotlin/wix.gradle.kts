//package top.kagg886.compose.installer.windows
//
//import java.io.*
//import org.w3c.dom.Document
//import org.w3c.dom.Element
//import java.io.File
//import java.util.UUID
//import javax.xml.parsers.DocumentBuilderFactory
//import javax.xml.transform.TransformerFactory
//import javax.xml.transform.dom.DOMSource
//import javax.xml.transform.stream.StreamResult
//import javax.xml.transform.OutputKeys
//import java.nio.charset.StandardCharsets
//
//// 如果要让用户选择安装路径，需要设置 license 文件，如果不设置，会使用一个默认的 license
//val licenseFile = project.file("license.rtf")
//val licensePath = if(licenseFile.exists()) licenseFile.absolutePath else ""
//
//// 设置安装包的图标，显示在控制面板的应用程序列表
//val iconFile = project.file("src/main/resources/logo/logo.ico")
//val iconPath = if(iconFile.exists()) iconFile.absolutePath else ""
//
//// 可以设置为开发者的名字或开发商的名字，在控制面板里 manufacturer 会显示为发布者
//// 这个信息会和项目的名称一起写入到注册表中
//val manufacturer = "未知"
//
//// 快捷方式的名字，会显示在桌面和开始菜单
//val shortcutName = "记事本"
//
//val appDir = project.layout.projectDirectory.dir("build/compose/binaries/main/app/")
//
//project.tasks.register<Exec>("harvest") {
//    group = "compose wix"
//    description = "Generates WiX authoring from application image"
//    val createDistributable = tasks.named("createDistributable")
//    dependsOn(createDistributable)
//    workingDir(appDir)
//
//    var heatFile = project.layout.projectDirectory.file("build/wix311/heat.exe").getAsFile()
//    // 有的版本的 wix311 在 build 目录下，有的版本在项目根目录下
//    if(!heatFile.exists()) {
//        heatFile = project.layout.projectDirectory.file("wix311/heat.exe").getAsFile()
//    }
//    val heat = heatFile.absolutePath
//
//    commandLine(
//        heat,
//        "dir",
//        "./${project.name}",
//        "-nologo",
//        "-cg",
//        "DefaultFeature",
//        "-gg",
//        "-sfrag",
//        "-sreg",
//        "-template",
//        "product",
//        "-out",
//        "${project.name}.wxs",
//        "-var",
//        "var.SourceDir"
//    )
//
//}
//
//project.tasks.register("editWxs") {
//    group = "compose wix"
//    description = "Edit the WXS File"
//    val harvest = tasks.named("harvest")
//    dependsOn(harvest)
//    doLast {
//        editWixTask(
//            shortcutName = shortcutName,
//            iconPath = iconPath,
//            licensePath = licensePath,
//            manufacturer = manufacturer
//        )
//    }
//}
//
//project.tasks.register<Exec>("compileWxs") {
//    group = "compose wix"
//    description = "Compile WXS file to WIXOBJ"
//    val editWxs = tasks.named("editWxs")
//    dependsOn(editWxs)
//    workingDir(appDir)
//
//    var candleFile = project.layout.projectDirectory.file("build/wix311/candle.exe").getAsFile()
//    // 有的版本的 wix311 在 build 目录下，有的版本在项目根目录下
//    if(!candleFile.exists()) {
//        candleFile = project.layout.projectDirectory.file("wix311/candle.exe").getAsFile()
//    }
//    val candle = candleFile.absolutePath
//    commandLine(candle, "${project.name}.wxs","-nologo", "-dSourceDir=.\\${project.name}")
//}
//
//project.tasks.register<Exec>("light") {
//    group = "compose wix"
//    description = "Linking the .wixobj file and creating a MSI"
//    val compileWxs = tasks.named("compileWxs")
//    dependsOn(compileWxs)
//    workingDir(appDir)
//    var lightFile =  project.layout.projectDirectory.file("build/wix311/light.exe").getAsFile()
//    // 有的版本的 wix311 在 build 目录下，有的版本在项目根目录下
//    if(!lightFile.exists()) {
//        lightFile = project.layout.projectDirectory.file("wix311/light.exe").getAsFile()
//    }
//    val light = lightFile.absolutePath
//
//    commandLine(light, "-ext", "WixUIExtension", "-cultures:zh-CN", "-spdb","-nologo", "${project.name}.wixobj", "-o", "${project.name}-${project.version}.msi")
//}
//
//
//
//
//private fun editWixTask(
//    shortcutName: String,
//    iconPath: String,
//    licensePath: String,
//    manufacturer:String
//) {
//    val wixFile = project.layout.projectDirectory.dir("build/compose/binaries/main/app/${project.name}.wxs").getAsFile()
//
//    val dbf = DocumentBuilderFactory.newInstance()
//    val doc = dbf.newDocumentBuilder().parse(wixFile)
//    doc.documentElement.normalize()
//
//    // 设置 Product 节点
//    //<Product Codepage="" Id="" Language="" Manufacturer="" Name="" UpgradeCode="" Version="1.0">
//    val productElement = doc.documentElement.getElementsByTagName("Product").item(0) as Element
//
//    // 设置升级码, 用于升级,大版本更新时，可能需要修改这个值
//    // 如果要修改这个值，可能还需要修改安装位置，如果不修改安装位置，两个版本会安装在同一个位置
//    // 这段代码和 MajorUpgrade 相关，如果 UpgradeCode 一直保持不变，安装新版的时候会自动卸载旧版本。
//    val upgradeCode = createNameUUID("v1")
//    productElement.apply {
//        setAttribute("Manufacturer", manufacturer)
//        setAttribute("Codepage", "936")
//        // 这个 Name 属性会出现在安装引导界面
//        // 控制面板-程序列表里也是这个名字
//        setAttribute("Name", "${shortcutName}")
//        setAttribute("Version", "${project.version}")
//        setAttribute("UpgradeCode", upgradeCode)
//    }
//
//
//
//    // 设置 Package 节点
//    // <Package Compressed="" InstallerVersion="" Languages="" Manufacturer="" Platform="x64"/>
//    val packageElement = productElement.getElementsByTagName("Package").item(0) as Element
//    packageElement.apply{
//        setAttribute("Compressed", "yes")
//        setAttribute("InstallerVersion", "200")
//        setAttribute("Languages", "1033")
//        setAttribute("Manufacturer", manufacturer)
//        setAttribute("Platform", "x64")
//    }
//
//    //  <Directory Id="TARGETDIR" Name="SourceDir">
//    val targetDirectory = doc.documentElement.getElementsByTagName("Directory").item(0) as Element
//
//    // 桌面文件夹
//    // <Directory Id="DesktopFolder" Name="Desktop" />
//    val desktopFolderElement = directoryBuilder(doc, id = "DesktopFolder").apply{
//        setAttributeNode(doc.createAttribute("Name").also { it.value = "Desktop" })
//    }
//    val desktopGuid = createNameUUID("DesktopShortcutComponent")
//    val desktopComponent = componentBuilder(doc, id = "DesktopShortcutComponent", guid = desktopGuid)
//    val desktopReg = registryBuilder(doc, id = "DesktopShortcutReg", productCode = "[ProductCode]")
//    // <Shortcut Advertise="no" Directory="DesktopFolder" Target = "[INSTALLDIR]${project.name}.exe" Icon="icon.ico" IconIndex="0" Id="DesktopShortcut" Name="$shortcutName" WorkingDirectory="INSTALLDIR"/>
//    val desktopShortcut = shortcutBuilder(
//        doc,
//        id = "DesktopShortcut",
//        directory = "DesktopFolder",
//        workingDirectory = "INSTALLDIR",
//        name = shortcutName,
//        target = "[INSTALLDIR]${project.name}.exe",
//        icon="icon.ico"
//    )
//    //   <RemoveFile Id="DesktopShortcut" On="uninstall" Name="shortcutName.lnk" Directory="DesktopFolder"/>
//    val removeDesktopShortcut = doc.createElement("RemoveFile").apply{
//        setAttributeNode(doc.createAttribute("Id").also { it.value = "DesktopShortcut" })
//        setAttributeNode(doc.createAttribute("On").also { it.value = "uninstall" })
//        setAttributeNode(doc.createAttribute("Name").also { it.value = "$shortcutName.lnk" })
//        setAttributeNode(doc.createAttribute("Directory").also { it.value = "DesktopFolder" })
//    }
//    desktopComponent.appendChild(desktopShortcut)
//    desktopComponent.appendChild(desktopReg)
//    desktopComponent.appendChild(removeDesktopShortcut)
//    desktopFolderElement.appendChild(desktopComponent)
//    targetDirectory.appendChild(desktopFolderElement)
//
//    // 开始菜单文件夹
//    val programMenuFolderElement = directoryBuilder(doc, id = "ProgramMenuFolder", name = "Programs")
//    val programeMenuDir = directoryBuilder(doc, id = "ProgramMenuDir", name = shortcutName)
//    val menuGuid = createNameUUID("programMenuDirComponent")
//    val programMenuDirComponent = componentBuilder(doc, id = "programMenuDirComponent", guid = menuGuid)
//    val startMenuShortcut = shortcutBuilder(
//        doc,
//        id = "startMenuShortcut",
//        directory = "ProgramMenuDir",
//        workingDirectory = "INSTALLDIR",
//        name = shortcutName,
//        target = "[INSTALLDIR]${project.name}.exe",
//        icon="icon.ico"
//    )
//    val uninstallShortcut = shortcutBuilder(
//        doc,
//        id = "uninstallShortcut",
//        name = "卸载$shortcutName",
//        directory = "ProgramMenuDir",
//        target = "[System64Folder]msiexec.exe",
//        arguments = "/x [ProductCode]",
//    )
//    val removeFolder = removeFolderBuilder(doc, id = "CleanUpShortCut", directory = "ProgramMenuDir")
//    val pRegistryValue = registryBuilder(doc, id = "ProgramMenuShortcutReg", productCode = "[ProductCode]")
//
//    programMenuFolderElement.appendChild(programeMenuDir)
//    programeMenuDir.appendChild(programMenuDirComponent)
//    programMenuDirComponent.appendChild(startMenuShortcut)
//    programMenuDirComponent.appendChild(uninstallShortcut)
//    programMenuDirComponent.appendChild(removeFolder)
//    programMenuDirComponent.appendChild(pRegistryValue)
//
//    //<Component Guid="*" Id="RemoveShortcutComponent" Win64="yes">
//    //  <RemoveFile Id="RemoveMenuShortcut" On="uninstall" Name="shortcutName.lnk" Directory="ProgramMenuDir"/>
//    //  <RegistryValue Id="RemoveMenuShortcutReg" Key="Software\manufacturer" KeyPath="yes" Name="ProductCode" Root="HKCU" Type="string" Value="[ProductCode]"/>
//    //</Component>
//    val removeShortcutComponent = componentBuilder(doc, id = "RemoveShortcutComponent", guid = createNameUUID("RemoveShortcutComponent"))
//    val removeMenuShortcut = doc.createElement("RemoveFile").apply{
//        setAttributeNode(doc.createAttribute("Id").also { it.value = "RemoveMenuShortcut" })
//        setAttributeNode(doc.createAttribute("On").also { it.value = "uninstall" })
//        setAttributeNode(doc.createAttribute("Name").also { it.value = "*.lnk" })
//        setAttributeNode(doc.createAttribute("Directory").also { it.value = "ProgramMenuDir" })
//    }
//    val removeMenuShortcutReg = registryBuilder(doc, id = "RemoveMenuShortcutReg", productCode = "[ProductCode]")
//    removeShortcutComponent.appendChild(removeMenuShortcut)
//    removeShortcutComponent.appendChild(removeMenuShortcutReg)
//
//
//    targetDirectory.appendChild(programMenuFolderElement)
//    targetDirectory.appendChild(removeShortcutComponent)
//
//    // 设置所有组件的架构为 64 位
//    val components = doc.documentElement.getElementsByTagName("Component")
//    for (i in 0 until components.length) {
//        val component = components.item(i) as Element
//        val win64 = doc.createAttribute("Win64")
//        win64.value = "yes"
//        component.setAttributeNode(win64)
//    }
//
//    // 添加 ProgramFiles64Folder 节点
//    val programFilesElement = doc.createElement("Directory")
//    val idAttr = doc.createAttribute("Id")
//    idAttr.value = "ProgramFiles64Folder"
//    programFilesElement.setAttributeNode(idAttr)
//    targetDirectory.appendChild(programFilesElement)
//    val installDir = targetDirectory.getElementsByTagName("Directory").item(0)
//    // 移除 installDir 节点
//    val removedNode = targetDirectory.removeChild(installDir)
//    // 将 installDir 节点添加到 programFilesElement 节点
//    programFilesElement.appendChild(removedNode)
//    // 设置安装目录的 Id 为 INSTALLDIR，快捷方式需要引用这个 Id
//    val installDirElement = programFilesElement.getElementsByTagName("Directory").item(0) as Element
//    installDirElement.setAttribute("Id", "INSTALLDIR")
//
//    // 设置 Feature 节点
//    val featureElement = doc.getElementsByTagName("Feature").item(0) as Element
//    featureElement.setAttribute("Id", "Complete")
//    featureElement.setAttribute("Title", "${project.name}")
//
//    // 设置 UI
//    // 添加 <Property Id="WIXUI_INSTALLDIR" Value="INSTALLDIR" />
//    doc.createElement("Property").apply {
//        setAttributeNode(doc.createAttribute("Id").also { it.value = "WIXUI_INSTALLDIR" })
//        setAttributeNode(doc.createAttribute("Value").also { it.value = "INSTALLDIR" })
//    }.also { productElement.appendChild(it) }
//
//
//    //<UI>
//    //  <UIRef Id="WixUI_InstallDir" />
//    //</UI>
//    val uiElement = doc.createElement("UI")
//    productElement.appendChild(uiElement)
//    doc.createElement("UIRef").apply {
//        setAttributeNode(doc.createAttribute("Id").also { it.value = "WixUI_InstallDir" })
//    }.also { uiElement.appendChild(it) }
//
//    if (licensePath.isEmpty()) {
//
//        //  <Publish Dialog="WelcomeDlg"
//        //        Control="Next"
//        //        Event="NewDialog"
//        //        Value="InstallDirDlg"
//        //        Order="2">1</Publish>
//        doc.createElement("Publish").apply {
//            setAttributeNode(doc.createAttribute("Dialog").also { it.value = "WelcomeDlg" })
//            setAttributeNode(doc.createAttribute("Control").also { it.value = "Next" })
//            setAttributeNode(doc.createAttribute("Event").also { it.value = "NewDialog" })
//            setAttributeNode(doc.createAttribute("Value").also { it.value = "InstallDirDlg" })
//            setAttributeNode(doc.createAttribute("Order").also { it.value = "2" })
//            appendChild(doc.createTextNode("1"))
//        }.also { uiElement.appendChild(it) }
//
//        //  <Publish Dialog="InstallDirDlg"
//        //        Control="Back"
//        //        Event="NewDialog"
//        //        Value="WelcomeDlg"
//        //        Order="2">1</Publish>
//        val publish2 = doc.createElement("Publish").apply {
//            setAttributeNode(doc.createAttribute("Dialog").also { it.value = "InstallDirDlg" })
//            setAttributeNode(doc.createAttribute("Control").also { it.value = "Back" })
//            setAttributeNode(doc.createAttribute("Event").also { it.value = "NewDialog" })
//            setAttributeNode(doc.createAttribute("Value").also { it.value = "WelcomeDlg" })
//            setAttributeNode(doc.createAttribute("Order").also { it.value = "2" })
//            appendChild(doc.createTextNode("1"))
//        }.also { uiElement.appendChild(it) }
//
//    }
//
//
//    // 添加 <UIRef Id="WixUI_ErrorProgressText" />
//    val errText = doc.createElement("UIRef").apply {
//        setAttributeNode(doc.createAttribute("Id").also { it.value = "WixUI_ErrorProgressText" })
//    }.also { productElement.appendChild(it) }
//
//
//    //  添加 Icon, 这个 Icon 会显示在控制面板的应用程序列表
//    //  <Icon Id="icon.ico" SourceFile="$iconPath"/>
//    //  <Property Id="ARPPRODUCTICON" Value="icon.ico" />
//    if(iconPath.isNotEmpty()) {
//        doc.createElement("Icon").apply{
//            setAttributeNode(doc.createAttribute("Id").also { it.value = "icon.ico" })
//            setAttributeNode(doc.createAttribute("SourceFile").also { it.value = iconPath })
//        }.also{ productElement.appendChild(it)}
//
//        doc.createElement("Property").apply{
//            setAttributeNode(doc.createAttribute("Id").also { it.value = "ARPPRODUCTICON" })
//            setAttributeNode(doc.createAttribute("Value").also { it.value = "icon.ico" })
//        }.also{ productElement.appendChild(it) }
//
//    }
//
//
//    // 设置 license file
//    //  <WixVariable Id="WixUILicenseRtf" Value="license.rtf" />
//    if (licensePath.isNotEmpty()) {
//        val wixVariable = doc.createElement("WixVariable").apply {
//            setAttributeNode(doc.createAttribute("Id").also { it.value = "WixUILicenseRtf" })
//            setAttributeNode(doc.createAttribute("Value").also { it.value = licensePath })
//        }.also { productElement.appendChild(it) }
//    }
//
//
//    // 安装新版时，自动卸载旧版本，已经安装新版，再安装旧版本，提示用户先卸载新版。
//    // 这段逻辑要和 UpgradeCode 一起设置，如果 UpgradeCode 一直保持不变，安装新版的时候会自动卸载旧版本。
//    // 如果 UpgradeCode 改变了，可能会安装两个版本
//    // <MajorUpgrade AllowSameVersionUpgrades="yes" DowngradeErrorMessage="A newer version of [ProductName] is already installed." AllowSameVersionUpgrades="yes"/>
//    doc.createElement("MajorUpgrade").apply {
//        val errorMessage = "新版的[ProductName]已经安装，如果要安装旧版本，请先把新版本卸载。"
//        setAttributeNode(doc.createAttribute("AllowSameVersionUpgrades").also { it.value = "yes" })
//        setAttributeNode(doc.createAttribute("DowngradeErrorMessage").also { it.value = errorMessage })
//    }.also { productElement.appendChild(it) }
//
//
//    // 设置 fragment 节点
//    val fragmentElement = doc.getElementsByTagName("Fragment").item(0) as Element
//    val componentGroup = fragmentElement.getElementsByTagName("ComponentGroup").item(0) as Element
//    val programMenuDirRef = componentRefBuilder(doc, "programMenuDirComponent")
//    val desktopShortcuRef = componentRefBuilder(doc, "DesktopShortcutComponent")
//    val removeShortcutRef = componentRefBuilder(doc, "RemoveShortcutComponent")
//    componentGroup.appendChild(desktopShortcuRef)
//    componentGroup.appendChild(programMenuDirRef)
//    componentGroup.appendChild(removeShortcutRef)
//
//    generateXml(doc, wixFile)
//}
//
//
//
//
//
//
//
//
//
