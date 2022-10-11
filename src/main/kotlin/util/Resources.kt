package util

import java.io.FileNotFoundException

object Resources {
    fun readOrNull(name: String) = javaClass.classLoader.getResource(name)?.readText()

    fun read(name: String) = readOrNull(name)
        ?: throw FileNotFoundException("Resource with name $name can't be found.")
}