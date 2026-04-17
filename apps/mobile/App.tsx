import { StatusBar } from "expo-status-bar";
import { SafeAreaView, StyleSheet, Text, View } from "react-native";
import { ParseProgress } from "./components/ParseProgress";
import { ProductList } from "./components/ProductList";
import { UrlInput } from "./components/UrlInput";
import { useProducts } from "./hooks/useProducts";

export default function App() {
  const {
    buildImageUri,
    feedback,
    isHydrated,
    isWorking,
    phase,
    products,
    submitUrl,
  } = useProducts();
  const lastParserUsed = products[0]?.parser_used ?? "대기 중";

  return (
    <SafeAreaView style={styles.safeArea}>
      <StatusBar style="light" />
      <ProductList
        buildImageUri={buildImageUri}
        header={
          <>
            <View style={styles.heroCard}>
              <Text style={styles.heroEyebrow}>Linkcart Mobile</Text>
              <Text style={styles.heroTitle}>
                쇼핑 링크를 붙여 넣으면 모바일 카드 리스트로 바로 정리합니다.
              </Text>
              <Text style={styles.heroDescription}>
                웹과 같은 shared API 클라이언트와 URL 검증을 재사용하고, 결과는 AsyncStorage에 보관합니다.
              </Text>

              <View style={styles.statsRow}>
                <View style={[styles.statCard, styles.statCardDark]}>
                  <Text style={styles.statEyebrowLight}>Stored</Text>
                  <Text style={styles.statValueLight}>{products.length}</Text>
                  <Text style={styles.statDescriptionLight}>저장된 카드 수</Text>
                </View>
                <View style={[styles.statCard, styles.statCardLight]}>
                  <Text style={styles.statEyebrowDark}>Last Parser</Text>
                  <Text style={styles.statValueDark}>{lastParserUsed}</Text>
                  <Text style={styles.statDescriptionDark}>최근 성공 파서</Text>
                </View>
              </View>
            </View>

            <View style={styles.panel}>
              <UrlInput
                feedback={feedback}
                isHydrated={isHydrated}
                isWorking={isWorking}
                onSubmit={submitUrl}
              />
              <View style={styles.progressWrapper}>
                <ParseProgress phase={phase} />
              </View>
            </View>
          </>
        }
        isHydrated={isHydrated}
        products={products}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: "#091525",
  },
  heroCard: {
    borderColor: "rgba(255,255,255,0.08)",
    borderRadius: 30,
    borderWidth: 1,
    backgroundColor: "#10213a",
    gap: 16,
    paddingHorizontal: 22,
    paddingVertical: 24,
  },
  heroEyebrow: {
    color: "#67e8f9",
    fontSize: 12,
    fontWeight: "700",
    letterSpacing: 2.4,
    textTransform: "uppercase",
  },
  heroTitle: {
    color: "#f8fafc",
    fontSize: 34,
    fontWeight: "700",
    lineHeight: 42,
  },
  heroDescription: {
    color: "#cbd5e1",
    fontSize: 15,
    lineHeight: 24,
  },
  statsRow: {
    gap: 12,
  },
  statCard: {
    borderRadius: 22,
    paddingHorizontal: 16,
    paddingVertical: 16,
  },
  statCardDark: {
    backgroundColor: "#020617",
  },
  statCardLight: {
    backgroundColor: "#ecfeff",
  },
  statEyebrowLight: {
    color: "#67e8f9",
    fontSize: 11,
    fontWeight: "700",
    letterSpacing: 2,
    textTransform: "uppercase",
  },
  statValueLight: {
    color: "#f8fafc",
    fontSize: 28,
    fontWeight: "700",
    marginTop: 10,
  },
  statDescriptionLight: {
    color: "#cbd5e1",
    fontSize: 13,
    marginTop: 6,
  },
  statEyebrowDark: {
    color: "#0369a1",
    fontSize: 11,
    fontWeight: "700",
    letterSpacing: 2,
    textTransform: "uppercase",
  },
  statValueDark: {
    color: "#0f172a",
    fontSize: 24,
    fontWeight: "700",
    marginTop: 10,
  },
  statDescriptionDark: {
    color: "#475569",
    fontSize: 13,
    marginTop: 6,
  },
  panel: {
    borderColor: "rgba(255,255,255,0.08)",
    borderRadius: 30,
    borderWidth: 1,
    backgroundColor: "#10213a",
    paddingHorizontal: 20,
    paddingVertical: 22,
  },
  progressWrapper: {
    marginTop: 18,
  },
});
