package de.itcr.termite.model.entity

import de.itcr.termite.util.r4b.JsonUtil
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hl7.fhir.r4b.model.*
import java.util.*
import javax.persistence.*
import org.hibernate.annotations.UpdateTimestamp

@Entity
@Table(name = "fhir_code_system_metadata", schema = "public")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
data class FhirCodeSystemMetadata(
    @Column(name = "id") @Id @GeneratedValue val id: Long,
    @Column(name = "version_id") @Version val versionId: Long,
    @Column(name = "last_updated") @Temporal(TemporalType.TIMESTAMP) @UpdateTimestamp val lastUpdated: Date?,
    @Column(name = "source") val source: String?,
    @Column(name = "profile") @ElementCollection val profile: List<String?>,
    @Column(name = "url") val url: String?,
    @Column(name = "identifier", columnDefinition = "jsonb") @Type(type = "jsonb") val identifier: String?,
    @Column(name = "version") val version: String?,
    @Column(name = "name") val name: String?,
    @Column(name = "title") val title: String?,
    @Column(name = "status") val status: String,
    @Column(name = "experimental") val experimental: Boolean,
    @Column(name = "date") val date: Date?,
    @Column(name = "publisher") val publisher: String?,
    @Column(name = "contact", columnDefinition = "jsonb") @Type(type = "jsonb") val contact: String?,
    @Column(name = "description") val description: String?,
    @Column(name = "use_context", columnDefinition = "jsonb") @Type(type = "jsonb") val useContext: String?,
    @Column(name = "jurisdiction", columnDefinition = "jsonb") @Type(type = "jsonb") val jurisdiction: String?,
    @Column(name = "purpose") val purpose: String?,
    @Column(name = "copyright") val copyright: String?,
    @Column(name = "case_sensitive") val caseSensitive: Boolean,
    @Column(name = "value_set") val valueSet: String?,
    @Column(name = "hierarchy_meaning") val hierarchyMeaning: String?,
    @Column(name = "compositional") val compositional: Boolean,
    @Column(name = "version_needed") val versionNeeded: Boolean,
    @Column(name = "content") val content: String,
    @Column(name = "supplements") val supplements: String?,
    // TODO: Autogen if not present?
    @Column(name = "count") val count: Int,
    @Column(name = "filter", columnDefinition = "jsonb") @Type(type = "jsonb") val filter: String?,
    @Column(name = "property", columnDefinition = "jsonb") @Type(type = "jsonb") val property: String?
)

fun CodeSystem.toFhirCodeSystemMetadata(): FhirCodeSystemMetadata {
    return FhirCodeSystemMetadata(
        if (meta.idBase != null) meta.idBase.toLong() else 0L,
        if (meta.versionId != null) meta.versionId.toLong() else 0L,
        null,
        meta.source,
        meta.profile.map { it.valueAsString },
        url,
        JsonUtil.serialize(identifier),
        version,
        name,
        title,
        status.toCode(),
        experimental,
        date,
        publisher,
        JsonUtil.serialize(contact),
        description,
        JsonUtil.serialize(useContext),
        JsonUtil.serialize(jurisdiction),
        purpose,
        copyright,
        caseSensitive,
        valueSet,
        hierarchyMeaning?.toCode(),
        compositional,
        versionNeeded,
        content.toCode(),
        supplements,
        count,
        JsonUtil.serialize(filter),
        JsonUtil.serialize(property)
    )
}

fun FhirCodeSystemMetadata.toCodeSystemResource(): CodeSystem {
    val cs = CodeSystem()

    cs.meta.id = id.toString()
    cs.meta.versionId = versionId.toString()
    cs.meta.lastUpdated = lastUpdated
    cs.meta.source = source
    cs.meta.profile = profile.map { CanonicalType(it) }
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