package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AttachmentItem
import com.example.data.model.AttachmentType
import com.example.data.model.Category
import com.example.data.model.ChecklistItem
import com.example.data.model.Note
import com.example.data.model.Priority
import com.example.data.model.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [Task::class, Note::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tasknest.db"
                ).fallbackToDestructiveMigration()
                 .addCallback(object : RoomDatabase.Callback() {
                     override fun onCreate(db: SupportSQLiteDatabase) {
                         super.onCreate(db)
                         CoroutineScope(Dispatchers.IO).launch {
                             populateInitialData(getInstance(context))
                         }
                     }
                 })
                 .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            val todayCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayMillis = todayCal.timeInMillis
            val tomorrowMillis = todayMillis + 86400000L
            val dayAfterTomorrowMillis = todayMillis + (86400000L * 2)

            // 1. Task: Design System Review
            val t1Id = db.taskDao().insertTask(
                Task(
                    title = "Design System Review",
                    description = "Verify Material 3 Expressive corner radii and tactile spring animations.",
                    dueDate = todayMillis,
                    dueTime = "09:30",
                    priority = Priority.HIGH,
                    category = Category.WORK,
                    isCompleted = false,
                    checklistJson = ChecklistItem.encodeList(
                        listOf(
                            ChecklistItem(text = "Audit color contrast for dark & light modes", isDone = true),
                            ChecklistItem(text = "Verify 28dp+ corner radii on cards", isDone = true),
                            ChecklistItem(text = "Test 72dp squircle FAB spring bounce", isDone = false)
                        )
                    )
                )
            )

            // Attached Note to Task 1
            db.noteDao().insertNote(
                Note(
                    taskId = t1Id,
                    title = "Design Review Notes",
                    content = "The M3 Expressive guidelines emphasize vibrant tonal cards and squircle FAB with 72dp size.",
                    colorIndex = 1
                )
            )

            // 2. Task: Biology Research Paper
            val t2Id = db.taskDao().insertTask(
                Task(
                    title = "Biology Research Paper",
                    description = "Synthesize findings for chapter 4 and format bibliography.",
                    dueDate = todayMillis,
                    dueTime = "14:00",
                    priority = Priority.MEDIUM,
                    category = Category.STUDY,
                    isCompleted = false,
                    checklistJson = ChecklistItem.encodeList(
                        listOf(
                            ChecklistItem(text = "Read 3 peer-reviewed sources", isDone = true),
                            ChecklistItem(text = "Draft methods section", isDone = false),
                            ChecklistItem(text = "Check citations", isDone = false)
                        )
                    )
                )
            )

            db.noteDao().insertNote(
                Note(
                    taskId = t2Id,
                    title = "Paper Citations",
                    content = "Look up Miller et al. 2024 in Nature Biotechnology for the cellular breakdown diagram.",
                    colorIndex = 2
                )
            )

            // 3. Task: Evening Run & Stretch
            db.taskDao().insertTask(
                Task(
                    title = "Evening Run & Stretch",
                    description = "5km scenic run through the park followed by 15-minute cool down.",
                    dueDate = todayMillis,
                    dueTime = "18:00",
                    priority = Priority.LOW,
                    category = Category.PERSONAL,
                    isCompleted = true,
                    checklistJson = ChecklistItem.encodeList(
                        listOf(
                            ChecklistItem(text = "Warm-up dynamic stretch", isDone = true),
                            ChecklistItem(text = "Run 5k route", isDone = true)
                        )
                    )
                )
            )

            // 4. Task: Team Weekly Sync
            db.taskDao().insertTask(
                Task(
                    title = "Team Weekly Sync",
                    description = "Sprint retrospective and roadmap milestones alignment.",
                    dueDate = tomorrowMillis,
                    dueTime = "10:00",
                    priority = Priority.HIGH,
                    category = Category.WORK,
                    isCompleted = false
                )
            )

            // 5. Task: Grocery Run & Meal Prep
            db.taskDao().insertTask(
                Task(
                    title = "Grocery Run & Meal Prep",
                    description = "Pick up fresh produce, grains, and prep lunches for the week.",
                    dueDate = dayAfterTomorrowMillis,
                    dueTime = "11:30",
                    priority = Priority.MEDIUM,
                    category = Category.PERSONAL,
                    isCompleted = false
                )
            )

            // Standalone Notes
            // Seeded Note matching the reference image: "Weekend Project Plan"
            db.noteDao().insertNote(
                Note(
                    title = "Weekend Project Plan",
                    content = "",
                    colorIndex = 0,
                    checklistJson = ChecklistItem.encodeList(
                        listOf(
                            ChecklistItem(text = "Buy painting supplies", isDone = true),
                            ChecklistItem(text = "Research color palette", isDone = false),
                            ChecklistItem(text = "Sketch layout for living room", isDone = false)
                        )
                    ),
                    attachmentsJson = AttachmentItem.encodeList(
                        listOf(
                            AttachmentItem(
                                type = AttachmentType.IMAGE,
                                title = "IMG_2048.JPG",
                                subtitle = "PNG • 2.4 MB • Added 10:32 AM",
                                uriOrUrl = "sample://img_paint_palette",
                                extraData = "image/jpeg"
                            ),
                            AttachmentItem(
                                type = AttachmentType.VIDEO,
                                title = "design_walkthrough.mp4",
                                subtitle = "MP4 • 0:45 • Added 10:35 AM",
                                uriOrUrl = "sample://img_interior_walkthrough",
                                extraData = "0:45"
                            ),
                            AttachmentItem(
                                type = AttachmentType.LINK,
                                title = "Material 3 Expressive — Design Guidelines",
                                subtitle = "https://developer.android.com/design/material/expressive",
                                uriOrUrl = "https://developer.android.com/design/material/expressive",
                                extraData = "Explore expressive shapes, dynamic color, and components for Android 16"
                            ),
                            AttachmentItem(
                                type = AttachmentType.AUDIO,
                                title = "voice_memo_1032.m4a",
                                subtitle = "M4A • 1.2 MB • Added 10:38 AM",
                                uriOrUrl = "sample://voice_memo_1032",
                                extraData = "1:24 / 3:12"
                            )
                        )
                    )
                )
            )

            db.noteDao().insertNote(
                Note(
                    title = "Nest Architecture Principles",
                    content = "Single source of truth via Room reactive flows. Always handle exact alarms gracefully on Android 12+.",
                    colorIndex = 3
                )
            )

            db.noteDao().insertNote(
                Note(
                    title = "Reading List 📚",
                    content = "1. Designing Data-Intensive Applications\n2. The Design of Everyday Things\n3. Thinking in Systems",
                    colorIndex = 4
                )
            )
        }
    }
}
