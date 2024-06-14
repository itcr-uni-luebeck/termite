package de.itcr.termite.index.provider.r4b

import org.apache.logging.log4j.LogManager
import org.hl7.fhir.r4b.model.StructureDefinition
import org.rocksdb.Options
import org.rocksdb.RocksDB
import java.nio.file.Path
import kotlin.io.path.pathString

class RocksDBIndexStore(dbPath: Path) {

    companion object {

        private val logger = LogManager.getLogger(RocksDBIndexStore)

    }

    private val dbOptions = Options().setCreateIfMissing(true)
    private val database: RocksDB = RocksDB.open(dbOptions, dbPath.toAbsolutePath().pathString)

}

fun configureRocksDBIndexStore(dbPath: Path, structureDefinition: StructureDefinition) {
    val indexStore = RocksDBIndexStore(dbPath)
}