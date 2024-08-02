package de.itcr.termite.api.r4b

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.api.PreferHandlingEnum
import de.itcr.termite.config.ApplicationConfig
import de.itcr.termite.exception.api.UnsupportedParameterException
import de.itcr.termite.metadata.annotation.*
import de.itcr.termite.metadata.annotation.SearchParameter
import de.itcr.termite.persistence.r4b.FhirPersistenceManager
import org.apache.logging.log4j.Logger
import org.hl7.fhir.r4b.model.*
import org.springframework.http.HttpMethod
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations

@ForResource(
    type = "Resource",
    searchParam = [
        SearchParameter(
            name = "_id",
            type = "token",
            documentation = "Logical id of this artifact",
            processing = ProcessingHint(
                targetType = StringType::class,
                elementPath = "id"
            )
        ),
        SearchParameter(
            name = "_lastUpdated",
            type = "date",
            documentation = "When the resource version last changed",
            processing = ProcessingHint(
                targetType = InstantType::class,
                elementPath = "meta.lastUpdated"
            )
        ),
        SearchParameter(
            name = "_profile",
            type = "uri",
            documentation = "Profiles this resource claims to conform to",
            processing = ProcessingHint(
                targetType = CanonicalType::class,
                elementPath = "meta.profile"
            )
        ),
        SearchParameter(
            name = "_source",
            type = "uri",
            documentation = "Identifies where the resource comes from",
            processing = ProcessingHint(
                targetType = UriType::class,
                elementPath = "meta.source"
            )
        ),
        SearchParameter(
            name = "_security",
            type = "token",
            documentation = "Security Labels applied to this resource",
            processing = ProcessingHint(
                targetType = CodeType::class,
                elementPath = "meta.security"
            )
        ),
        SearchParameter(
            name = "_tag",
            type = "token",
            documentation = "Tags applied to this resource",
            processing = ProcessingHint(
                targetType = CodeType::class,
                elementPath = "meta.tag"
            )
        )
    ]
)
abstract class ResourceController<TYPE, ID>(
    open val persistence: FhirPersistenceManager<TYPE, ID>,
    fhirContext: FhirContext,
    properties: ApplicationConfig,
    logger: Logger
): FhirController(fhirContext, properties, logger) {

    val searchParameterMap: Map<String, SearchParameter>
    val operationParameterMap: Map<String, Map<String, Parameter>>

    init {
        // Get class calling this classes constructor (some direct subclass) from call stack
        val inheritingClass = Class.forName(Thread.currentThread().stackTrace[2].className)

        // Search parameters
        val paramAnnList = inheritingClass.kotlin.findAnnotation<ForResource>()?.searchParam?.toMutableList() ?: mutableListOf()
        paramAnnList.addAll(ResourceController::class.findAnnotation<ForResource>()!!.searchParam)
        this.searchParameterMap = paramAnnList.associateBy { it.name }

        // Operation parameters
        operationParameterMap = mutableMapOf()
        inheritingClass.kotlin.findAnnotations<SupportsOperation>().map { op ->
            val opCode = op.code
            operationParameterMap[opCode] = op.parameter.filter { it.type == "in" }.associateBy { it.name }
        }
        paramAnnList.addAll(ResourceController::class.findAnnotation<ForResource>()!!.searchParam)
    }

    fun validateSearchParameters(
        parameters: Map<String, String>,
        handling: PreferHandlingEnum,
        apiPath: String,
        method: HttpMethod,
        exemptions: Set<String> = setOf("_format")
    ): Map<String, String> {
        return if (handling == PreferHandlingEnum.LENIENT) parameters.filter { entry -> entry.key in searchParameterMap.keys }
        else {
            val diff = parameters.keys - searchParameterMap.keys - exemptions
            if (diff.isNotEmpty()) throw UnsupportedParameterException(diff.elementAt(0), apiPath, method)
            parameters.filter { entry -> entry.key !in exemptions }
        }
    }

    fun validateOperationParameters(
        operationCode: String,
        parameters: Map<String, String>,
        handling: PreferHandlingEnum,
        apiPath: String,
        method: HttpMethod,
        exemptions: Set<String> = setOf("_format")
    ): Map<String, String> {
        val opParameterMap = operationParameterMap[operationCode]!!
        return if (handling == PreferHandlingEnum.LENIENT) parameters.filter { entry -> entry.key in  opParameterMap.keys}
        else {
            val diff = parameters.keys - opParameterMap.keys - exemptions
            if (diff.isNotEmpty()) throw UnsupportedParameterException(diff.elementAt(0), apiPath, method)
            parameters.filter { entry -> entry.key !in exemptions }
        }
    }

}
