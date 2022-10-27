package de.itcr.termite.api.delegation

@Target(AnnotationTarget.FUNCTION)
@Retention
annotation class Delegate(val path: Array<String>, val params: Array<String>)
