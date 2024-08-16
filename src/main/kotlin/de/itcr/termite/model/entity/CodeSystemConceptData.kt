package de.itcr.termite.model.entity

import de.itcr.termite.util.r4b.JsonUtil
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hl7.fhir.r4b.model.CodeSystem
import javax.persistence.*

typealias CSConceptData = CodeSystemConceptData

private typealias CSConcept = CodeSystem.ConceptDefinitionComponent

// TODO: Check if just storing the ConceptDefinitionComponent instance as JSONB is better than destructuring it
//       and only storing certain parts as JSONB
@Entity
@Table(name = "cs_concept", schema = "public")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
data class CodeSystemConceptData (
    @Column(name = "id") @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long?,
    @ManyToOne val cs: CodeSystemData,
    @Column(name = "code") val code: String,
    @Column(name = "display") val display: String?,
    @Column(name = "definition") val definition: String?,
    @Column(name = "designation", columnDefinition = "jsonb") @Type(type = "jsonb") val designation: String?,
    @Column(name = "property", columnDefinition = "jsonb") @Type(type = "jsonb") val property: String?
) {

    // See https://vladmihalcea.com/the-best-way-to-map-a-onetomany-association-with-jpa-and-hibernate/

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CodeSystemConceptData) return false
        return id == other.id
    }

    override fun hashCode(): Int = this::class.hashCode()

}

fun CSConcept.toCSConceptData(cs: CodeSystemData): CodeSystemConceptData {
    return CodeSystemConceptData(
        id?.toLong(),
        cs,
        code,
        display,
        definition,
        JsonUtil.serialize(designation),
        JsonUtil.serialize(property)
    )
}

fun CodeSystemConceptData.toCSConceptDefinitionComponent(): CSConcept {
    val csDefComponent = CSConcept()
    csDefComponent.id = id.toString()
    csDefComponent.code = code
    csDefComponent.display = display
    csDefComponent.definition = definition
    csDefComponent.designation = JsonUtil.deserializeList(designation, "CodeSystem.ConceptDefinitionDesignationComponent")
            as MutableList<CodeSystem.ConceptDefinitionDesignationComponent>
    csDefComponent.property = JsonUtil.deserializeList(property, "CodeSystem.ConceptDefinitionDesignationComponent")
            as MutableList<CodeSystem.ConceptPropertyComponent>
    return csDefComponent
}