package de.itcr.termite.model.entity

import de.itcr.termite.util.r4b.JsonUtil
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hl7.fhir.r4b.model.CanonicalType
import org.hl7.fhir.r4b.model.ConceptMap
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "concept_map_data", schema = "public")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
class ConceptMapData(
    cm: ConceptMap
): ResourceData(
    if (cm.meta.idBase != null) cm.meta.idBase.toInt() else null,
    if (cm.meta.versionId != null) cm.meta.versionId.toInt() else null,
    null,
    cm.meta.source,
    cm.meta.profile.map { it.valueAsString },
    JsonUtil.serialize(cm.meta.security),
    JsonUtil.serialize(cm.meta.tag)
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
    @Column(name = "purpose") val purpose: String?
    @Column(name = "copyright") val copyright: String?
    @Column(name = "source") val source: String?
    @Column(name = "target") val target: String?
    
    init {
        url = cm.url
        identifier = JsonUtil.serialize(cm.identifier)
        version = cm.version
        name = cm.name
        title = cm.title
        status = cm.status.toCode()
        experimental = cm.experimental
        date = cm.date
        publisher = cm.publisher
        contact = JsonUtil.serialize(cm.contact)
        description = cm.description
        useContext = JsonUtil.serialize(cm.useContext)
        jurisdiction = JsonUtil.serialize(cm.jurisdiction)
        purpose = cm.purpose
        copyright = cm.copyright
        // TODO: Switch to full JSONB serialization in the future to avoid such solutions
        source = "${if (cm.source is CanonicalType) "canonical" else "uri"}#${cm.source}"
        target = "${if (cm.target is CanonicalType) "canonical" else "uri"}#${cm.target}"
    }

}