package de.itcr.termite.exception

import java.lang.Exception

class AmbiguousValueSetVersionException(message: String, private val availableVersions: List<String>): Exception(message){

    fun getAvailableVersions(): List<String> = availableVersions

}