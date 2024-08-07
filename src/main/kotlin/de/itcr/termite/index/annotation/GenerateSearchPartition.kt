package de.itcr.termite.index.annotation

import ca.uhn.fhir.model.api.IResource
import de.itcr.termite.metadata.annotation.SearchParameter
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.instance.model.api.IBaseResource
import java.lang.annotation.Inherited
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class GenerateSearchPartition(
    val target: KClass<out IBaseResource>,
    val param: SearchParameter
)
