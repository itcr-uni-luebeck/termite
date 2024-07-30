package de.itcr.termite.model.entity

import de.itcr.termite.util.r4b.JsonUtil
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hl7.fhir.r4b.model.CanonicalType
import org.hl7.fhir.r4b.model.ValueSet
import org.yaml.snakeyaml.constructor.SafeConstructor.ConstructYamlSet
import javax.persistence.*

typealias VSContentData = ValueSetContentData
typealias VSIncludeData = ValueSetIncludeData
typealias VSExcludeData = ValueSetExcludeData

private typealias ConceptSetComponent = ValueSet.ConceptSetComponent

@Entity
@Table(name = "fhir_value_set_compose_data", schema = "public")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(discriminatorType = DiscriminatorType.CHAR, name = "type")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
abstract class ValueSetContentData(
    @Column(name = "id", nullable = false) @Id @GeneratedValue open val id: Int,
    @ManyToOne(targetEntity = ValueSetMetadata::class) open val vs: ValueSetMetadata,
    @Column(name = "system") open val system: String?,
    @Column(name = "version") open val version: String?,
    @Column(name = "filter", columnDefinition = "jsonb") @Type(type = "jsonb") open val filter: String?,
    @Column(name = "valueSet") open val valueSet: String?
)

fun ValueSetContentData.toConceptSetComponent(): ConceptSetComponent {
    val csComponent = ConceptSetComponent()

    csComponent.system = system
    csComponent.version = version
    csComponent.filter = JsonUtil.deserializeList(filter, "ValueSet.ConceptSetFilterComponent")
            as List<ValueSet.ConceptSetFilterComponent>
    csComponent.valueSet = JsonUtil.deserializeList(valueSet, "CanonicalType")
            as List<CanonicalType>

    return csComponent
}

@Entity
@DiscriminatorValue("I")
data class ValueSetIncludeData(
    override val id: Int,
    override val vs: ValueSetMetadata,
    override val system: String?,
    override val version: String?,
    override val filter: String?,
    override val valueSet: String?
): ValueSetContentData(id, vs, system, version, filter, valueSet)

fun ConceptSetComponent.toValueSetIncludeData(vs: ValueSetMetadata): ValueSetIncludeData {
    return ValueSetIncludeData(
        0, // Default value as it will be generated anyway,
        vs,
        system,
        version,
        JsonUtil.serialize(filter),
        JsonUtil.serialize(valueSet)
    )
}

@Entity
@DiscriminatorValue("E")
data class ValueSetExcludeData(
    override val id: Int,
    override val vs: ValueSetMetadata,
    override val system: String?,
    override val version: String?,
    override val filter: String?,
    override val valueSet: String?
): ValueSetContentData(id, vs, system, version, filter, valueSet)

fun ConceptSetComponent.toValueSetExcludeData(vs: ValueSetMetadata): ValueSetExcludeData {
    return ValueSetExcludeData(
        0, // Default value as it will be generated anyway,
        vs,
        system,
        version,
        JsonUtil.serialize(filter),
        JsonUtil.serialize(valueSet)
    )
}