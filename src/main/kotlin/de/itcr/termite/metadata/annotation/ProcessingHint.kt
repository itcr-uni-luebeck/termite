package de.itcr.termite.metadata.annotation

import org.hl7.fhir.instance.model.api.IBase
import kotlin.reflect.KClass

annotation class ProcessingHint(
    val targetType: KClass<out IBase>,
    val elementPath: String // FhirPath
)
