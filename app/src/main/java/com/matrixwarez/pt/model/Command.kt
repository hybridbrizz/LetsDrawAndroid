package com.matrixwarez.pt.model

class Command(val name: String, val argSyntax: String, val argRegex: Regex) {

    var replacedArgSyntax = ""
}