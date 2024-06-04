package de.itcr.termite.metadata

import de.itcr.termite.metadata.annotation.ForResource
import de.itcr.termite.metadata.annotation.Parameter
import de.itcr.termite.metadata.annotation.SupportsInteraction
import de.itcr.termite.metadata.annotation.SupportsOperation
import de.itcr.termite.util.ResourceUtils
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.hl7.fhir.r4b.model.*
import org.hl7.fhir.r4b.model.CapabilityStatement.CapabilityStatementRestComponent
import org.hl7.fhir.r4b.model.CapabilityStatement.CapabilityStatementRestResourceComponent
import org.hl7.fhir.r4b.model.OperationDefinition.OperationDefinitionParameterComponent
import org.springframework.stereotype.Controller
import java.net.URI
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations

object MetadataCompiler {

    private val logger: Logger = LogManager.getLogger(MetadataCompiler::class)
    private val classLoader: ClassLoader = Thread.currentThread().contextClassLoader

    fun compileStaticFhirServerMetadata(apiPackageName: String, baseUrl: URI): Pair<CapabilityStatement, Array<OperationDefinition>>
    {
        logger.info("Compiling static FHIR terminology server capabilities (CapabilityStatement)")
        // Load all classes in API package
        val classes = ResourceUtils.findClassesInPackage(apiPackageName, classLoader)
        return compileCapabilitiesFromAnnotations(classes, baseUrl)
    }

    // TODO: Implement method properly
    fun compileTerminologyCapabilities(): TerminologyCapabilities {
        return TerminologyCapabilities()
    }

    private fun compileCapabilitiesFromAnnotations(classes: Collection<KClass<*>>, baseUrl: URI):
            Pair<CapabilityStatement, Array<OperationDefinition>>
    {
        val capabilityStatement = CapabilityStatement()
        val operationDefinitions: MutableList<OperationDefinition> = mutableListOf()
        // Set basic metadata
        with(capabilityStatement) {
            url = baseUrl.resolve("metadata").path
            version = "1.0.0"
            status = Enumerations.PublicationStatus.ACTIVE
            experimental = false
            date = Date.from(Instant.now())
            kind = Enumerations.CapabilityStatementKind.INSTANCE
            fhirVersion = Enumerations.FHIRVersion._4_3_0_CIBUILD
            format = listOf(CodeType("xml"), CodeType("json"))
        }

        // Add RESTful interactions and operations
        val resources: MutableMap<ResourceType, CapabilityStatementRestResourceComponent> = mutableMapOf()
        val restComponent = capabilityStatement.addRest()

        val servletClasses = classes.filter { clazz -> clazz.findAnnotation<Controller>() != null }
        servletClasses.forEach { servletClass ->
            val forResource = servletClass.findAnnotation<ForResource>()
            // Check if there is already a component for the resource type and add one if not
            if (forResource != null) {
                val resourceType = ResourceType.fromCode(forResource.type)
                if (!resources.contains(resourceType)) resources[resourceType] =
                    addRestResourceComponent(forResource, restComponent)
            }

            val resourceType = if (forResource != null) ResourceType.fromCode(forResource.type) else null

            servletClass.findAnnotations<SupportsOperation>().forEach { ann ->
                val operationList =
                    if (resourceType != null) resources[resourceType]!!.operation else restComponent.operation
                addOperation(ann, baseUrl, operationList, operationDefinitions)
            }
            val supportInteraction = servletClass.findAnnotation<SupportsInteraction>()
            if (supportInteraction != null) {
                if (forResource != null) {
                    val resourceComponent = resources[ResourceType.fromCode(forResource.type)]!!
                    addAllResourceInteractions(supportInteraction, resourceComponent)
                }
                else {
                    addAllSystemInteractions(supportInteraction, restComponent)
                }
            }
            else {
                logger.debug("Servlet class ${servletClass.qualifiedName} is ignored due to lack of annotation")
            }
        }

        return Pair(capabilityStatement, operationDefinitions.toTypedArray())
    }

