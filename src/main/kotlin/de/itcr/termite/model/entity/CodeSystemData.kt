package de.itcr.termite.model.entity

import de.itcr.termite.model.lazy.LazyCodeSystem
import de.itcr.termite.util.r4b.JsonUtil
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.Where
import org.hl7.fhir.r4b.model.*
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "code_system_data", schema = "public")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
class CodeSystemData(
    cs: CodeSystem
): ResourceData(
    if (cs.meta.idBase != null) cs.meta.idBase.toInt() else null,
    if (cs.meta.versionId != null) cs.meta.versionId.toInt() else null,
    null,
    cs.meta.source,
    cs.meta.profile.map { it.valueAsString },
    JsonUtil.serialize(cs.meta.security),
    JsonUtil.serialize(cs.meta.tag)
) {

    @Column(name = "url") val url: String = cs.url
    @Column(name = "identifier", columnDefinition = "jsonb") @Type(type = "jsonb")
    val identifier: String? = JsonUtil.serialize(cs.identifier)
    @Column(name = "version") val version: String? = cs.version
    @Column(name = "name") val name: String? = cs.name
    @Column(name = "title") val title: String? = cs.title
    @Column(name = "status") val status: String? = cs.status?.toCode()
    @Column(name = "experimental") val experimental: Boolean = cs.experimental
    @Column(name = "date") val date: Date? = cs.date
    @Column(name = "publisher") val publisher: String? = cs.publisher
    @Column(name = "contact", columnDefinition = "jsonb") @Type(type = "jsonb")
    val contact: String? = JsonUtil.serialize(cs.contact)
    @Column(name = "description") val description: String? = cs.description
    @Column(name = "use_context", columnDefinition = "jsonb") @Type(type = "jsonb")
    val useContext: String? = JsonUtil.serialize(cs.useContext)
    @Column(name = "jurisdiction", columnDefinition = "jsonb") @Type(type = "jsonb")
    val jurisdiction: String? = JsonUtil.serialize(cs.jurisdiction)
    @Column(name = "purpose") val purpose: String? = cs.purpose
    @Column(name = "copyright") val copyright: String? = cs.copyright
    @Column(name = "case_sensitive") val caseSensitive: Boolean = cs.caseSensitive
    @Column(name = "value_set") val valueSet: String? = cs.valueSet
    @Column(name = "hierarchy_meaning") val hierarchyMeaning: String? = cs.hierarchyMeaning?.toCode()
    @Column(name = "compositional") val compositional: Boolean = cs.compositional
    @Column(name = "version_needed") val versionNeeded: Boolean = cs.versionNeeded
    @Column(name = "content") val content: String? = cs.content?.toCode()
    @Column(name = "supplements") val supplements: String? = cs.supplements
    // TODO: Autogen if not present?
    @Column(name = "count") val count: Int = cs.count
    @Column(name = "filter", columnDefinition = "jsonb") @Type(type = "jsonb")
    val filter: String? = JsonUtil.serialize(cs.filter)
    @Column(name = "property", columnDefinition = "jsonb") @Type(type = "jsonb")
    val property: String? = JsonUtil.serialize(cs.property)
    @OneToMany(mappedBy = "cs", cascade = [CascadeType.ALL], orphanRemoval = true)
    val concept: List<CSConceptData> = cs.concept.map { it.toCSConceptData(this) }

}

fun CodeSystem.toCodeSystemData(): CodeSystemData = CodeSystemData(this)

fun CodeSystemData.toCodeSystemResource(): CodeSystem {
    val cs = LazyCodeSystem { concept.map { it.toCSConceptDefinitionComponent() }.toMutableList() }

    cs.id = id.toString()
    cs.meta.versionId = versionId.toString()
    cs.meta.lastUpdated = lastUpdated
    cs.meta.source = sourceSystem
    cs.meta.profile = profile.map { CanonicalType(it) }
    cs.meta.security = JsonUtil.deserializeList(security, "Coding") as List<Coding>
    cs.meta.tag = JsonUtil.deserializeList(tag, "Coding") as List<Coding>
    cs.url = url
    cs.identifier = JsonUtil.deserializeList(identifier, "Identifier") as List<Identifier>
    cs.version = version
    cs.name = name
    cs.title = title
    cs.status = Enumerations.PublicationStatus.fromCode(status)
    cs.experimental = experimental
    cs.date = date
    cs.publisher = publisher
    cs.contact = JsonUtil.deserializeList(contact, "ContactDetail") as List<ContactDetail>
    cs.description = description
    cs.useContext = JsonUtil.deserializeList(useContext, "UsageContext") as List<UsageContext>
    cs.jurisdiction = JsonUtil.deserializeList(jurisdiction, "CodeableConcept") as List<CodeableConcept>
    cs.purpose = purpose
    cs.copyright = copyright
    cs.caseSensitive = caseSensitive
    cs.valueSet = valueSet
    cs.hierarchyMeaning = CodeSystem.CodeSystemHierarchyMeaning.fromCode(hierarchyMeaning)
    cs.compositional = compositional
    cs.versionNeeded = versionNeeded
    cs.content = CodeSystem.CodeSystemContentMode.fromCode(content)
    cs.supplements = supplements
    cs.count = count
    cs.filter = JsonUtil.deserializeList(filter, "CodeSystem.CodeSystemFilterComponent") as List<CodeSystem.CodeSystemFilterComponent>
    cs.property = JsonUtil.deserializeList(property, "CodeSystem.PropertyComponent") as List<CodeSystem.PropertyComponent>

    return cs
}