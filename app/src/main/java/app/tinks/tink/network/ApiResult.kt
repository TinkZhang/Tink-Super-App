package app.tinks.tink.network

import kotlinx.io.IOException
import retrofit2.HttpException

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(
        val message: String,
        val code: Int? = null,
        val throwable: Throwable? = null
    ) : ApiResult<Nothing>()

    object Loading: ApiResult<Nothing>()
}

suspend inline fun <T> safeApiCall(
    crossinline apiCall: suspend () -> T
): ApiResult<T> {
    return try {
        println("safeApiCall: calling api")
        ApiResult.Success(apiCall())
    } catch (e: HttpException) {
        ApiResult.Error(
            message = mapHttpError(e.code()),
            code = e.code(),
            throwable = e
        )
    } catch (e: IOException) {
        println("safeApiCall caught exception: ${e.message}")
        ApiResult.Error(
            message = "Network error. Please check your connection.",
            throwable = e
        )
    } catch (e: Exception) {
        println("safeApiCall caught exception: ${e.message}")
        ApiResult.Error(
            message = "Unexpected error occurred",
            throwable = e
        )
    }
}

fun mapHttpError(code: Int): String =
    when (code) {
        400 -> "Bad request"
        404 -> "Not found"
        500 -> "Server error"
        else -> "HTTP error ($code)"
    }
