package com.linkcart.presentation.dto

import com.linkcart.domain.model.UserProduct
import com.linkcart.domain.model.ParserName
import com.linkcart.domain.vo.Mall
import com.linkcart.domain.vo.Money
import java.time.Instant

/** AIP-148 resource: name = "users/me/products/{id}". */
data class UserProductResponse(
    val name: String,
    val displayName: String,
    val price: Money,
    val imageUrl: String?,
    val sourceUrl: String,
    val mall: Mall,
    val parserUsed: ParserName,
    val createTime: Instant?,
) {
    companion object {
        fun from(product: UserProduct): UserProductResponse = UserProductResponse(
            name = "users/me/products/${requireNotNull(product.id)}",
            displayName = product.name,
            price = product.price,
            imageUrl = product.imageUrl,
            sourceUrl = product.sourceUrl,
            mall = product.mall,
            parserUsed = product.parserUsed,
            createTime = product.createdAt,
        )
    }
}

data class UserProductsResponse(
    val products: List<UserProductResponse>,
    val nextPageToken: String? = null,
)
