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

typealias IssueSeverity = OperationOutcome.IssueSeverity
typealias IssueType = OperationOutcome.IssueType

private val logger = LogManager.getLogger("de.itcr.termite.api.r4b.exc.Handlers")

fun handleUnsupportedFormat(e: UnsupportedFormatException, parser: IParser) =
    handleException(e, parser, HttpStatus.NOT_ACCEPTABLE, IssueSeverity.ERROR, IssueType.VALUE)

fun handleUnparsableEntity(e: Exception, parser: IParser) =
    handleException(e, parser, HttpStatus.UNSUPPORTED_MEDIA_TYPE, IssueSeverity.ERROR, IssueType.VALUE, "Unparsable entity: {e}")

fun handleUnsupportedParameterValue(exc: UnsupportedValueException, parser: IParser) =
    handleException(exc, parser, HttpStatus.BAD_REQUEST, IssueSeverity.ERROR, IssueType.VALUE)

fun handleException(e: Throwable, parser: IParser, httpStatus: HttpStatus, severity: IssueSeverity, type: IssueType, template: String = "{e}"): ResponseEntity<String> {
    val message = replaceInTemplate(template, e.message ?: "")
    logger.debug(message)
    val opOutcome = generateOperationOutcomeString(severity, type, message, parser)
    return ResponseEntity.status(httpStatus)
        .eTag("W/\"0\"")
        .header("Content-Type", determineContentType(parser))
        .body(opOutcome)
}

private fun replaceInTemplate(template: String, message: String): String = template.replace("{e}", message)

private fun determineContentType(parser: IParser): String {
    return when(parser.encoding) {
        EncodingEnum.JSON -> "application/fhir+json"
        EncodingEnum.NDJSON -> "application/fhir+ndjson"
        EncodingEnum.XML -> "application/fhir+xml"
        EncodingEnum.RDF -> "application/fhir+turtle"
        null -> "text/plain"
    }
}