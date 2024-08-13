package de.itcr.termite.util

import java.time.Instant
import java.util.*
import javax.xml.bind.DatatypeConverter


/***
 * Converts a ISO 8601 formatted date string to a java.util.Date object representing this timestamp. Since the HL7 FHIR
 * standard requires dates to be in this format, the function can be used to parse such date strings
 *
 * @param dateString: Input string with date encoded according to ISO 8601 standard
 * @return Instance of java.util.Date class representing timestamp expressed by date string
 */
fun parseDate(dateString: String): Date = DatatypeConverter.parseDate(dateString).time

fun now(): Date = Date.from(Instant.now())