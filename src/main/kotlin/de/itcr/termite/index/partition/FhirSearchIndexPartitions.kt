package de.itcr.termite.index.partition

import org.hl7.fhir.instance.model.api.IAnyResource

interface FhirSearchIndexPartitions<FHIR_MODEL_TYPE, ELEMENT, KEY>:
    FhirIndexPartitions<FHIR_MODEL_TYPE, ELEMENT, Int, KEY, (ELEMENT) -> KEY, (ELEMENT, Int) -> KEY>