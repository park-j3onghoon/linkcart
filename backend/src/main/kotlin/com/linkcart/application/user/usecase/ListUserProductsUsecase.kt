package com.linkcart.application.user.usecase

import com.linkcart.domain.entity.UserProduct
import com.linkcart.domain.port.UserProductRepository
import org.springframework.stereotype.Service

@Service
class ListUserProductsUsecase(
    private val userProductRepository: UserProductRepository,
) {
    fun execute(userId: Long): List<UserProduct> =
        userProductRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
}
