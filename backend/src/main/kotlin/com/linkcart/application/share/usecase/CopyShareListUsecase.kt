package com.linkcart.application.share.usecase

import com.linkcart.domain.vo.ParserName
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
     * token이 capability, id는 보조. id 불일치도 NotFound로 응답해 enumeration을 막는다.
     * 만료/미존재 NotFound 처리는 [LookupShareListByTokenUsecase]에 위임.
     */
    fun execute(viewerId: Long, shareListId: Long, token: String): CopyShareListResult {
        val shareList = lookupShareListByTokenUsecase.execute(token)
        if (!shareList.hasId(shareListId)) {
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
