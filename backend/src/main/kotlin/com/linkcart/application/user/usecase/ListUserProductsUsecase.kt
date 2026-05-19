package com.linkcart.application.user.usecase

import com.linkcart.domain.model.UserProduct
import com.linkcart.domain.port.UserProductRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Base64

data class ListUserProductsPage(
    val products: List<UserProduct>,
    val nextPageToken: String?,
)

class InvalidPageTokenException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class InvalidPageSizeException(message: String) : RuntimeException(message)

@Service
class ListUserProductsUsecase(
    private val userProductRepository: UserProductRepository,
) {
    fun execute(userId: Long, pageSize: Int, pageToken: String?): ListUserProductsPage {
        if (pageSize < MIN_PAGE_SIZE || pageSize > MAX_PAGE_SIZE) {
            throw InvalidPageSizeException(
                "pageSize는 $MIN_PAGE_SIZE 이상 $MAX_PAGE_SIZE 이하여야 합니다 (입력: $pageSize)",
            )
        }
        val limit = pageSize
        val cursor = pageToken?.let(::decodeCursor)
        // limit + 1로 조회해서 hasNext 판단
        val fetched = userProductRepository.findPageByUserId(
            userId = userId,
            cursorCreatedAt = cursor?.createdAt,
            cursorId = cursor?.id,
            limit = limit + 1,
        )
        if (fetched.size <= limit) {
            return ListUserProductsPage(products = fetched, nextPageToken = null)
        }
        val items = fetched.take(limit)
        val last = items.last()
        return ListUserProductsPage(
            products = items,
            nextPageToken = encodeCursor(Cursor(last.createdAt!!, last.id!!)),
        )
    }

    private data class Cursor(val createdAt: Instant, val id: Long)

    // PostgreSQL TIMESTAMPTZ는 마이크로초 정밀도를 가지므로 millis로 truncate하면
    // 같은 millisecond 내 다른 microsecond row가 페이지에서 누락된다.
    // epochSecond + nano(0..999_999_999)를 분리 인코딩하여 풀해상도 보존한다.
    private fun encodeCursor(cursor: Cursor): String =
        BASE64_URL_ENCODER.encodeToString(
            "${cursor.createdAt.epochSecond}.${cursor.createdAt.nano}:${cursor.id}".toByteArray(),
        )

    private fun decodeCursor(token: String): Cursor {
        val raw = try {
            String(BASE64_URL_DECODER.decode(token))
        } catch (e: IllegalArgumentException) {
            throw InvalidPageTokenException("page_token이 올바르지 않습니다: not base64", e)
        }
        val parts = raw.split(":")
        if (parts.size != 2) {
            throw InvalidPageTokenException("page_token이 올바르지 않습니다: bad shape")
        }
        val timeParts = parts[0].split(".")
        if (timeParts.size != 2) {
            throw InvalidPageTokenException("page_token이 올바르지 않습니다: bad time format")
        }
        return try {
            Cursor(
                createdAt = Instant.ofEpochSecond(timeParts[0].toLong(), timeParts[1].toLong()),
                id = parts[1].toLong(),
            )
        } catch (e: NumberFormatException) {
            throw InvalidPageTokenException("page_token이 올바르지 않습니다: bad numbers", e)
        }
    }

    companion object {
        const val MIN_PAGE_SIZE = 1
        const val MAX_PAGE_SIZE = 100
        const val DEFAULT_PAGE_SIZE = 50

        private val BASE64_URL_ENCODER: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
        private val BASE64_URL_DECODER: Base64.Decoder = Base64.getUrlDecoder()
    }
}
