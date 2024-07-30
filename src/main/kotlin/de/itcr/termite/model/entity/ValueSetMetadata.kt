package de.itcr.termite.model.entity

import de.itcr.termite.util.r4b.JsonUtil
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hl7.fhir.r4b.model.*
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "fhir_value_set_metadata", schema = "public")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
data class ValueSetMetadata(
    override val id: Int,
    override val versionId: Int,
    override val lastUpdated: Date?,
    override val source: String?,
    override val profile: List<String?>,
    override val security: String?,
    override val tag: String?,
    @Column(name = "url") val url: String,
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
    @Column(name = "immutable") val immutable: Boolean = false,
    @Column(name = "purpose") val purpose: String?,
    @Column(name = "copyright") val copyright: String?,
    @Column(name = "composeLockedDate") val composeLockedDate: Date?,
    @Column(name = "composeInactive") val composeInactive: Boolean?
): ResourceMetadata(id, versionId, lastUpdated, source, profile, security, tag)

fun ValueSet.toValueSetMetadata(): ValueSetMetadata {
    return ValueSetMetadata(
        if (meta.idBase != null) meta.idBase.toInt() else 0,
        if (meta.versionId != null) meta.versionId.toInt() else 0,
        null,
        meta.source,
        meta.profile.map { it.valueAsString },
        JsonUtil.serialize(meta.security),
        JsonUtil.serialize(meta.tag),
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
        immutable,
        purpose,
        copyright,
        if (this.compose.hasLockedDate()) this.compose.lockedDate else null,
        if (this.compose.hasInactive()) this.compose.inactive else null
    )
}

fun ValueSetMetadata.toValueSetResource(): ValueSet {
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

    return vs
}