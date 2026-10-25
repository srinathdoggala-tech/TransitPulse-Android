package com.mishr.habittracker.domain.exception

class RequiresRecentLoginException(message: String = "This operation requires a recent login.") : Exception(message)
