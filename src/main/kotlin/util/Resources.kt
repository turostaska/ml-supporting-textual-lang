package util

import java.io.FileNotFoundException

object Resources {
    fun read(name: String) = javaClass.classLoader.getResource(name)?.readText()
        ?: throw FileNotFoundException("Resource with name $name can't be found.")
}