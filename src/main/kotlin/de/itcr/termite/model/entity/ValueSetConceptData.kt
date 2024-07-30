package de.itcr.termite.model.entity

import de.itcr.termite.util.r4b.JsonUtil
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import org.hibernate.annotations.TypeDef
import org.hl7.fhir.r4b.model.ValueSet
import javax.persistence.*

typealias VSConceptData = ValueSetConceptData

private typealias Concept = ValueSet.ConceptReferenceComponent
private typealias ConceptDesignation = ValueSet.ConceptReferenceDesignationComponent

@Entity
@Table(name = "vs_concept", schema = "public")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
data class ValueSetConceptData (
    @Column(name = "id") @Id val id: Long,
    @ManyToOne(targetEntity = ValueSetContentData::class) val vsContentData: ValueSetContentData,
    @Column(name = "code") val code: String,
    @Column(name = "display") val display: String?,
    @Column(name = "designation") val designation: String?
)

fun Concept.toVSConceptData(id: Long, vsContentData: ValueSetContentData): ValueSetConceptData {
    return ValueSetConceptData(
        id,
        vsContentData,
        code,
        display,
        JsonUtil.serialize(designation)
    )
}

fun ValueSetConceptData.toVSConceptReferenceComponent(): ValueSet.ConceptReferenceComponent {
    val vsRefComponent = ValueSet.ConceptReferenceComponent()
    vsRefComponent.code = code
    vsRefComponent.display = display
    vsRefComponent.designation = JsonUtil.deserializeList(designation, "ValueSet.ConceptReferenceDesignationComponent")
                as MutableList<ConceptDesignation>
    return vsRefComponent
}