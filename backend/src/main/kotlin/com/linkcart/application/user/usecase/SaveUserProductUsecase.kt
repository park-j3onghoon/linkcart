package com.linkcart.application.user.usecase

import com.linkcart.domain.model.UserProduct
import com.linkcart.domain.port.UserProductRepository
import org.springframework.stereotype.Service

class DuplicateUserProductException(message: String) : RuntimeException(message)

@Service
class SaveUserProductUsecase(
    private val userProductRepository: UserProductRepository,
) {

    fun execute(product: UserProduct): UserProduct {
        if (userProductRepository.existsByUserIdAndSourceUrl(product.userId, product.sourceUrl)) {
            throw DuplicateUserProductException("이미 저장된 상품입니다")
        }
        return userProductRepository.save(product)
    }
}
