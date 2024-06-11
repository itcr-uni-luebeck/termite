package de.itcr.termite.model.entity

import java.time.LocalDateTime
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "fhir_value_set_metadata", schema = "public")
data class FhirValueSetMetadata(
    @Column(name = "id") @Id @GeneratedValue val id: Int,
    @Column(name = "version_id") @Version val versionId: Int,
    @Column(name = "last_updated") @Temporal(TemporalType.TIMESTAMP) val lastUpdate: Date?,
    @Column(name = "source") val source: String?,
    @Column(name = "profile") val profile: String?,
    @Column(name = "url") val url: String?,
    //TODO: Change this to JSONB column if possible
    @Column(name = "identifier") val identifier: String?,
    @Column(name = "version") val version: String?,
    @Column(name = "name") val name: String?,
    @Column(name = "title") val title: String?,
    @Column(name = "status") val status: String,
    @Column(name = "experimental") val experimental: Boolean?,
    @Column(name = "date") val date: Date?,
    @Column(name = "publisher") val publisher: String?,
    //TODO: Change this to JSONB column if possible
    @Column(name = "contact") val contact: String?,
    @Column(name = "description") val description: String?,
    //TODO: Change this to JSONB column if possible
    @Column(name = "use_context") val useContext: String?,
    //TODO: Change this to JSONB column if possible
    @Column(name = "jurisdiction") val jurisdiction: String?,
    @Column(name = "purpose") val purpose: String?,
    @Column(name = "copyright") val copyright: String?
)