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

class InvalidPageTokenException(message: String) : RuntimeException(message)

@Service
class ListUserProductsUsecase(
    private val userProductRepository: UserProductRepository,
) {
    fun execute(userId: Long, pageSize: Int, pageToken: String?): ListUserProductsPage {
        val limit = pageSize.coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE)
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

    private fun encodeCursor(cursor: Cursor): String =
        BASE64_URL_ENCODER.encodeToString(
            "${cursor.createdAt.toEpochMilli()}:${cursor.id}".toByteArray(),
        )

    private fun decodeCursor(token: String): Cursor {
        val raw = try {
            String(BASE64_URL_DECODER.decode(token))
        } catch (e: IllegalArgumentException) {
            throw InvalidPageTokenException("page_token이 올바르지 않습니다")
        }
        val parts = raw.split(":")
        if (parts.size != 2) throw InvalidPageTokenException("page_token이 올바르지 않습니다")
        return try {
            Cursor(Instant.ofEpochMilli(parts[0].toLong()), parts[1].toLong())
        } catch (e: NumberFormatException) {
            throw InvalidPageTokenException("page_token이 올바르지 않습니다")
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
