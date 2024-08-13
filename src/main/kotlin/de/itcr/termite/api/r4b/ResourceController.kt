package de.itcr.termite.api.r4b

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.rest.api.PreferHandlingEnum
import de.itcr.termite.config.ApplicationConfig
import de.itcr.termite.exception.api.MissingParameterException
import de.itcr.termite.exception.api.UnsupportedParameterException
import de.itcr.termite.exception.api.UnsupportedValueException
import de.itcr.termite.metadata.annotation.*
import de.itcr.termite.metadata.annotation.SearchParameter
import de.itcr.termite.persistence.r4b.FhirPersistenceManager
import de.itcr.termite.util.r4b.parseParamMaxValue
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
                elementPath = "id",
                special = true
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
            operationParameterMap[opCode] = op.parameter.filter { it.use == "in" }.associateBy { it.name }
        }
    }

    fun validateSearchParameters(
        parameters: Map<String, List<String>>,
        handling: PreferHandlingEnum,
        apiPath: String,
        method: HttpMethod,
        exemptions: Set<String> = setOf("_format")
    ): Map<String, List<String>> {
        if ("code" in parameters) {
            for (value in parameters["code"]!!) {
                val code = value.trim()
                if (code.startsWith("|") || !code.contains('|'))
                    throw UnsupportedValueException("Parameter 'code' with value '$code' has to feature a system value")
            }
        }
        return if (handling == PreferHandlingEnum.LENIENT) parameters.filterKeys { key -> key in searchParameterMap.keys }
        else {
            val diff = parameters.keys - searchParameterMap.keys - exemptions
            if (diff.isNotEmpty()) throw UnsupportedParameterException(diff.elementAt(0), apiPath, method)
            parameters.filter { entry -> entry.key !in exemptions }
        }
    }

    fun validateOperationParameters(
        operationCode: String,
        parameters: Map<String, List<String>>,
        handling: PreferHandlingEnum,
        apiPath: String,
        method: HttpMethod,
        exemptions: Set<String> = setOf("_format")
    ): Map<String, List<String>> {
        val opParameterMap = operationParameterMap[operationCode]!!
        // Handle unknown parameters based on handling strategy
        val curatedParameters = if (handling == PreferHandlingEnum.LENIENT) {
            parameters.filter { entry -> entry.key in opParameterMap.keys }.toMutableMap()
        }
        else {
            val diff = parameters.keys - opParameterMap.keys - exemptions
            if (diff.isNotEmpty()) throw UnsupportedParameterException(diff.elementAt(0), apiPath, method)
            parameters.filter { entry -> entry.key !in exemptions }.toMutableMap()
        }
        // Check if parameter occurrence respects boundaries defined by min and max attributes
        opParameterMap.forEach { (name, paramDef) ->
            val value = parameters[name]
            curatedParameters[name] = value ?: emptyList()
            val amount = value?.size ?: 0
            if (amount < paramDef.min || amount > parseParamMaxValue(paramDef.max))
                throw UnsupportedParameterException("Occurrence of parameter '$name' not in range [${paramDef.min}, ${paramDef.max}]")
        }
        return curatedParameters
    }

}
