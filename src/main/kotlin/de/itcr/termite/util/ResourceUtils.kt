package de.itcr.termite.util

import de.itcr.termite.api.FhirController
import org.apache.logging.log4j.LogManager
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import java.io.File
import java.io.IOException
import kotlin.reflect.KClass
import java.lang.ClassLoader

class ResourceUtils {

    companion object {

        private val logger = LogManager.getLogger(ResourceUtils::class.java)

        fun findClassesInPackage(packageName: String, classLoader: ClassLoader, jarPath: String = ""): Set<KClass<*>> {
            val pattern = "classpath:${packageName.replace("[.]".toRegex(), "/")}/*"
            return findClasses(pattern, classLoader, packageName)
        }

        private fun findClasses(pattern: String, classLoader: ClassLoader, packageName: String): Set<KClass<*>>
        {
            logger.debug("Loading classes @ $pattern")
            val resourceResolver = PathMatchingResourcePatternResolver(classLoader)
            try {
                val resources = resourceResolver.getResources(pattern)

                val classSet: MutableSet<KClass<*>> = mutableSetOf()
                resources.forEach { resource ->
                    val resourceName = resource.filename!!
                    // Load class if extension is .class
                    if (resource.filename!!.endsWith(".class")) {
                        getClass(resourceName.split("/").last(), packageName)?.let { clazz -> classSet.add(clazz) }
                    }
                    // Else find classes in subpackage
                    else {
                        val newPackageName = "$packageName.${resourceName.split("/").last()}"
                        classSet.addAll(findClasses(resourceName, classLoader, newPackageName))
                    }
                }
                return classSet.toSet()
            }
            catch (e: IOException) {
                logger.debug("Could not load classes from $packageName:\n${e.message}")
                return setOf()
            }
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