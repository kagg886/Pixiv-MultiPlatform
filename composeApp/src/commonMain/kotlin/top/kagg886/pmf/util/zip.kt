package top.kagg886.pmf.util

import okio.FileSystem
import okio.Path
import okio.SYSTEM

expect fun Path.zip(target: Path = FileSystem.SYSTEM.canonicalize(this).parent!!.resolve("${this.name}.zip")): Path
