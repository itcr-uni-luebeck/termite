package de.itcr.termite.persistence.sequence

import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.id.enhanced.SequenceStyleGenerator
import java.io.Serializable
import javax.persistence.SequenceGenerator

@SequenceGenerator(name = "ConditionalIdGenerator", allocationSize = 1)
class ConditionalIdGenerator(): SequenceStyleGenerator() {

    override fun generate(session: SharedSessionContractImplementor, obj: Any): Serializable {
        val persister = session.getEntityPersister(null, obj)
        return persister.getIdentifier(obj, session) ?: super.generate(session, obj)
    }

}