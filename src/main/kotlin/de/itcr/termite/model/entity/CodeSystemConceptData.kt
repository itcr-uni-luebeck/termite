package de.itcr.termite.model.entity

import de.itcr.termite.util.r4b.JsonUtil
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hl7.fhir.r4b.model.CodeSystem
import javax.persistence.*

typealias CSConceptData = CodeSystemConceptData

// TODO: Check if just storing the ConceptDefinitionComponent instance as JSONB is better than destructuring it
//       and only storing certain parts as JSONB
@Entity
@Table(name = "cs_concept", schema = "public")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
data class CodeSystemConceptData (
    @Column(name = "id") @Id val id: Long,
    @ManyToOne(targetEntity = CodeSystemMetadata::class) val cs: CodeSystemMetadata,
    @Column(name = "code") val code: String,
    @Column(name = "display") val display: String?,
    @Column(name = "definition") val definition: String?,
    @Column(name = "designation", columnDefinition = "jsonb") @Type(type = "jsonb") val designation: String?,
    @Column(name = "property", columnDefinition = "jsonb") @Type(type = "jsonb") val property: String?
)

fun CodeSystem.ConceptDefinitionComponent.toCSConceptData(id: Long, cs: CodeSystemMetadata): CodeSystemConceptData {
    return CodeSystemConceptData(
        id,
        cs,
        code,
        display,
        definition,
        JsonUtil.serialize(designation),
        JsonUtil.serialize(property)
    )
}

fun CodeSystem.ConceptDefinitionComponent.toCSConceptData(cs: CodeSystemMetadata): CodeSystemConceptData = this.toCSConceptData(0, cs)

fun CodeSystemConceptData.toCSConceptDefinitionComponent(): CodeSystem.ConceptDefinitionComponent {
    val csDefComponent = CodeSystem.ConceptDefinitionComponent()
    csDefComponent.code = code
    csDefComponent.display = display
    csDefComponent.definition = definition
    csDefComponent.designation = JsonUtil.deserializeList(designation, "CodeSystem.ConceptDefinitionDesignationComponent")
            as MutableList<CodeSystem.ConceptDefinitionDesignationComponent>
    csDefComponent.property = JsonUtil.deserializeList(property, "CodeSystem.ConceptDefinitionDesignationComponent")
            as MutableList<CodeSystem.ConceptPropertyComponent>
    return csDefComponent
}