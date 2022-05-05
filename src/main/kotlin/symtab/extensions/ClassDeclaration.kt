package symtab.extensions

import kobraParser.ClassDeclarationContext

val ClassDeclarationContext.className get() = this.simpleIdentifier().text
    ?: throw RuntimeException("Class name can't be null")

val ClassDeclarationContext.superClasses get() =
    this.delegationSpecifiers()?.delegationSpecifier()?.map { it.simpleIdentifier().text } ?: emptyList()
