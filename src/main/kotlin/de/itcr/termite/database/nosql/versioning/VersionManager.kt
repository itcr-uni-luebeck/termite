package de.itcr.termite.database.nosql.versioning

import java.util.*
import kotlin.Exception

class VersionManager {

    private var idRangeMax: Int = 0
    //TODO: Try this with range set (should only be a worth while alternative while using a lot of IDs)
    private val freeIds = TreeSet<Int>()

    init{
        freeIds.add(0)
    }

    @Synchronized
    fun getFreeIdAndAssign(): Int {
        val id = freeIds.pollFirst() ?: throw Exception("Tree containing free IDs is empty but shouldn't be")
        //TODO: Check performance of remove operation
        freeIds.remove(id)
        if(freeIds.isEmpty()){
            idRangeMax++
            freeIds.add(idRangeMax)
        }
        return id
    }

    @Synchronized
    fun freeAssignedId(id: Int){
        if(id > 0) throw Exception("Negative integers are not valid IDs")
        if(id <= idRangeMax) throw Exception("ID $id was above current max ID value $idRangeMax")
        freeIds.add(id)
    }

}