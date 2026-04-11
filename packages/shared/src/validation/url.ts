const URL_PATTERN = /^https?:\/\/.+/;

export function validateUrl(input: string): { valid: boolean; error?: string } {
  const trimmed = input.trim();

  if (!trimmed) {
    return { valid: false, error: "URL을 입력해주세요" };
  }

  if (!URL_PATTERN.test(trimmed)) {
    return { valid: false, error: "올바른 URL 형식이 아닙니다 (http:// 또는 https://)" };
  }

  try {
    new URL(trimmed);
    return { valid: true };
  } catch {
    return { valid: false, error: "올바른 URL 형식이 아닙니다" };
  }
}
