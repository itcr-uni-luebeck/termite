package de.itcr.termite.model.lazy

object AlwaysInitialized: Lazy<Nothing> {

    override val value: Nothing
        get() = throw UnsupportedOperationException("Returns no value by design")

    override fun isInitialized(): Boolean = true

}