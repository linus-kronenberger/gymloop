package com.example.gymloop.domain

/**
 * Aggregate root representing a strength exercise and its chronological personal record history.
 */
data class Exercise(
    val id: ExerciseId,
    val name: ExerciseName,
    val records: List<PersonalRecord> = emptyList()
) {
    val sortedRecords: List<PersonalRecord> = records.sortedBy { it.recordedAt }

    fun addRecord(record: PersonalRecord): Exercise =
        copy(records = (sortedRecords + record).sortedBy { it.recordedAt })

    fun latestRecord(): PersonalRecord? = sortedRecords.lastOrNull()

    fun bestRecordByVolume(): PersonalRecord? = sortedRecords.maxByOrNull { it.volume }

    fun latestTrend(): ProgressTrend {
        if (sortedRecords.size < 2) return ProgressTrend.INSUFFICIENT_DATA
        return trendAt(sortedRecords.lastIndex)
    }

    fun trendAt(index: Int): ProgressTrend {
        if (index <= 0 || index >= sortedRecords.size) return ProgressTrend.INSUFFICIENT_DATA
        val previous = sortedRecords[index - 1].volume
        val current = sortedRecords[index].volume
        return when {
            current > previous -> ProgressTrend.BETTER
            current < previous -> ProgressTrend.WORSE
            else -> ProgressTrend.SAME
        }
    }
}
