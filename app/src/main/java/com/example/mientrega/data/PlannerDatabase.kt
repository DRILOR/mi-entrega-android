package com.example.mientrega.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class StudentUser(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val email: String,
    val passwordHash: String,
    val passwordSalt: String
)

@Entity(
    tableName = "subjects",
    indices = [Index(value = ["ownerId", "name"], unique = true)],
    foreignKeys = [ForeignKey(
        entity = StudentUser::class,
        parentColumns = ["id"], childColumns = ["ownerId"],
        onDelete = ForeignKey.RESTRICT
    )]
)
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    val color: Int = 0,
    // Registros antiguos: sin propietario hasta el primer registro de cuenta.
    val ownerId: Long? = null
)

@Entity(
    tableName = "assignments",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class, parentColumns = ["id"],
            childColumns = ["subjectId"], onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = StudentUser::class, parentColumns = ["id"],
            childColumns = ["ownerId"], onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("subjectId"), Index("ownerId")]
)
data class Assignment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val subjectId: Long,
    val type: String = "Tarea",
    val dueDate: Long,
    val priority: String = "Media",
    val status: String = "Pendiente",
    val ownerId: Long? = null
)

@Dao
interface PlannerDao {
    @Query("SELECT * FROM users WHERE email = :email COLLATE NOCASE LIMIT 1")
    suspend fun userByEmail(email: String): StudentUser?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun userById(id: Long): StudentUser?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun userCount(): Int

    @Insert
    suspend fun insertUser(user: StudentUser): Long

    @Query("SELECT * FROM subjects WHERE ownerId = :userId ORDER BY name COLLATE NOCASE")
    fun subjects(userId: Long): Flow<List<Subject>>

    @Query("SELECT * FROM assignments WHERE ownerId = :userId ORDER BY dueDate, id")
    fun assignments(userId: Long): Flow<List<Assignment>>

    @Query("SELECT COUNT(*) FROM subjects WHERE id = :subjectId AND ownerId = :userId")
    suspend fun ownsSubject(subjectId: Long, userId: Long): Int

    @Insert
    suspend fun insertSubject(subject: Subject): Long

    @Insert
    suspend fun insertAssignment(assignment: Assignment): Long

    @Query("UPDATE subjects SET name = :name, color = :color WHERE id = :id AND ownerId = :userId")
    suspend fun updateSubject(id: Long, userId: Long, name: String, color: Int): Int

    @Query("UPDATE assignments SET title = :title, description = :description, subjectId = :subjectId, type = :type, dueDate = :dueDate, priority = :priority, status = :status WHERE id = :id AND ownerId = :userId")
    suspend fun updateAssignment(id: Long, userId: Long, title: String, description: String,
                                 subjectId: Long, type: String, dueDate: Long,
                                 priority: String, status: String): Int

    @Query("DELETE FROM subjects WHERE id = :id AND ownerId = :userId")
    suspend fun deleteSubject(id: Long, userId: Long): Int

    @Query("DELETE FROM assignments WHERE id = :id AND ownerId = :userId")
    suspend fun deleteAssignment(id: Long, userId: Long): Int

    @Query("UPDATE assignments SET status = :status WHERE id = :id AND ownerId = :userId")
    suspend fun changeStatus(id: Long, userId: Long, status: String): Int

    @Query("UPDATE subjects SET ownerId = :userId WHERE ownerId IS NULL")
    suspend fun claimLegacySubjects(userId: Long)

    @Query("UPDATE assignments SET ownerId = :userId WHERE ownerId IS NULL")
    suspend fun claimLegacyAssignments(userId: Long)
}

@Database(entities = [StudentUser::class, Subject::class, Assignment::class], version = 2, exportSchema = true)
abstract class PlannerDatabase : RoomDatabase() {
    abstract fun dao(): PlannerDao

    companion object {
        // No usar fallbackToDestructiveMigration: conserva las materias y actividades.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `displayName` TEXT NOT NULL, `email` TEXT COLLATE NOCASE NOT NULL, `passwordHash` TEXT NOT NULL, `passwordSalt` TEXT NOT NULL)""")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_email` ON `users` (`email`)")
                db.execSQL("ALTER TABLE `subjects` ADD COLUMN `ownerId` INTEGER REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT")
                db.execSQL("ALTER TABLE `assignments` ADD COLUMN `ownerId` INTEGER REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT")
                db.execSQL("DROP INDEX IF EXISTS `index_subjects_name`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_subjects_ownerId_name` ON `subjects` (`ownerId`, `name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_assignments_ownerId` ON `assignments` (`ownerId`)")
            }
        }

        @Volatile private var instance: PlannerDatabase? = null
        fun get(context: Context): PlannerDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, PlannerDatabase::class.java, "mi-entrega.db"
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
