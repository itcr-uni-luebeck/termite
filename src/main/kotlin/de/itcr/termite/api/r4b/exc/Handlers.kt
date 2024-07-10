package de.itcr.termite.api.r4b.exc

import ca.uhn.fhir.parser.IParser
import ca.uhn.fhir.parser.JsonParser
import ca.uhn.fhir.rest.api.EncodingEnum
import de.itcr.termite.api.r4b.BaseController
import de.itcr.termite.api.r4b.BaseController.Companion
import de.itcr.termite.exception.api.UnsupportedFormatException
import de.itcr.termite.exception.api.UnsupportedValueException
import de.itcr.termite.util.generateOperationOutcomeString
import org.apache.logging.log4j.LogManager
import org.hl7.fhir.r4b.model.OperationOutcome
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

private val logger = LogManager.getLogger("de.itcr.termite.api.r4b.exc.Handlers")

fun handleUnsupportedFormat(exc: UnsupportedFormatException, parser: IParser): ResponseEntity<String> {
    logger.debug(exc.message)
    val opOutcome = generateOperationOutcomeString(
        OperationOutcome.IssueSeverity.ERROR,
        OperationOutcome.IssueType.VALUE,
        exc.message,
        parser
    )
    return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
        .eTag("W/\"0\"")
        .header("Content-Type", determineContentType(parser))
        .body(opOutcome)
}

fun handleUnparsableEntity(exc: Exception, parser: IParser): ResponseEntity<String> {
    logger.debug(exc.message)
    val opOutcome = generateOperationOutcomeString(
        OperationOutcome.IssueSeverity.ERROR,
        OperationOutcome.IssueType.VALUE,
        "Unparsable entity: " + exc.message,
        parser
    )
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .eTag("W/\"0\"")
        .header("Content-Type", determineContentType(parser))
        .body(opOutcome)
}

fun handleUnsupportedParameterValue(exc: UnsupportedValueException, parser: IParser): ResponseEntity<String> {
    logger.debug(exc.message)
    val opOutcome = generateOperationOutcomeString(
        OperationOutcome.IssueSeverity.ERROR,
        OperationOutcome.IssueType.VALUE,
        exc.message,
        parser
    )
    return ResponseEntity.badRequest()
        .eTag("W/\"0\"")
        .header("Content-Type", determineContentType(parser))
        .body(opOutcome)
}

fun determineContentType(parser: IParser): String {
    return when(parser.encoding) {
        EncodingEnum.JSON -> "application/fhir+json"
        EncodingEnum.NDJSON -> "application/fhir+ndjson"
        EncodingEnum.XML -> "application/fhir+xml"
        EncodingEnum.RDF -> "application/fhir+turtle"
        null -> "text/plain"
    }
}