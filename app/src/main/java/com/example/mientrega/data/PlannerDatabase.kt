package com.example.mientrega.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "subjects", indices = [Index(value = ["name"], unique = true)])
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    val color: Int = 0
)

@Entity(tableName = "assignments", foreignKeys = [ForeignKey(
    entity = Subject::class, parentColumns = ["id"], childColumns = ["subjectId"],
    onDelete = ForeignKey.RESTRICT
)], indices = [Index("subjectId")])
data class Assignment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val subjectId: Long,
    val type: String = "Tarea",
    val dueDate: Long,
    val priority: String = "Media",
    val status: String = "Pendiente"
)

@Dao
interface PlannerDao {
    @Query("SELECT * FROM subjects ORDER BY name COLLATE NOCASE")
    fun subjects(): Flow<List<Subject>>
    @Query("SELECT * FROM assignments ORDER BY dueDate, id")
    fun assignments(): Flow<List<Assignment>>
    @Upsert suspend fun save(subject: Subject)
    @Upsert suspend fun save(assignment: Assignment)
    @Delete suspend fun delete(subject: Subject)
    @Delete suspend fun delete(assignment: Assignment)
    @Query("UPDATE assignments SET status = :status WHERE id = :id")
    suspend fun changeStatus(id: Long, status: String)
}

@Database(entities = [Subject::class, Assignment::class], version = 1, exportSchema = true)
abstract class PlannerDatabase : RoomDatabase() {
    abstract fun dao(): PlannerDao
    companion object {
        @Volatile private var instance: PlannerDatabase? = null
        fun get(context: Context): PlannerDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext,
                PlannerDatabase::class.java, "mi-entrega.db").build().also { instance = it }
        }
    }
}
