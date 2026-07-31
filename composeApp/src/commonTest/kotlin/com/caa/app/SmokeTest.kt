package com.caa.app

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Placeholder smoke test so the commonTest source set compiles and the CI
 * test task always exercises at least one test. Replace with real tests.
 */
class SmokeTest {

    @Test
    fun smokeTestPasses() {
        assertTrue(true)
    }

    @Test
    fun coroutinesTestRuntimeWorks() = runTest {
        assertTrue(1 + 1 == 2)
    }
}
