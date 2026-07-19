package com.bhoomibot.os.feature.live

import android.app.Application
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Regression test for the operator-screen crash.
 *
 * Compose's viewModel() (backed by SavedStateViewModelFactory / AndroidViewModelFactory)
 * builds an AndroidViewModel by reflectively calling getConstructor(Application.class).
 * A Kotlin primary constructor whose extra params have default values does NOT synthesize
 * that (Application)-only constructor, so the call threw NoSuchMethodException and the
 * operator screen crashed the moment it opened. This asserts the constructor the factory
 * needs is present.
 */
class OperatorLiveViewModelConstructorTest {
    @Test
    fun viewModelFactoryCanConstructWithApplicationOnly() {
        val ctor = OperatorLiveViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull(
            "OperatorLiveViewModel must expose a (Application) constructor for viewModel() to build it",
            ctor
        )
    }
}
