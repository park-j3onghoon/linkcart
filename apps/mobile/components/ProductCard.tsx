import { MALL_LABELS, type ParseResponse } from "@linkcart/shared";
import { useState } from "react";
import { Image, Linking, Pressable, StyleSheet, Text, View } from "react-native";

type ProductCardProps = {
  imageUri: string | null;
  product: ParseResponse;
};

function formatPrice(product: ParseResponse): string {
  if (!product.price) {
    return "가격 정보 없음";
  }

  const formattedAmount = new Intl.NumberFormat("ko-KR").format(product.price.amount);
  if (product.price.currency === "KRW") {
    return `${formattedAmount}원`;
  }

  return `${formattedAmount} ${product.price.currency}`;
}

export function ProductCard({ imageUri, product }: ProductCardProps) {
  const [imageFailed, setImageFailed] = useState(false);
  const hasPartialFields = product.partial && Object.keys(product.partial).length > 0;
  const sourceUrl = product.sourceUrl;

  return (
    <View style={styles.card}>
      <View style={styles.imageFrame}>
        {imageUri && !imageFailed ? (
          <Image
            accessibilityLabel={`${product.name ?? "상품"} 이미지`}
            onError={() => setImageFailed(true)}
            resizeMode="cover"
            source={{ uri: imageUri }}
            style={styles.image}
          />
        ) : (
          <View style={styles.imageFallback}>
            <Text style={styles.imageFallbackEyebrow}>Image Fallback</Text>
            <Text style={styles.imageFallbackText}>이미지를 준비하지 못했습니다</Text>
          </View>
        )}
      </View>

      <View style={styles.content}>
        <View style={styles.badges}>
          <Text style={[styles.badge, styles.mallBadge]}>
            {product.mall ? MALL_LABELS[product.mall] : "출처 미상"}
          </Text>
          <Text style={[styles.badge, styles.parserBadge]}>{product.parserUsed}</Text>
          {product.fallbackUsed ? (
            <Text style={[styles.badge, styles.fallbackBadge]}>OG 폴백</Text>
          ) : null}
          {hasPartialFields ? (
            <Text style={[styles.badge, styles.partialBadge]}>부분 파싱</Text>
          ) : null}
        </View>

        <View style={styles.copyBlock}>
          <Text style={styles.name}>{product.name ?? "상품명을 불러오지 못했습니다"}</Text>
          <Text style={styles.price}>{formatPrice(product)}</Text>
        </View>

        {sourceUrl ? (
          <Pressable
            onPress={() => {
              void Linking.openURL(sourceUrl);
            }}
          >
            <Text style={styles.link}>원본 상품 페이지 열기</Text>
          </Pressable>
        ) : (
          <Text style={styles.linkDisabled}>원본 상품 링크를 제공하지 않았습니다</Text>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: "#ffffff",
    borderColor: "#d9e2ec",
    borderRadius: 26,
    borderWidth: 1,
    overflow: "hidden",
  },
  imageFrame: {
    aspectRatio: 4 / 3,
    backgroundColor: "#e0f2fe",
  },
  image: {
    height: "100%",
    width: "100%",
  },
  imageFallback: {
    alignItems: "center",
    flex: 1,
    gap: 6,
    justifyContent: "center",
    paddingHorizontal: 24,
    backgroundColor: "#eff6ff",
  },
  imageFallbackEyebrow: {
    color: "#64748b",
    fontSize: 11,
    fontWeight: "700",
    letterSpacing: 1.8,
    textTransform: "uppercase",
  },
  imageFallbackText: {
    color: "#475569",
    fontSize: 14,
    fontWeight: "600",
  },
  content: {
    gap: 16,
    paddingHorizontal: 18,
    paddingVertical: 18,
  },
  badges: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
  },
  badge: {
    borderRadius: 999,
    overflow: "hidden",
    paddingHorizontal: 10,
    paddingVertical: 6,
    fontSize: 11,
    fontWeight: "700",
  },
  mallBadge: {
    backgroundColor: "#0f172a",
    color: "#f8fafc",
  },
  parserBadge: {
    backgroundColor: "#e0f2fe",
    color: "#0369a1",
  },
  fallbackBadge: {
    backgroundColor: "#fef3c7",
    color: "#b45309",
  },
  partialBadge: {
    backgroundColor: "#ffe4e6",
    color: "#be123c",
  },
  copyBlock: {
    gap: 8,
  },
  name: {
    color: "#0f172a",
    fontSize: 21,
    fontWeight: "700",
    lineHeight: 30,
  },
  price: {
    color: "#0f172a",
    fontSize: 24,
    fontWeight: "700",
  },
  link: {
    color: "#0369a1",
    fontSize: 14,
    fontWeight: "700",
  },
  linkDisabled: {
    color: "#94a3b8",
    fontSize: 14,
    fontWeight: "600",
  },
});
