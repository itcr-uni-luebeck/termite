package de.itcr.termite.model.entity

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.UpdateTimestamp
import java.util.*
import javax.persistence.*

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
abstract class FhirResourceMetadata(
    @Column(name = "id", nullable = false) @Id @GeneratedValue open val id: Int,
    @Column(name = "version_id", nullable = false) @Version open val versionId: Int,
    @Column(name = "last_updated") @Temporal(TemporalType.TIMESTAMP) @UpdateTimestamp open val lastUpdated: Date?,
    @Column(name = "source") open val source: String?,
    @Column(name = "profile") @ElementCollection open val profile: List<String?>,
    @Column(name = "security", columnDefinition = "jsonb") @Type(type = "jsonb") open val security: String?,
    @Column(name = "tag", columnDefinition = "jsonb") @Type(type = "jsonb") open val tag: String?
)