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
        if (product.userId != userId) {
            // 타인 소유 — 존재 정보 노출 방지로 동일한 not-found 반환.
            throw UserProductNotFoundException("상품을 찾을 수 없습니다")
        }
        userProductRepository.deleteById(productId)
    }
}
