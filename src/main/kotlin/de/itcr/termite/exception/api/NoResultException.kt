package de.itcr.termite.exception.api

class NoResultException(message: String, e: Throwable?): ApiException(message, e)

fun NoResultException(message: String) = NoResultException(message, null)