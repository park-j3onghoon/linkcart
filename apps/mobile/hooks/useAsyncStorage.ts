import AsyncStorage from "@react-native-async-storage/async-storage";
import { useEffect, useRef, useState, type Dispatch, type SetStateAction } from "react";

type UseAsyncStorageResult<T> = {
  isHydrated: boolean;
  setValue: Dispatch<SetStateAction<T>>;
  value: T;
};

export function useAsyncStorage<T>(
  key: string,
  initialValue: T,
): UseAsyncStorageResult<T> {
  const initialValueRef = useRef(initialValue);
  const [value, setValue] = useState(initialValueRef.current);
  const [isHydrated, setIsHydrated] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function hydrate() {
      try {
        const storedValue = await AsyncStorage.getItem(key);
        if (!cancelled && storedValue !== null) {
          setValue(JSON.parse(storedValue) as T);
        }
      } catch (err) {
        console.warn(`[useAsyncStorage] hydrate("${key}") failed; resetting`, err);
        await AsyncStorage.removeItem(key);
        if (!cancelled) {
          setValue(initialValueRef.current);
        }
      } finally {
        if (!cancelled) {
          setIsHydrated(true);
        }
      }
    }

    void hydrate();

    return () => {
      cancelled = true;
    };
  }, [key]);

  useEffect(() => {
    if (!isHydrated) {
      return;
    }

    AsyncStorage.setItem(key, JSON.stringify(value)).catch((err) => {
      // 저장소가 비활성화된 환경에서는 메모리 상태만 유지한다. 디버깅을 위해 원인은 남긴다.
      console.warn(`[useAsyncStorage] setItem("${key}") failed`, err);
    });
  }, [isHydrated, key, value]);

  return { isHydrated, setValue, value };
}
