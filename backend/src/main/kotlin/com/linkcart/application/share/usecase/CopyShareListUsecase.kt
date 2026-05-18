package com.linkcart.application.share.usecase

import com.linkcart.domain.model.ParserName
import com.linkcart.domain.model.ShareListItem
import com.linkcart.domain.model.UserProduct
import com.linkcart.domain.port.UserProductRepository
import com.linkcart.domain.vo.Mall
import org.springframework.stereotype.Service

data class CopyShareListResult(
    val copiedCount: Int,
    val skippedCount: Int,
    val products: List<UserProduct>,
)

@Service
class CopyShareListUsecase(
    private val lookupShareListByTokenUsecase: LookupShareListByTokenUsecase,
    private val userProductRepository: UserProductRepository,
) {

    /**
     * token으로 ShareList를 lookup한 뒤 path의 shareListId가 일치하는 경우에만 복제한다.
     * - token이 capability이고 id는 보조 식별자. 일치하지 않으면 NotFound(404)로 응답.
     * - 만료/미존재도 NotFound (LookupShareListByTokenUsecase 동작).
     */
    fun execute(viewerId: Long, shareListId: Long, token: String): CopyShareListResult {
        val shareList = lookupShareListByTokenUsecase.execute(token)
        if (shareList.id != shareListId) {
            throw ShareListNotFoundException("공유 리스트를 찾을 수 없습니다")
        }
        val copied = mutableListOf<UserProduct>()
        var skipped = 0
        for (item in shareList.items) {
            if (userProductRepository.existsByUserIdAndSourceUrl(viewerId, item.sourceUrl)) {
                skipped += 1
                continue
            }
            copied += userProductRepository.save(item.toUserProduct(viewerId))
        }
        return CopyShareListResult(copiedCount = copied.size, skippedCount = skipped, products = copied)
    }

    private fun ShareListItem.toUserProduct(viewerId: Long): UserProduct = UserProduct(
        userId = viewerId,
        name = name,
        price = price,
        imageUrl = imageUrl,
        sourceUrl = sourceUrl,
        mall = mall,
        parserUsed = mall.toParserName(),
    )

    private fun Mall.toParserName(): ParserName = when (this) {
        Mall.COUPANG -> ParserName.COUPANG
        Mall.ELEVENST -> ParserName.ELEVENST
        Mall.GENERIC -> ParserName.OG
    }
}
