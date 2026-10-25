package com.mishr.habittracker.core.designsystem.component

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDate

@Immutable
class StableSet<T> private constructor(private val items: Set<T>) : Set<T> by items {
    companion object {
        fun <T> create(elements: Iterable<T>): StableSet<T> {
            // Defensively copy into a new read-only set
            return StableSet(elements.toSet())
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StableSet<*>) return false
        return items == other.items
    }

    override fun hashCode(): Int = items.hashCode()
    
    override fun toString(): String = items.toString()
}

fun <T> Iterable<T>.toStableSet(): StableSet<T> = StableSet.create(this)
