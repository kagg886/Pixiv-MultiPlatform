package top.kagg886.compose.installer.windows.top.kagg886.compose.installer.windows.util

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

internal fun componentRefBuilder(doc: Document, id: String): Element {
    val componentRef = doc.createElement("ComponentRef")
    val attrId = doc.createAttribute("Id")
    attrId.value = id
    componentRef.setAttributeNode(attrId)
    return componentRef
}


internal fun removeFolderBuilder(doc: Document, id: String, directory: String): Element {
    val removeFolder = doc.createElement("RemoveFolder").apply {
        setAttributeNode(doc.createAttribute("Id").also { it.value = id })
        setAttributeNode(doc.createAttribute("Directory").also { it.value = directory })
        setAttributeNode(doc.createAttribute("On").also { it.value = "uninstall" })
    }
    return removeFolder
}


internal fun shortcutBuilder(
    doc: Document,
    id: String,
    directory: String = "",
    workingDirectory: String = "",
    name: String,
    target: String = "",
    description: String = "",
    arguments: String = "",
    icon: String = ""
): Element {
    val shortcut = doc.createElement("Shortcut")
    val shortcutId = doc.createAttribute("Id")
    shortcutId.value = id
    val shortcutName = doc.createAttribute("Name")
    shortcutName.value = name
    val advertise = doc.createAttribute("Advertise")
    advertise.value = "no"

    shortcut.setAttributeNode(shortcutId)
    shortcut.setAttributeNode(shortcutName)
    shortcut.setAttributeNode(advertise)

    if (target.isNotEmpty()) {
        val shortcutTarget = doc.createAttribute("Target")
        shortcutTarget.value = target
        shortcut.setAttributeNode(shortcutTarget)
    }

    if (directory.isNotEmpty()) {
        val shortcutDir = doc.createAttribute("Directory")
        shortcutDir.value = directory
        shortcut.setAttributeNode(shortcutDir)
    }

    if (workingDirectory.isNotEmpty()) {
        val shortcutWorkDir = doc.createAttribute("WorkingDirectory")
        shortcutWorkDir.value = workingDirectory
        shortcut.setAttributeNode(shortcutWorkDir)
    }
    if (description.isNotEmpty()) {
        val shortcutDescription = doc.createAttribute("Description")
        shortcutDescription.value = description
        shortcut.setAttributeNode(shortcutDescription)
    }

    if (arguments.isNotEmpty()) {
        val shortcutArguments = doc.createAttribute("Arguments")
        shortcutArguments.value = arguments
        shortcut.setAttributeNode(shortcutArguments)
    }
    if (icon.isNotEmpty()) {
        val shortcutIcon = doc.createAttribute("Icon")
        shortcutIcon.value = icon
        shortcut.setAttributeNode(shortcutIcon)
    }

    return shortcut
}


internal fun componentBuilder(doc: Document, id: String, guid: String): Element {
    val component = doc.createElement("Component")
    val scAttrId = doc.createAttribute("Id")
    scAttrId.value = id
    component.setAttributeNode(scAttrId)
    val scGuid = doc.createAttribute("Guid")
    scGuid.value = guid
    component.setAttributeNode(scGuid)
    return component
}

internal fun registryBuilder(doc: Document, id: String, productCode: String,manufacturer:String,appName:String): Element {
    val regComponentElement = doc.createElement("RegistryValue")
    val regAttrId = doc.createAttribute("Id")
    regAttrId.value = id
    val regAttrRoot = doc.createAttribute("Root")
    regAttrRoot.value = "HKCU"
    val regKey = doc.createAttribute("Key")
    regKey.value = "Software\\$manufacturer\\$appName"
    val regType = doc.createAttribute("Type")
    regType.value = "string"
    val regName = doc.createAttribute("Name")
    regName.value = "ProductCode"
    val regValue = doc.createAttribute("Value")
    regValue.value = productCode
    val regKeyPath = doc.createAttribute("KeyPath")
    regKeyPath.value = "yes"
    regComponentElement.setAttributeNode(regAttrId)
    regComponentElement.setAttributeNode(regAttrRoot)
    regComponentElement.setAttributeNode(regAttrRoot)
    regComponentElement.setAttributeNode(regKey)
    regComponentElement.setAttributeNode(regType)
    regComponentElement.setAttributeNode(regName)
    regComponentElement.setAttributeNode(regValue)
    regComponentElement.setAttributeNode(regKeyPath)
    return regComponentElement
}
internal fun directoryBuilder(doc: Document, id: String, name: String = ""): Element {
    val directory = doc.createElement("Directory")
    val attrId = doc.createAttribute("Id")
    attrId.value = id
    directory.setAttributeNode(attrId)
    if (name.isNotEmpty()) {
        val attrName = doc.createAttribute("Name")
        attrName.value = name
        directory.setAttributeNode(attrName)
    }
    return directory
}

internal fun generateXml(doc: Document, file: File) {

    // Instantiate the Transformer
    val transformerFactory = TransformerFactory.newInstance()
    transformerFactory.setAttribute("indent-number", 4)
    val transformer = transformerFactory.newTransformer()

    // Enable indentation and set encoding
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")

    val source = DOMSource(doc)
    val result = StreamResult(file)
    transformer.transform(source, result)
}
