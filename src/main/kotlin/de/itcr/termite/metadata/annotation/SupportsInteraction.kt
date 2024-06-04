package de.itcr.termite.metadata.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SupportsInteraction(
    val value: Array<String> = []
)
