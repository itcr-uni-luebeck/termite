package de.itcr.termite.util

import org.apache.logging.log4j.LogManager
import org.hl7.fhir.r4b.model.CanonicalResource
import org.hl7.fhir.r4b.model.Coding
import kotlin.reflect.KClass

class ResourceUtil {

    companion object {

        private val logger = LogManager.getLogger(ResourceUtil::class.java)

        fun findClassesInPackage(packageName: String, classLoader: ClassLoader): Set<KClass<*>>
        {
            logger.debug("Loading classes in package $packageName")
            val stream = classLoader.getResourceAsStream(packageName.replace("[.]".toRegex(), "/"))
            if (stream == null) {
                logger.debug("Could not load classes from $packageName: Package not found or access denied")
                return setOf()
            }

            val classSet: MutableSet<KClass<*>> = mutableSetOf()
            stream.bufferedReader().forEachLine { line ->
                // Load class if extension is .class
                if (line.endsWith(".class")) getClass(line, packageName)?.let { clazz -> classSet.add(clazz) }
                // Else find classes in subpackage
                else classSet.addAll(findClassesInPackage("$packageName.$line", classLoader))
            }
            return classSet.toSet()
        }

        private fun getClass(className: String, packageName: String): KClass<*>?
        {
            val fullClassName = "$packageName.${className.substring(0, className.lastIndexOf('.'))}"
            return try {
                Class.forName(fullClassName).kotlin
            } catch (exception: ClassNotFoundException) {
                logger.debug("Could not load class $fullClassName", exception)
                null
            }
        }

    }

}

inline fun <reified T: CanonicalResource> T.tagAsSummarized(): T {
    meta.addTag(
        Coding(
        "http://terminology.hl7.org/CodeSystem/v3-ObservationValue",
        "SUBSETTED",
        "subsetted"
        )
    )
    return this
}