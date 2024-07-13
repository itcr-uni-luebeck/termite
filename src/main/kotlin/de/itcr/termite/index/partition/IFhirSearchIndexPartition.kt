package de.itcr.termite.index.partition

interface IFhirSearchIndexPartition<FHIR_MODEL_TYPE, ELEMENT, KEY>:
    IFhirIndexPartition<FHIR_MODEL_TYPE, ELEMENT, Int, KEY, (ELEMENT) -> KEY, (ELEMENT, Int) -> KEY>