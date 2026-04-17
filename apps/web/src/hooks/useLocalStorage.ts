"use client";

import { useEffect, useRef, useState, type Dispatch, type SetStateAction } from "react";

type UseLocalStorageResult<T> = {
  value: T;
  setValue: Dispatch<SetStateAction<T>>;
  isHydrated: boolean;
};

export function useLocalStorage<T>(
  key: string,
  initialValue: T,
): UseLocalStorageResult<T> {
  const initialValueRef = useRef(initialValue);
  const [value, setValue] = useState(initialValueRef.current);
  const [isHydrated, setIsHydrated] = useState(false);

  useEffect(() => {
    if (typeof window === "undefined") {
      return;
    }

    try {
      const storedValue = window.localStorage.getItem(key);
      if (storedValue !== null) {
        setValue(JSON.parse(storedValue) as T);
      }
    } catch {
      window.localStorage.removeItem(key);
      setValue(initialValueRef.current);
    } finally {
      setIsHydrated(true);
    }
  }, [key]);

  useEffect(() => {
    if (!isHydrated || typeof window === "undefined") {
      return;
    }

    try {
      window.localStorage.setItem(key, JSON.stringify(value));
    } catch {
      // localStorage가 비활성화된 환경에서는 메모리 상태만 유지한다.
    }
  }, [isHydrated, key, value]);

  return { value, setValue, isHydrated };
}
