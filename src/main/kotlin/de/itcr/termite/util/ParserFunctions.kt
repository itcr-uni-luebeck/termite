package de.itcr.termite.util

import ca.uhn.fhir.parser.IParser
import org.hl7.fhir.instance.model.api.IBaseResource

// TODO: Could impede parallelization due to resource locking!
fun IParser.encodeResourceToString(resource: IBaseResource, summarized: Boolean): String {
    return if (summarized == this.isSummaryMode) this.encodeResourceToString(resource)
    else synchronized(this) {
        val prevVal = isSummaryMode
        isSummaryMode = summarized
        val str = this.encodeResourceToString(resource)
        isSummaryMode = prevVal
        return@synchronized str
    }
}

fun IParser.encodeResourceToString(
    resource: IBaseResource,
    summarized: Boolean,
    dontEncode: Set<String> = emptySet(),
    doEncode: Set<String> = emptySet()
): String {
    synchronized(this) {
        isSummaryMode = summarized
        setDontEncodeElements(dontEncode)
        setEncodeElements(doEncode)
        return encodeResourceToString(resource)
    }
}