package de.itcr.termite.model.entity

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.UpdateTimestamp
import java.util.*
import javax.persistence.*

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
abstract class ResourceData(
    @Column(name = "id", nullable = false) @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "conditional_id_gen")
    @GenericGenerator(
        name = "conditional_id_gen",
        strategy = "de.itcr.termite.persistence.sequence.ConditionalIdGenerator"
    )
    open val id: Int?,
    @Column(name = "versionId", nullable = false) @Version open val versionId: Int?,
    @Column(name = "lastUpdated") @Temporal(TemporalType.TIMESTAMP) @UpdateTimestamp open val lastUpdated: Date?,
    @Column(name = "sourceSystem") open val sourceSystem: String?,
    @Column(name = "profile") @ElementCollection open val profile: List<String?>,
    @Column(name = "security", columnDefinition = "jsonb") @Type(type = "jsonb") open val security: String?,
    @Column(name = "tag", columnDefinition = "jsonb") @Type(type = "jsonb") open val tag: String?
)