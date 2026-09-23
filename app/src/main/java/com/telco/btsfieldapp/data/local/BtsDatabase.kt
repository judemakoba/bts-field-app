package com.telco.btsfieldapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.telco.btsfieldapp.data.local.dao.AuditDao
import com.telco.btsfieldapp.data.local.dao.PendingSyncDao
import com.telco.btsfieldapp.data.local.dao.SiteDao
import com.telco.btsfieldapp.data.local.entity.AuditEntity
import com.telco.btsfieldapp.data.local.entity.PendingSyncEntity
import com.telco.btsfieldapp.data.local.entity.SiteEntity

@Database(
    entities = [
        SiteEntity::class,
        AuditEntity::class,
        PendingSyncEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class BtsDatabase : RoomDatabase() {
    abstract fun siteDao(): SiteDao
    abstract fun auditDao(): AuditDao
    abstract fun pendingSyncDao(): PendingSyncDao
}
