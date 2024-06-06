package de.itcr.termite.model.entity

import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "CodeSystem")
data class FhirCodeSystemMetaData(
    @Id @GeneratedValue val id: String,
    @Column(name = "version_id") val versionId: String?,
    @Column(name = "last_updated") val lastUpdate: LocalDateTime?,
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
    @Column(name = "date") val date: LocalDateTime?,
    @Column(name = "publisher") val publisher: String?,
    //TODO: Change this to JSONB column if possible
    @Column(name = "contact") val contact: String?,
    @Column(name = "description") val description: String?,
    //TODO: Change this to JSONB column if possible
    @Column(name = "use_context") val useContext: String?,
    //TODO: Change this to JSONB column if possible
    @Column(name = "jurisdiction") val jurisdiction: String?,
    @Column(name = "purpose") val purpose: String?,
    @Column(name = "copyright") val copyright: String?,
    @Column(name = "copyright_label") val copyrightLabel: String?,
    @Column(name = "approval_date") val approvalDate: LocalDate?,
    @Column(name = "last_review_date") val lastReviewDate: LocalDate?,
    //TODO: Change this to JSONB column if possible
    @Column(name = "effective_period") val effectivePeriod: String?,
    //TODO: Change this to JSONB column if possible
    @Column(name = "topic") val topic: String?,
    //TODO: Change this to JSONB column if possible
    @Column(name = "author") val author: String?,
    //TODO: Change this to JSONB column if possible
    @Column(name = "editor") val editor: String?,
    //TODO: Change this to JSONB column if possible
    @Column(name = "reviewer") val reviewer: String?,
    //TODO: Change this to JSONB column if possible
    @Column(name = "endorser") val endorser: String?,
    //TODO: Change this to JSONB column if possible
    @Column(name = "related_artifact") val relatedArtifact: String?,
    @Column(name = "case_sensitive") val caseSensitive: Boolean?,
    @Column(name = "value_set") val valueSet: String?,
    @Column(name = "hierarchy_meaning") val hierarchyMeaning: String?,
    @Column(name = "compositional") val compositional: Boolean?,
    @Column(name = "version_needed") val versionNeeded: Boolean?,
    @Column(name = "content") val content: String,
    @Column(name = "supplements") val supplements: String?,
    @Column(name = "count") val count: Int?,
    //TODO: Change this to JSONB column if possible
    @Column(name = "filter") val filter: String?,
    //TODO: Change this to JSONB column if possible
    @Column(name = "property") val property: String
)