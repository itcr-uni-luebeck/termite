package de.itcr.termite.metadata.annotation.interaction.instance

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SupportsVRead(
    val readHistory: Boolean = false
)