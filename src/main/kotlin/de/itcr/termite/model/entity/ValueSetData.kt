package de.itcr.termite.model.entity

import de.itcr.termite.util.r4b.JsonUtil
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.Where
import org.hl7.fhir.r4b.model.*
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "value_set_data", schema = "public")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
class ValueSetData(
    vs: ValueSet
): ResourceData(
    if (vs.meta.idBase != null) vs.meta.idBase.toInt() else null,
    if (vs.meta.versionId != null) vs.meta.versionId.toInt() else null,
    null,
    vs.meta.source,
    vs.meta.profile.map { it.valueAsString },
    JsonUtil.serialize(vs.meta.security),
    JsonUtil.serialize(vs.meta.tag)
) {

    @Column(name = "url") val url: String
    @Column(name = "identifier", columnDefinition = "jsonb") @Type(type = "jsonb") val identifier: String?
    @Column(name = "version") val version: String?
    @Column(name = "name") val name: String?
    @Column(name = "title") val title: String?
    @Column(name = "status") val status: String
    @Column(name = "experimental") val experimental: Boolean
    @Column(name = "date") val date: Date?
    @Column(name = "publisher") val publisher: String?
    @Column(name = "contact", columnDefinition = "jsonb") @Type(type = "jsonb") val contact: String?
    @Column(name = "description") val description: String?
    @Column(name = "use_context", columnDefinition = "jsonb") @Type(type = "jsonb") val useContext: String?
    @Column(name = "jurisdiction", columnDefinition = "jsonb") @Type(type = "jsonb") val jurisdiction: String?
    @Column(name = "immutable") val immutable: Boolean
    @Column(name = "purpose") val purpose: String?
    @Column(name = "copyright") val copyright: String?
    @Column(name = "composeLockedDate") val composeLockedDate: Date?
    @Column(name = "composeInactive") val composeInactive: Boolean?
    @OneToMany(mappedBy = "vs", cascade = [CascadeType.ALL], orphanRemoval = true) @Where(clause = "type='Include'")
    val composeInclude: List<VSIncludeData>
    @OneToMany(mappedBy = "vs", cascade = [CascadeType.ALL], orphanRemoval = true) @Where(clause = "type='Exclude'")
    val composeExclude: List<VSExcludeData>

    init {
        url = vs.url
        identifier = JsonUtil.serialize(vs.identifier)
        version = vs.version
        name = vs.name
        title = vs.title
        status = vs.status.toCode()
        experimental = vs.experimental
        date = vs.date
        publisher = vs.publisher
        contact =JsonUtil.serialize(vs.contact)
        description = vs.description
        useContext = JsonUtil.serialize(vs.useContext)
        jurisdiction = JsonUtil.serialize(vs.jurisdiction)
        immutable = vs.immutable
        purpose = vs.purpose
        copyright = vs.copyright
        composeLockedDate = if (vs.compose.hasLockedDate()) vs.compose.lockedDate else null
        composeInactive = if (vs.compose.hasInactive()) vs.compose.inactive else null
        composeInclude = vs.compose.include.map { it.toValueSetIncludeData(this) }
        composeExclude = vs.compose.exclude.map { it.toValueSetExcludeData(this) }
    }

}

fun ValueSet.toValueSetData(): ValueSetData = ValueSetData(this)

fun ValueSetData.toValueSetResource(): ValueSet {
    val vs = ValueSet()

    vs.id = id.toString()
    vs.meta.versionId = versionId.toString()
    vs.meta.lastUpdated = lastUpdated
    vs.meta.source = source
    vs.meta.profile = profile.map { CanonicalType(it) }
    vs.meta.security = JsonUtil.deserializeList(security, "Coding") as List<Coding>
    vs.meta.tag = JsonUtil.deserializeList(tag, "Coding") as List<Coding>
    vs.url = url
    vs.identifier = JsonUtil.deserializeList(identifier, "Identifier") as List<Identifier>
    vs.version = version
    vs.name = name
    vs.title = title
    vs.status = Enumerations.PublicationStatus.fromCode(status)
    vs.experimental = experimental
    vs.date = date
    vs.publisher = publisher
    vs.contact = JsonUtil.deserializeList(contact, "ContactDetail") as List<ContactDetail>
    vs.description = description
    vs.useContext = JsonUtil.deserializeList(useContext, "UsageContext") as List<UsageContext>
    vs.jurisdiction = JsonUtil.deserializeList(jurisdiction, "CodeableConcept") as List<CodeableConcept>
    vs.immutable = immutable
    vs.purpose = purpose
    vs.copyright = copyright
    vs.compose.lockedDate = composeLockedDate
    if (composeInactive != null) vs.compose.inactive = composeInactive
    vs.compose.include = composeInclude.map { it.toConceptSetComponent() }
    vs.compose.exclude = composeExclude.map { it.toConceptSetComponent() }

    return vs
}