import type { ParseResponse } from "@linkcart/shared";
import type { ReactElement } from "react";
import { FlatList, StyleSheet, Text, View } from "react-native";
import { ProductCard } from "./ProductCard";

type ProductListProps = {
  buildImageUri: (imageUrl?: string | null) => string | null;
  header?: ReactElement | null;
  isHydrated: boolean;
  products: ParseResponse[];
};

export function ProductList({
  buildImageUri,
  header,
  isHydrated,
  products,
}: ProductListProps) {
  return (
    <FlatList
      contentContainerStyle={[
        styles.listContent,
        products.length === 0 && styles.listContentEmpty,
      ]}
      data={products}
      keyExtractor={(item) => item.source_url ?? `${item.parser_used}-${item.name ?? "unknown"}`}
      ListEmptyComponent={
        <View style={styles.emptyCard}>
          <Text style={styles.eyebrow}>Product List</Text>
          <Text style={styles.emptyTitle}>
            {isHydrated ? "아직 수집한 상품이 없습니다." : "저장된 상품을 불러오는 중입니다."}
          </Text>
          <Text style={styles.emptyDescription}>
            {isHydrated
              ? "첫 번째 링크를 추가하면 모바일에서도 같은 파싱 결과를 카드로 쌓을 수 있습니다."
              : "AsyncStorage에서 이전 카드 목록을 복원하고 있습니다."}
          </Text>
        </View>
      }
      ListHeaderComponent={header ? <View style={styles.headerContainer}>{header}</View> : null}
      renderItem={({ item }) => (
        <ProductCard imageUri={buildImageUri(item.image_url)} product={item} />
      )}
      showsVerticalScrollIndicator={false}
    />
  );
}

const styles = StyleSheet.create({
  emptyCard: {
    borderColor: "#d9e2ec",
    borderRadius: 28,
    borderWidth: 1,
    backgroundColor: "rgba(255,255,255,0.78)",
    gap: 10,
    paddingHorizontal: 22,
    paddingVertical: 24,
  },
  eyebrow: {
    color: "#0369a1",
    fontSize: 12,
    fontWeight: "700",
    letterSpacing: 2,
    textTransform: "uppercase",
  },
  emptyTitle: {
    color: "#0f172a",
    fontSize: 24,
    fontWeight: "700",
    lineHeight: 32,
  },
  emptyDescription: {
    color: "#475569",
    fontSize: 15,
    lineHeight: 24,
  },
  listContent: {
    gap: 16,
    paddingBottom: 16,
    paddingHorizontal: 16,
    paddingTop: 12,
  },
  listContentEmpty: {
    flexGrow: 1,
  },
  headerContainer: {
    marginBottom: 18,
  },
});
