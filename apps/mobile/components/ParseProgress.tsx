import { ActivityIndicator, StyleSheet, Text, View } from "react-native";
import type { ParsePhase } from "../hooks/useProducts";

type ParseProgressProps = {
  phase: ParsePhase;
};

const steps = [
  {
    key: "validating",
    title: "URL 확인",
    description: "형식과 중복 여부를 먼저 점검합니다.",
  },
  {
    key: "parsing",
    title: "상품 정보 파싱",
    description: "백엔드 API가 링크를 해석하고 응답을 정리합니다.",
  },
  {
    key: "success",
    title: "카드 저장",
    description: "응답을 리스트와 AsyncStorage에 반영합니다.",
  },
] as const;

export function ParseProgress({ phase }: ParseProgressProps) {
  function getTone(stepKey: (typeof steps)[number]["key"]) {
    if (phase === "success") {
      return styles.stepComplete;
    }

    if (phase === "error" && stepKey === "parsing") {
      return styles.stepError;
    }

    if (phase === stepKey) {
      return styles.stepActive;
    }

    if (phase === "parsing" && stepKey === "validating") {
      return styles.stepComplete;
    }

    return styles.stepIdle;
  }

  return (
    <View style={styles.wrapper}>
      <View style={styles.header}>
        <Text style={styles.eyebrow}>Progress</Text>
        {phase === "validating" || phase === "parsing" ? (
          <ActivityIndicator color="#67e8f9" size="small" />
        ) : null}
      </View>

      {steps.map((step, index) => (
        <View key={step.key} style={[styles.stepCard, getTone(step.key)]}>
          <View style={styles.stepIndex}>
            <Text style={styles.stepIndexText}>0{index + 1}</Text>
          </View>
          <View style={styles.stepCopy}>
            <Text style={styles.stepTitle}>{step.title}</Text>
            <Text style={styles.stepDescription}>{step.description}</Text>
          </View>
        </View>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: {
    gap: 12,
  },
  header: {
    alignItems: "center",
    flexDirection: "row",
    justifyContent: "space-between",
  },
  eyebrow: {
    color: "#67e8f9",
    fontSize: 12,
    fontWeight: "700",
    letterSpacing: 2.2,
    textTransform: "uppercase",
  },
  stepCard: {
    borderRadius: 18,
    borderWidth: 1,
    flexDirection: "row",
    gap: 14,
    paddingHorizontal: 16,
    paddingVertical: 14,
  },
  stepIdle: {
    backgroundColor: "rgba(255,255,255,0.05)",
    borderColor: "rgba(255,255,255,0.08)",
  },
  stepActive: {
    backgroundColor: "rgba(103,232,249,0.12)",
    borderColor: "rgba(103,232,249,0.35)",
  },
  stepComplete: {
    backgroundColor: "rgba(74,222,128,0.12)",
    borderColor: "rgba(74,222,128,0.35)",
  },
  stepError: {
    backgroundColor: "rgba(251,113,133,0.12)",
    borderColor: "rgba(251,113,133,0.35)",
  },
  stepIndex: {
    alignItems: "center",
    borderColor: "rgba(255,255,255,0.16)",
    borderRadius: 999,
    borderWidth: 1,
    height: 34,
    justifyContent: "center",
    width: 34,
  },
  stepIndexText: {
    color: "#f8fafc",
    fontSize: 13,
    fontWeight: "700",
  },
  stepCopy: {
    flex: 1,
    gap: 4,
  },
  stepTitle: {
    color: "#f8fafc",
    fontSize: 15,
    fontWeight: "700",
  },
  stepDescription: {
    color: "#cbd5e1",
    fontSize: 13,
    lineHeight: 20,
  },
});
