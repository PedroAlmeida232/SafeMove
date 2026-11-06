/**
 * O pacote `firebase/auth` só publica os tipos da versão web, mas no React Native
 * o Metro resolve a build `react-native`, que exporta `getReactNativePersistence`.
 */
import type { Persistence } from 'firebase/auth';

declare module 'firebase/auth' {
  interface ReactNativeAsyncStorage {
    setItem(key: string, value: string): Promise<void>;
    getItem(key: string): Promise<string | null>;
    removeItem(key: string): Promise<void>;
  }
  export function getReactNativePersistence(storage: ReactNativeAsyncStorage): Persistence;
}
