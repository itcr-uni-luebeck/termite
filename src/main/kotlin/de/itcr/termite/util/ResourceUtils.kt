package de.itcr.termite.util

import org.apache.logging.log4j.LogManager

class ResourceUtils {

    companion object {

        private val logger = LogManager.getLogger(ResourceUtils::class.java)

        fun findClassesInPackage(packageName: String, classLoader: ClassLoader): Set<Class<*>>
        {
            logger.debug("Loading classes in package $packageName")
            val stream = classLoader.getResourceAsStream(packageName.replace("[.]".toRegex(), "/"))
            if (stream == null) {
                logger.debug("Could not load classes from $packageName: Package not found or access denied")
                return setOf()
            }

            val classSet: MutableSet<Class<*>> = mutableSetOf()
            stream.bufferedReader().forEachLine { line ->
                // Load class if extension is .class
                if (line.endsWith(".class")) getClass(line, packageName)?.let { clazz -> classSet.add(clazz) }
                // Else find classes in subpackage
                else classSet.addAll(findClassesInPackage("$packageName.$line", classLoader))
            }
            return classSet.toSet()
        }

        private fun getClass(className: String, packageName: String): Class<*>?
        {
            val fullClassName = "$packageName.${className.substring(0, className.lastIndexOf('.'))}"
            return try {
                Class.forName(fullClassName)
            } catch (exception: ClassNotFoundException) {
                logger.debug("Could not load class $fullClassName", exception)
                null
            }
        }

    }

}