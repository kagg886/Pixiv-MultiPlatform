package top.kagg886.epub

import okio.Path
import okio.use
import top.kagg886.epub.data.Metadata
import top.kagg886.epub.data.ResourceItem
import top.kagg886.epub.data.Spine
import top.kagg886.epub.nodes.container.container
import top.kagg886.epub.nodes.container.rootFile
import top.kagg886.epub.nodes.container.rootFiles
import top.kagg886.epub.nodes.opf.*
import top.kagg886.epub.nodes.toc.navMap
import top.kagg886.epub.nodes.toc.navPoint
import top.kagg886.epub.nodes.toc.ncx
import top.kagg886.pmf.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class Epub internal constructor(
    private val temp: Path,
    private val metadata: Metadata,
    private val resources: List<ResourceItem>,
    private val spine: Spine? = null,
) {
    @OptIn(ExperimentalUuidApi::class)
    fun writeTo(path: Path) {
        val workDirBase = temp.resolve(Uuid.random().toHexString())
        workDirBase.mkdirs()

        with(workDirBase.resolve("mimetype")) {
            parentFile()?.mkdirs()
            createNewFile()
            writeString("application/epub+zip")
        }

        with(workDirBase.resolve("META-INF").resolve("container.xml")) {
            parentFile()?.mkdirs()
            createNewFile()
            writeString(
                with(RootScope) {
                    container {
                        rootFiles {
                            rootFile("EPUB/package.opf")
                        }
                    }
                }
            )
        }

        with(workDirBase.resolve("EPUB").resolve("package.opf")) {
            parentFile()?.mkdirs()
            createNewFile()
            writeString(
                with(RootScope) {
                    pkg {
                        metadata { //epub元数据
                            dcTitle(metadata.title)
                            metadata.description?.let {
                                dcDescription(it)
                            }
                            metadata.creator?.let {
                                dcCreator(it)
                            }
                            metadata.publisher?.let {
                                dcPublisher(it)
                            }
                            metadata.rights?.let {
                                dcRights(it)
                            }
                            dcIdentifier(metadata.identifier)
                            dcLanguage(metadata.language)
                            for ((k, v) in metadata.meta) {
                                dcMeta(k, v)
                            }
                        }

                        manifest { //epub资源目录
                            for (resource in resources) {
                                with(workDirBase.resolve("EPUB").resolve(resource.fileName)) {
                                    parentFile()?.mkdirs()
                                    createNewFile()
                                    this.sink().use { resource.file.transfer(it) }
                                }
                                item(
                                    id = resource.uuid,
                                    href = resource.fileName,
                                    mediaType = resource.mediaType,
                                    properties = resource.properties
                                )
                            }

                            //TOC存在则注册ncx文件
                            if (spine != null) {
                                item(id = "ncx", href = "toc.ncx", mediaType = "application/x-dtbncx+xml")
                            }
                        }

                        if (spine != null) {
                            //导航。TOC为描述，内部结构则规定滑动顺序。
                            spine(toc = "ncx") {
                                for (ref in spine.refs) {
                                    check(resources.contains(ref)) {
                                        "Resource $ref not found in resources"
                                    }
                                    itemRef(idref = ref.uuid)
                                }
                            }
                        }
                    }
                }
            )
        }

        if (spine != null) {
            with(workDirBase.resolve("EPUB").resolve("toc.ncx")) {
                parentFile()?.mkdirs()
                createNewFile()
                writeString(
                    with(RootScope) {
                        ncx {
                            navMap {
                                for ((index, toc) in spine.toc.withIndex()) {
                                    check(resources.contains(toc.item)) {
                                        "Resource ${toc.item} not found in resources"
                                    }
                                    navPoint(
                                        id = toc.item.uuid,
                                        playOrder = index.toString(),
                                        label = toc.title,
                                        content = toc.item.fileName
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        workDirBase.zip(path)
        workDirBase.deleteRecursively()
    }
}
