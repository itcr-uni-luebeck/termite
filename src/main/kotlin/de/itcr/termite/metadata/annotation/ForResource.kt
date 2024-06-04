package de.itcr.termite.metadata.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ForResource(
    val type: String,
    val documentation: String = "",
    val versioning: String = "no-version",
    val readHistory: Boolean = false,
    val updateCreate: Boolean = false,
    val conditionalCreate: Boolean = false,
    val conditionalRead: String = "not-supported",
    val conditionalUpdate: Boolean = false,
    val conditionalDelete: String = "not-supported",
    val referencePolicy: Array<String> = [],
    val searchInclude: Array<String> = [],
    val searchRevInclude: Array<String> = [],
    val searchParam: Array<SearchParameter> = []
)
