package de.itcr.termite.exception

class CodeSystemNotFoundException constructor(url: String): Exception("CodeSystem instance [url = $url] was not found")