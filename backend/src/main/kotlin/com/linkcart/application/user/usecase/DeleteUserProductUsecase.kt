package com.linkcart.application.user.usecase

import com.linkcart.domain.port.UserProductRepository
import org.springframework.stereotype.Service

class UserProductNotFoundException(message: String) : RuntimeException(message)

@Service
class DeleteUserProductUsecase(
    private val userProductRepository: UserProductRepository,
) {

    fun execute(userId: Long, productId: Long) {
        val product = userProductRepository.findById(productId)
            ?: throw UserProductNotFoundException("상품을 찾을 수 없습니다")
        // 타인 소유 시에도 동일 NotFound. 존재 정보 누설 방지 정책.
        if (!product.isOwnedBy(userId)) {
            throw UserProductNotFoundException("상품을 찾을 수 없습니다")
        }
        userProductRepository.deleteById(productId)
    }
}
