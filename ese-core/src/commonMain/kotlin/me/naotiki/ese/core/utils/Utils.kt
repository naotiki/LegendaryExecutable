package me.naotiki.ese.core.utils

import kotlinx.coroutines.yield
import kotlin.reflect.KClass

fun String.splitArgs()= replace(" {2,}".toRegex()," ").split(' ')

fun <E : Enum<E>> Enum<E>.getFlag(): Int = 1 shl ordinal

fun normalizeYesNoAnswer(input:String): Boolean? {
    return when (input.lowercase()) {
        "y" -> true
        "yes"->true
        "n"->false
        "no"->false
        else -> null
    }
}

suspend inline fun loop( block: ()->Unit){
    while (true) {
        block()
        yield()
    }
}
expect fun String.format(vararg args:Any?):String