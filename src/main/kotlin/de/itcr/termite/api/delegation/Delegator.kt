package de.itcr.termite.api.delegation

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

class Delegator<T, R> constructor(private val clazzT: Class<T>, private val clazzR: Class<R>){

    companion object{
        private val lookup: MethodHandles.Lookup = MethodHandles.lookup()
    }

    private val delegationMap: Map<Array<String>, Map<Set<String>, MethodHandle>>

    init {
        //Get methods annotated by @Delegated annotation
        val delegatedMethods = clazzT.methods.filter { m -> m.isAnnotationPresent(Delegate::class.java) }

        //Check if annotated methods have right signature
        delegatedMethods.forEach { m ->
            val ann = m.getAnnotation(Delegate::class.java)
            //Unfortunately, we can only check if the method parameter is of type map due to type erasure
            if(m.returnType != clazzR){
                throw DelegationException("Return type of method ${m.name} in class ${clazzT.name} is ${m.returnType.name} but should be ${clazzR.name}")
            }
            else if(m.parameterCount != ann.params.size) {
                throw DelegationException("Method ${m.name} in class ${clazzT.name} has ${m.parameterCount} parameters but has to have exactly ${ann.params.size}")
            }
            m.parameterTypes.forEachIndexed { idx, pType ->
                if(pType != String::class.java) {
                    throw DelegationException("Parameter $idx of method ${m.name} in class ${clazzT.name} is ${pType.name} but should be ${String::class.java.name}")
                }
            }
        }

        //Assign methods to path and parameter combinations
        val mType = MethodType.methodType(clazzR, Map::class.java)
        val map = mutableMapOf<Array<String>, MutableMap<Set<String>, MethodHandle>>()
        delegatedMethods.forEach { m ->
            val mHandle = lookup.findVirtual(clazzT, m.name, mType)
            val delegate = m.getAnnotation(Delegate::class.java)
            val path = delegate.path
            val params = delegate.params.toSet()
            if(map.contains(path) && map[path] != null){
                val paramMap = map[path]
                if (paramMap != null) {
                    if(paramMap.contains(params)){
                        throw DelegationException("Parameters $params for path $path already in use")
                    } else{
                        paramMap[params] = mHandle
                    }
                }
            }
            else{
                map[path] = mutableMapOf(params to mHandle)
            }
        }
        delegationMap = map
    }

    fun delegate(path: Array<String>, params: Map<String, String>): R {
        //NOTE: Due to prior operations maps entries should never be null
        return delegationMap[path]!![params.keys]!!.invokeExact(params) as R
    }

}

inline fun <reified T, reified R> Delegator(): Delegator<T, R>{
    return Delegator(T::class.java, R::class.java)
}