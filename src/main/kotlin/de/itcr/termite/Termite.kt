package de.itcr.termite

import ca.uhn.fhir.context.FhirContext
import de.itcr.termite.database.sql.TerminologyDatabase
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
open class Termite{

    @Bean
    open fun logger(): Logger = LogManager.getLogger(Termite::class.java)

    @Bean
    open fun fhirContext(): FhirContext = FhirContext.forR4()

    @Bean
    open fun database(): TerminologyDatabase = TerminologyDatabase("jdbc:sqlite::memory:")

}
fun main(args: Array<String>){
    runApplication<Termite>(*args)
}