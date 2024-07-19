package de.itcr.termite.index.partition

import de.itcr.termite.metadata.annotation.SearchParameter

interface IFhirSearchIndexPartition<FHIR_MODEL_TYPE, ELEMENT, KEY>: IFhirIndexPartition<FHIR_MODEL_TYPE, KEY, (ELEMENT) -> KEY, (ELEMENT, Int) -> KEY> {

    fun elementPath(): (FHIR_MODEL_TYPE) -> Iterable<ELEMENT>

    fun parameter(): SearchParameter

}