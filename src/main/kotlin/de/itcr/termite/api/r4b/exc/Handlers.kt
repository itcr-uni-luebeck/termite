package de.itcr.termite.api.r4b.exc

import de.itcr.termite.api.r4b.FhirController
import de.itcr.termite.exception.NotFoundException
import de.itcr.termite.exception.api.UnsupportedFormatException
import de.itcr.termite.exception.api.UnsupportedParameterException
import de.itcr.termite.exception.api.UnsupportedValueException
import de.itcr.termite.exception.fhir.r4b.UnexpectedResourceTypeException
import de.itcr.termite.exception.persistence.PersistenceException
import org.apache.logging.log4j.Level
import org.hl7.fhir.r4b.model.OperationOutcome
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

typealias IssueSeverity = OperationOutcome.IssueSeverity
typealias IssueType = OperationOutcome.IssueType

fun FhirController.handleUnsupportedFormat(exc: UnsupportedFormatException, accept: String?) =
    this.handleException(exc, accept, HttpStatus.NOT_ACCEPTABLE, IssueSeverity.ERROR, IssueType.VALUE)

fun FhirController.handleUnparsableEntity(exc: Exception, accept: String?) =
    this.handleException(exc, accept, HttpStatus.UNSUPPORTED_MEDIA_TYPE, IssueSeverity.ERROR, IssueType.VALUE,
        template = "Unparsable entity: {e}")

fun FhirController.handleUnsupportedParameterValue(exc: UnsupportedValueException, accept: String?) =
    this.handleException(exc, accept, HttpStatus.BAD_REQUEST, IssueSeverity.ERROR, IssueType.VALUE)

fun FhirController.handleUnsupportedParameter(exc: UnsupportedParameterException, accept: String?) =
    this.handleException(exc, accept, HttpStatus.BAD_REQUEST, IssueSeverity.ERROR, IssueType.NOTSUPPORTED)

fun FhirController.handleUnexpectedResourceType(exc: UnexpectedResourceTypeException, accept: String?) =
    this.handleException(exc, accept, HttpStatus.BAD_REQUEST, IssueSeverity.ERROR, IssueType.STRUCTURE)

fun FhirController.handleNotFound(exc: NotFoundException, accept: String?) =
    handleException(exc, accept, HttpStatus.NOT_FOUND, IssueSeverity.INFORMATION, IssueType.NOTFOUND)

fun FhirController.handlePersistenceException(exc: PersistenceException, accept: String?) =
    this.handleException(exc, accept, HttpStatus.INTERNAL_SERVER_ERROR, IssueSeverity.ERROR, IssueType.PROCESSING,
        logLevel = Level.ERROR, logStackTrace = true,
        template = "Operation failed during database access. Reason: {e}")

fun FhirController.handleUnexpectedError(e: Throwable, accept: String?) =
    this.handleException(e, accept, HttpStatus.INTERNAL_SERVER_ERROR, IssueSeverity.ERROR, IssueType.PROCESSING,
        logLevel = Level.ERROR, logStackTrace = true,
        template = "Unexpected error occurred: {e}", )

fun FhirController.handleException(
    e: Throwable, accept: String?,
    httpStatus: HttpStatus,
    severity: IssueSeverity,
    type: IssueType,
    logLevel: Level = Level.DEBUG,
    logStackTrace: Boolean = false,
    template: String = "{e}"
): ResponseEntity<String> {
    val message = replaceInTemplate(template, e.message ?: e::class.simpleName ?: "Error without message")
    logger.log(logLevel, message)
    if (logStackTrace) logger.debug(e.stackTraceToString())
    val opOutcome = generateOperationOutcomeString(severity, type, message, accept ?: "application/fhir+json")
    return ResponseEntity.status(httpStatus)
        .eTag("W/\"0\"")
        .header("Content-Type", accept)
        .body(opOutcome)
}

private fun replaceInTemplate(template: String, message: String): String = template.replace("{e}", message)