package symtab.extensions

import com.kobra.kobraParser

val kobraParser.ClassDeclarationContext.className get() = this.simpleIdentifier().text
    ?: throw RuntimeException("Class name can't be null")

val kobraParser.ClassDeclarationContext.superClasses get() =
    this.delegationSpecifiers()?.delegationSpecifier()?.map { it.simpleIdentifier().text } ?: emptyList()
