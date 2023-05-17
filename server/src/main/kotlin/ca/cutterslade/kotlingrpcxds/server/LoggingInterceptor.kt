package ca.cutterslade.kotlingrpcxds.server

import io.github.oshai.KotlinLogging
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor

private val logger = KotlinLogging.logger {}

object LoggingInterceptor : ServerInterceptor {
  private fun ServerCall<*, *>?.logDetails() {
    this?.also {
      logger.debug {
        """Server Call Details (${this::class.qualifiedName}):
          | - Method Descriptor: $methodDescriptor
          | - Authority: $authority
          | - Security Level: $securityLevel
          | - Attributes: $attributes
        """.trimMargin()
      }
    } ?: logger.warn { "Server Call is null" }
  }

  private fun Metadata.logDetails() {
    logger.debug { "Call Metadata: ${toString()}" }
  }

  override fun <ReqT : Any?, RespT : Any?> interceptCall(
    call: ServerCall<ReqT, RespT>,
    headers: Metadata,
    next: ServerCallHandler<ReqT, RespT>
  ): ServerCall.Listener<ReqT> {
    call.logDetails()
    headers.logDetails()
    return next.startCall(call, headers)
  }
}
