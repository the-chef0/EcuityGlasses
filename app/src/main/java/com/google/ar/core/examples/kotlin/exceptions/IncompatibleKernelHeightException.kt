package com.google.ar.core.examples.kotlin.exceptions

import java.lang.Exception

class IncompatibleKernelHeightException(message: String? = null, cause: Throwable? = null)
                                        : Exception(message, cause) {
    constructor(cause: Throwable) : this(null, cause)
}