import AsyncStorage from '@react-native-async-storage/async-storage';
import { getApp, getApps, initializeApp } from 'firebase/app';
import { getAuth, getReactNativePersistence, initializeAuth, type Auth } from 'firebase/auth';

const config = {
  apiKey: process.env.EXPO_PUBLIC_FIREBASE_API_KEY,
  authDomain: process.env.EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN,
  projectId: process.env.EXPO_PUBLIC_FIREBASE_PROJECT_ID,
  appId: process.env.EXPO_PUBLIC_FIREBASE_APP_ID,
};

export const firebaseConfigurado = Boolean(config.apiKey && config.projectId && config.appId);

function criarAuth(): Auth | null {
  if (!firebaseConfigurado) return null;
  if (getApps().length > 0) return getAuth(getApp());
  const app = initializeApp(config);
  return initializeAuth(app, { persistence: getReactNativePersistence(AsyncStorage) });
}

/** null quando as variáveis EXPO_PUBLIC_FIREBASE_* não estão preenchidas. */
export const auth: Auth | null = criarAuth();
