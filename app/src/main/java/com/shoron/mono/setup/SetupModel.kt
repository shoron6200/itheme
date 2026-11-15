package com.shoron.mono.setup

/** Pure setup-wizard model (JVM-tested). */
enum class SetupStepId { Home, Overlay, Accessibility, Notifications, Battery, Weather }

enum class StepState {
    /** Granted / configured. */
    Done,

    /** Not granted yet and the user has not gone to Settings for it. */
    NotEnabled,

    /** The user opened Android Settings for it and has not finished there yet. */
    Waiting,

    /** Optional step the user chose to skip. */
    Skipped,
}

data class StepStatus(val id: SetupStepId, val required: Boolean, val state: StepState)

object SetupProgress {

    fun state(granted: Boolean, opened: Boolean, skipped: Boolean): StepState = when {
        granted -> StepState.Done
        skipped -> StepState.Skipped
        opened -> StepState.Waiting
        else -> StepState.NotEnabled
    }

    /** Setup is complete when every required step is done and every optional one is done or skipped. */
    fun isComplete(steps: List<StepStatus>): Boolean = steps.all {
        it.state == StepState.Done || (!it.required && it.state == StepState.Skipped)
    }

    /** Fraction of steps finished (done or skipped). */
    fun progress(steps: List<StepStatus>): Float =
        if (steps.isEmpty()) 1f else steps.count { it.state == StepState.Done || it.state == StepState.Skipped }.toFloat() / steps.size

    /** First step that still needs attention, in order. */
    fun next(steps: List<StepStatus>): StepStatus? =
        steps.firstOrNull { it.state == StepState.NotEnabled || it.state == StepState.Waiting }
}
