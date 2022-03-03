import java.io.FileNotFoundException

object Resources {
    fun get(name: String) = javaClass.getResource(name)?.readText()
        ?: throw FileNotFoundException("Resource with name $name can't be found.")
}

fun main() {
    MyTestListener(Resources.get("hello1")).getKotlinCode().also(::println)
}