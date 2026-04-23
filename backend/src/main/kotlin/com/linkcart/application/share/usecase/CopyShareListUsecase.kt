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
    private val getShareListByTokenUsecase: GetShareListByTokenUsecase,
    private val userProductRepository: UserProductRepository,
) {

    fun execute(viewerId: Long, token: String): CopyShareListResult {
        val shareList = getShareListByTokenUsecase.execute(token)
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
