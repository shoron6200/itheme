package com.shoron.mono.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupModelTest {

    private fun step(id: SetupStepId, required: Boolean, state: StepState) = StepStatus(id, required, state)

    @Test fun stateReflectsGrantOpenAndSkip() {
        assertEquals(StepState.Done, SetupProgress.state(granted = true, opened = true, skipped = true))
        assertEquals(StepState.Skipped, SetupProgress.state(granted = false, opened = true, skipped = true))
        assertEquals(StepState.Waiting, SetupProgress.state(granted = false, opened = true, skipped = false))
        assertEquals(StepState.NotEnabled, SetupProgress.state(granted = false, opened = false, skipped = false))
    }

    @Test fun completionNeedsRequiredDoneAndOptionalDoneOrSkipped() {
        val done = listOf(
            step(SetupStepId.Home, true, StepState.Done),
            step(SetupStepId.Overlay, false, StepState.Skipped),
            step(SetupStepId.Battery, false, StepState.Done),
        )
        assertTrue(SetupProgress.isComplete(done))
        assertFalse(SetupProgress.isComplete(done + step(SetupStepId.Notifications, false, StepState.Waiting)))
        // A required step can never be skipped into completion.
        assertFalse(SetupProgress.isComplete(listOf(step(SetupStepId.Home, true, StepState.Skipped))))
    }

    @Test fun progressAndNextStep() {
        val steps = listOf(
            step(SetupStepId.Home, true, StepState.Done),
            step(SetupStepId.Overlay, false, StepState.Skipped),
            step(SetupStepId.Accessibility, false, StepState.Waiting),
            step(SetupStepId.Notifications, false, StepState.NotEnabled),
        )
        assertEquals(0.5f, SetupProgress.progress(steps), 1e-6f)
        assertEquals(SetupStepId.Accessibility, SetupProgress.next(steps)?.id)
        assertEquals(1f, SetupProgress.progress(emptyList()), 0f)
    }

    @Test fun guideStatus() {
        assertEquals(GuideStatus.Enabled, RestrictedGuideLogic.status(enabled = true, userWentToSettings = false))
        assertEquals(GuideStatus.WaitingForUser, RestrictedGuideLogic.status(enabled = false, userWentToSettings = true))
        assertEquals(GuideStatus.NotEnabled, RestrictedGuideLogic.status(enabled = false, userWentToSettings = false))
    }
}
