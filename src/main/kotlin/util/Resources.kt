package util

import java.io.File

object Resources {
    fun readFromClasspathOrNull(name: String) = javaClass.classLoader.getResource(name)?.readText()

    fun read(name: String) = readFromClasspathOrNull(name)
        ?: File(name).readText()
}