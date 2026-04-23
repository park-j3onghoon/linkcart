package com.linkcart.application.share.usecase

import com.linkcart.application.share.port.ShareListTokenGenerator
import com.linkcart.application.user.usecase.UserProductNotFoundException
import com.linkcart.domain.model.ShareList
import com.linkcart.domain.model.ShareListItem
import com.linkcart.domain.model.UserProduct
import com.linkcart.domain.port.ShareListRepository
import com.linkcart.domain.port.UserProductRepository
import org.springframework.stereotype.Service
import java.time.Instant

class EmptyShareListException(message: String) : RuntimeException(message)

@Service
class CreateShareListUsecase(
    private val userProductRepository: UserProductRepository,
    private val shareListRepository: ShareListRepository,
    private val tokenGenerator: ShareListTokenGenerator,
) {

    fun execute(
        ownerId: Long,
        productIds: List<Long>,
        title: String? = null,
        expiresAt: Instant? = null,
    ): ShareList {
        if (productIds.isEmpty()) {
            throw EmptyShareListException("공유할 상품을 1개 이상 선택해주세요")
        }
        val products = productIds.map { id -> findOwnedProduct(id, ownerId) }
        val items = products.map(ShareListItem::fromUserProduct)
        val shareList = ShareList(
            ownerId = ownerId,
            token = tokenGenerator.generate(),
            title = title,
            expiresAt = expiresAt,
            items = items,
        )
        return shareListRepository.save(shareList)
    }

    private fun findOwnedProduct(productId: Long, ownerId: Long): UserProduct {
        val product = userProductRepository.findById(productId)
            ?: throw UserProductNotFoundException("상품을 찾을 수 없습니다")
        if (product.userId != ownerId) {
            throw UserProductNotFoundException("상품을 찾을 수 없습니다")
        }
        return product
    }
}
