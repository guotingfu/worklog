package com.example.worklog.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// [修改] version 升级为 2
@Database(entities = [Session::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "worklog_database")
                    // [新增] 开发阶段允许数据库重建（因为修改了 Session 结构）
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { Instance = it }
            }
        }
    }
}