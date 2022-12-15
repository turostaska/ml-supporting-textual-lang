package util

import java.io.File

fun File.pathIfExistsElseNull(): String? = if (this.exists()) this.absolutePath else null
