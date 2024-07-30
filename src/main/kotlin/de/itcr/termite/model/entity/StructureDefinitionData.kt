package de.itcr.termite.model.entity

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.UpdateTimestamp
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "structure_definition_data", schema = "public")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
data class StructureDefinitionData(
    @Column(name = "id", nullable = false) @Id @GeneratedValue val id: Int,
    @Column(name = "version_id", nullable = false) @Version var versionId: Int,
    @Column(name = "last_updated") @Temporal(TemporalType.TIMESTAMP) @UpdateTimestamp val lastUpdated: Date?,
    @Column(name = "source") val source: String?,
    @Column(name = "profile") @ElementCollection val profile: List<String?>,
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
    @Column(name = "purpose") val purpose: String?,
    @Column(name = "copyright") val copyright: String?,
    @Column(name = "keyword", columnDefinition = "jsonb") @Type(type = "jsonb") val keyword: String?,
    @Column(name = "fhirVersion") val fhirVersion: String?,
    @Column(name = "mapping", columnDefinition = "jsonb") @Type(type = "jsonb") val mapping: String?,
    @Column(name = "context", columnDefinition = "jsonb") @Type(type = "jsonb") val context: String?,
    @Column(name = "snapshot", columnDefinition = "jsonb") @Type(type = "jsonb") val snapshot: String?,
    @Column(name = "differential", columnDefinition = "jsonb") @Type(type = "jsonb") val differential: String?
)