package com.microservice.kotlin

import com.microservice.authentication.common.model.Authentication
import com.microservice.authentication.common.model.Authority
import com.microservice.kotlin.model.Task
import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar

class CustomRuntimeHintsRegistrar : RuntimeHintsRegistrar {
    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        hints.reflection().registerType(Task::class.java, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
        hints.serialization().registerType(Authentication::class.java)
        hints.serialization().registerType(Authority::class.java)
    }
}
