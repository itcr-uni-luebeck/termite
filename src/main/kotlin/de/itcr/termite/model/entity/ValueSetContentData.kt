package de.itcr.termite.model.entity

import de.itcr.termite.model.lazy.LazyConceptSetComponent
import de.itcr.termite.util.r4b.JsonUtil
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hl7.fhir.r4b.model.CanonicalType
import org.hl7.fhir.r4b.model.ValueSet
import javax.persistence.*

typealias VSContentData = ValueSetContentData
typealias VSIncludeData = ValueSetIncludeData
typealias VSExcludeData = ValueSetExcludeData

private typealias ConceptSetComponent = ValueSet.ConceptSetComponent
private typealias ConceptReferenceComponent = ValueSet.ConceptReferenceComponent
private typealias ConceptSetFilterComponent = ValueSet.ConceptSetFilterComponent

@Entity
@Table(name = "fhir_value_set_compose_data", schema = "public")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING, name = "type")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
abstract class ValueSetContentData(
    id: String?,
    @ManyToOne val vs: ValueSetData?,
    @Column(name = "system") val system: String?,
    @Column(name = "version") val version: String?,
    concept: List<ConceptReferenceComponent>,
    filter: List<ConceptSetFilterComponent>,
    valueSet: List<CanonicalType>
) {

    @Column(name = "id", nullable = false) @Id @GeneratedValue
    val id: Int? = id?.toInt()
    @OneToMany(mappedBy = "vsContentData", cascade = [CascadeType.ALL], orphanRemoval = true)
    val concept: List<VSConceptData> = concept.map { it.toVSConceptData(this) }
    @Column(name = "filter", columnDefinition = "jsonb") @Type(type = "jsonb")
    val filter: String? = JsonUtil.serialize(filter)
    @Column(name = "valueSet")
    val valueSet: String? = JsonUtil.serialize(valueSet)

    // See https://vladmihalcea.com/the-best-way-to-map-a-onetomany-association-with-jpa-and-hibernate/

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ValueSetContentData) return false
        return id == other.id
    }

    override fun hashCode(): Int = this::class.hashCode()

}

fun ValueSetContentData.toConceptSetComponent(): ConceptSetComponent {
    val csComponent = LazyConceptSetComponent { concept.map { it.toVSConceptReferenceComponent() }.toMutableList() }

    csComponent.system = system
    csComponent.version = version
    csComponent.filter = JsonUtil.deserializeList(filter, "ValueSet.ConceptSetFilterComponent")
            as List<ValueSet.ConceptSetFilterComponent>
    csComponent.valueSet = JsonUtil.deserializeList(valueSet, "CanonicalType")
            as List<CanonicalType>

    return csComponent
}

@Entity
@DiscriminatorValue("Include")
class ValueSetIncludeData(
    conceptSet: ConceptSetComponent,
    vsData: ValueSetData
): ValueSetContentData(
    conceptSet.id,
    vsData,
    conceptSet.system,
    conceptSet.version,
    conceptSet.concept,
    conceptSet.filter,
    conceptSet.valueSet
)

fun ConceptSetComponent.toValueSetIncludeData(vs: ValueSetData): ValueSetIncludeData =
    ValueSetIncludeData(this, vs)

@Entity
@DiscriminatorValue("Exclude")
class ValueSetExcludeData(
    conceptSet: ConceptSetComponent,
    vsData: ValueSetData
): ValueSetContentData(
    conceptSet.id,
    vsData,
    conceptSet.system,
    conceptSet.version,
    conceptSet.concept,
    conceptSet.filter,
    conceptSet.valueSet
)

fun ConceptSetComponent.toValueSetExcludeData(vs: ValueSetData): ValueSetExcludeData =
    ValueSetExcludeData(this, vs)