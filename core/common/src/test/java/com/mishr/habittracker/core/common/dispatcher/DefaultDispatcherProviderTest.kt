package com.mishr.habittracker.core.common.dispatcher

import org.junit.Assert.assertNotNull
import org.junit.Test

class DefaultDispatcherProviderTest {

    @Test
    fun `DefaultDispatcherProvider returns non-null dispatchers for all properties`() {
        val provider = DefaultDispatcherProvider()
        
        assertNotNull(provider.io)
        assertNotNull(provider.main)
        assertNotNull(provider.default)
        assertNotNull(provider.unconfined)
    }
}
