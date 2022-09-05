package com.ericversteeg.radiofrost.model

class Command(val name: String, val argSyntax: String, val argRegex: Regex) {

    var replacedArgSyntax = ""
}