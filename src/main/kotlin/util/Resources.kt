package util

import java.io.File

object Resources {
    fun readFromClasspathOrNull(name: String) = javaClass.classLoader.getResource(name)?.readText()

    fun read(name: String) = readFromClasspathOrNull(name)
        ?: File(name).readText()

    fun read(name: String, readKModule: Boolean): String {
        val code = read(name)

        if (!readKModule) return code

        val kModuleCode = read("KModule.kb")

        val lines = code.lines().toMutableList()
        val insertAt = lines.indexOfFirst { !it.startsWith("import") }
        lines.add(insertAt, kModuleCode)

        return lines.joinToString(System.lineSeparator())
    }

    fun readOrNull(name: String): String? {
        return readFromClasspathOrNull(name)
            ?: File(name).let {
                if (it.exists()) it.readText() else null
            }
    }
}