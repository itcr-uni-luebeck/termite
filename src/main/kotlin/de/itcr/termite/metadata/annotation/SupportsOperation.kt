package de.itcr.termite.metadata.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class SupportsOperation(
    val url: String = "", // If not provided, then it should be automatically generated
    val name: String,
    val title: String = "",
    val status: String = "draft",
    val kind: String,
    val experimental: Boolean = true,
    val description : String = "",
    val affectState: Boolean = false,
    val code: String,
    val comment: String = "",
    val resource: Array<String> = [],
    val system: Boolean,
    val type: Boolean,
    val instance: Boolean,
    val parameter: Array<Parameter> = []
)