    private fun addRestResourceComponent(ann: ForResource, restComponent: CapabilityStatementRestComponent): CapabilityStatementRestResourceComponent
    {
        return restComponent.addResource().apply {
            type = ann.type
            documentation = ann.documentation
            versioning = CapabilityStatement.ResourceVersionPolicy.fromCode(ann.versioning)
            readHistory = ann.readHistory
            updateCreate = ann.updateCreate
            conditionalCreate = ann.conditionalCreate
            conditionalRead = CapabilityStatement.ConditionalReadStatus.fromCode(ann.conditionalRead)
            conditionalUpdate = ann.conditionalUpdate
            conditionalDelete = CapabilityStatement.ConditionalDeleteStatus.fromCode(ann.conditionalDelete)
            ann.referencePolicy.forEach {
                policy -> addReferencePolicy(CapabilityStatement.ReferenceHandlingPolicy.fromCode(policy))
            }
            ann.searchInclude.forEach { include -> addSearchInclude(include) }
            ann.searchRevInclude.forEach { include -> addSearchRevInclude(include) }
        }
    }

    private fun addOperation(ann: SupportsOperation,
                             baseURL: URI,
                             operationList: MutableList<CapabilityStatement.CapabilityStatementRestResourceOperationComponent>,
                             operationDefinitions: MutableList<OperationDefinition>)
    {
        val opDefUrl = "$baseURL/OperationDefinition/${ann.resource.joinToString("-") { it.lowercase() }}-${ann.code}"
        operationList.add(CapabilityStatement.CapabilityStatementRestResourceOperationComponent().apply {
            name = ann.name
            definition = opDefUrl
            documentation = ann.description
        })
        operationDefinitions.add(getOperationDefinition(ann, opDefUrl))
    }

    private fun getOperationDefinition(ann: SupportsOperation, opDefUrl: String): OperationDefinition
    {
        return OperationDefinition().apply {
            id = opDefUrl.split("/").last()
            url = opDefUrl
            version = "1.0.0"
            name = ann.name
            title = ann.title
            status = Enumerations.PublicationStatus.fromCode(ann.status)
            kind = OperationDefinition.OperationKind.fromCode(ann.kind)
            experimental = ann.experimental
            affectsState = ann.affectState
            code = ann.code
            comment = ann.comment
            description = ann.description
            system = ann.system
            type = ann.type
            instance = ann.instance
            parameter = getOperationDefParameters(ann.parameter)
        }
    }

    private fun getOperationDefParameters(parameters: Array<Parameter>): List<OperationDefinitionParameterComponent>
    {
        val parameterList = mutableListOf<OperationDefinitionParameterComponent>()
        parameterList.addAll(
            Arrays.stream(parameters).map { parameter -> OperationDefinitionParameterComponent().apply {
                name = parameter.name
                use = Enumerations.OperationParameterUse.fromCode(parameter.use)
                min = parameter.min
                max = parameter.max
                documentation = parameter.documentation
                type = Enumerations.FHIRAllTypes.fromCode(parameter.type)
            } }.toList()
        )
        return parameterList
    }

    private fun addAllResourceInteractions(ann: SupportsInteraction,
                                           resourceComponent: CapabilityStatementRestResourceComponent)
    {
        resourceComponent.interaction.addAll(
            Arrays.stream(ann.value)
                .map { s -> CapabilityStatement.ResourceInteractionComponent(CapabilityStatement.TypeRestfulInteraction.fromCode(s)) }
                .toList()
        )
    }

    private fun addAllSystemInteractions(ann: SupportsInteraction,
                                         restComponent: CapabilityStatementRestComponent)
    {
        restComponent.interaction.addAll(
            Arrays.stream(ann.value)
                .map { s -> CapabilityStatement.SystemInteractionComponent(CapabilityStatement.SystemRestfulInteraction.fromCode(s)) }
                .toList()
        )
    }

}