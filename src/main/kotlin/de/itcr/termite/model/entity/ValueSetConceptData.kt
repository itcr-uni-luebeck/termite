package de.itcr.termite.model.entity

import de.itcr.termite.util.r4b.JsonUtil
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import org.hibernate.annotations.TypeDef
import org.hl7.fhir.r4b.model.ValueSet
import javax.persistence.*

typealias VSConceptData = ValueSetConceptData

private typealias VSConcept = ValueSet.ConceptReferenceComponent
private typealias VSConceptDesignation = ValueSet.ConceptReferenceDesignationComponent

@Entity
@Table(name = "vs_concept", schema = "public")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
data class ValueSetConceptData (
    @Column(name = "id") @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long?,
    @ManyToOne val vsContentData: ValueSetContentData,
    @Column(name = "code") val code: String,
    @Column(name = "display") val display: String?,
    @Column(name = "designation") val designation: String?
) {

    // See https://vladmihalcea.com/the-best-way-to-map-a-onetomany-association-with-jpa-and-hibernate/

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ValueSetConceptData) return false
        return id == other.id
    }

    override fun hashCode(): Int = this::class.hashCode()

}

fun VSConcept.toVSConceptData(vsContentData: ValueSetContentData): ValueSetConceptData {
    return ValueSetConceptData(
        id?.toLong(),
        vsContentData,
        code,
        display,
        JsonUtil.serialize(designation)
    )
}

fun ValueSetConceptData.toVSConceptReferenceComponent(): VSConcept {
    val vsRefComponent = VSConcept()
    vsRefComponent.id = id.toString()
    vsRefComponent.code = code
    vsRefComponent.display = display
    vsRefComponent.designation = JsonUtil.deserializeList(designation, "ValueSet.ConceptReferenceDesignationComponent")
                as MutableList<VSConceptDesignation>
    return vsRefComponent
}