import { validateUrl } from "@linkcart/shared";
import { useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";
import type { Feedback } from "../hooks/useProducts";

type UrlInputProps = {
  feedback: Feedback | null;
  isHydrated: boolean;
  isWorking: boolean;
  onSubmit: (url: string) => Promise<boolean> | boolean;
};

const noticeColors = {
  error: {
    backgroundColor: "#ffe4e6",
    borderColor: "#fecdd3",
    color: "#be123c",
  },
  success: {
    backgroundColor: "#dcfce7",
    borderColor: "#86efac",
    color: "#15803d",
  },
  warning: {
    backgroundColor: "#fef3c7",
    borderColor: "#fcd34d",
    color: "#b45309",
  },
} as const;

export function UrlInput({
  feedback,
  isHydrated,
  isWorking,
  onSubmit,
}: UrlInputProps) {
  const [url, setUrl] = useState("");
  const [localError, setLocalError] = useState<string | null>(null);

  async function handlePress() {
    const normalizedUrl = url.trim();
    const validation = validateUrl(normalizedUrl);

    if (!validation.valid) {
      setLocalError(validation.error ?? "올바른 URL을 입력해주세요");
      return;
    }

    setLocalError(null);
    const didSave = await onSubmit(normalizedUrl);
    if (didSave) {
      setUrl("");
    }
  }

  const activeNotice = localError
    ? { kind: "error" as const, text: localError }
    : feedback;

  return (
    <View style={styles.wrapper}>
      <View style={styles.copyBlock}>
        <Text style={styles.eyebrow}>Parser Input</Text>
        <Text style={styles.title}>상품 URL을 붙여 넣으면 모바일 카드로 바로 정리합니다.</Text>
        <Text style={styles.description}>
          링크 중복을 막고, 성공한 결과는 AsyncStorage에 저장해서 앱을 다시 열어도 이어서 볼 수 있습니다.
        </Text>
      </View>

      <TextInput
        autoCapitalize="none"
        autoCorrect={false}
        editable={isHydrated && !isWorking}
        onChangeText={(nextValue) => {
          setUrl(nextValue);
          if (localError) {
            setLocalError(null);
          }
        }}
        placeholder="https://www.coupang.com/vp/products/..."
        placeholderTextColor="#94a3b8"
        style={styles.input}
        value={url}
      />

      <Pressable
        accessibilityRole="button"
        disabled={!isHydrated || isWorking}
        onPress={() => {
          void handlePress();
        }}
        style={({ pressed }) => [
          styles.button,
          (!isHydrated || isWorking) && styles.buttonDisabled,
          pressed && isHydrated && !isWorking && styles.buttonPressed,
        ]}
      >
        <Text style={styles.buttonText}>
          {isWorking ? "파싱 중..." : isHydrated ? "상품 추가" : "저장소 준비 중..."}
        </Text>
      </Pressable>

      <View style={styles.tipBox}>
        <Text style={styles.tipText}>
          Android 에뮬레이터는 기본적으로 10.0.2.2로, iOS 시뮬레이터는 127.0.0.1로 로컬 백엔드에 접근합니다.
        </Text>
      </View>

      {activeNotice ? (
        <View
          style={[
            styles.notice,
            {
              backgroundColor: noticeColors[activeNotice.kind].backgroundColor,
              borderColor: noticeColors[activeNotice.kind].borderColor,
            },
          ]}
        >
          <Text style={[styles.noticeText, { color: noticeColors[activeNotice.kind].color }]}>
            {activeNotice.text}
          </Text>
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: {
    gap: 16,
  },
  copyBlock: {
    gap: 8,
  },
  eyebrow: {
    color: "#67e8f9",
    fontSize: 12,
    fontWeight: "700",
    letterSpacing: 2.2,
    textTransform: "uppercase",
  },
  title: {
    color: "#f8fafc",
    fontSize: 28,
    fontWeight: "700",
    lineHeight: 36,
  },
  description: {
    color: "#cbd5e1",
    fontSize: 15,
    lineHeight: 24,
  },
  input: {
    borderColor: "rgba(255,255,255,0.16)",
    borderRadius: 20,
    borderWidth: 1,
    color: "#f8fafc",
    fontSize: 15,
    paddingHorizontal: 18,
    paddingVertical: 16,
    backgroundColor: "rgba(255,255,255,0.08)",
  },
  button: {
    alignItems: "center",
    backgroundColor: "#67e8f9",
    borderRadius: 20,
    justifyContent: "center",
    minHeight: 52,
    paddingHorizontal: 16,
  },
  buttonPressed: {
    opacity: 0.9,
  },
  buttonDisabled: {
    backgroundColor: "#475569",
  },
  buttonText: {
    color: "#082f49",
    fontSize: 15,
    fontWeight: "700",
  },
  tipBox: {
    borderColor: "rgba(255,255,255,0.1)",
    borderRadius: 18,
    borderWidth: 1,
    backgroundColor: "rgba(255,255,255,0.05)",
    paddingHorizontal: 16,
    paddingVertical: 14,
  },
  tipText: {
    color: "#cbd5e1",
    fontSize: 13,
    lineHeight: 20,
  },
  notice: {
    borderRadius: 18,
    borderWidth: 1,
    paddingHorizontal: 16,
    paddingVertical: 14,
  },
  noticeText: {
    fontSize: 14,
    fontWeight: "600",
    lineHeight: 20,
  },
});
