package com.example.app

import com.example.greeting.Greeter
import com.example.legacy.LegacyGreeter

fun main() {
    val greeter = Greeter()

    // `name` is seen as `String?` in Kotlin: com.example.greeting is @NullMarked and the parameter carries
    // @Nullable, so Kotlin knows passing null here is fine.
    println(greeter.greet(null))
    println(greeter.greet("Ada"))

    // `lookupNickname` returns @Nullable String, so Kotlin infers `String?` - the local variable must say so too.
    // val nicknameNonNull: String = greeter.lookupNickname("Ada")
    val nicknameNullable: String? = greeter.lookupNickname("Ada")
    println(nicknameNullable ?: "no nickname")

    // com.example.legacy has no @NullMarked package-info.java, so Kotlin treats LegacyGreeter's members as
    // platform types (String!) instead of String/String?. It compiles either way; see README for what that costs.
    val legacy = LegacyGreeter()
    val greetingNonNull: String = legacy.greet("Ada")
    val greetingNullable: String? = legacy.greet("Ada")
    println(greetingNonNull)
    println(greetingNullable)
}
