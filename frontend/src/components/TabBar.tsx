import { BlurView } from 'expo-blur';
import { router } from 'expo-router';
import type { BottomTabBarProps } from 'expo-router/tabs';
import { Bell, CircleUser, CirclePlus, Map as MapIcon, Route, type LucideIcon } from 'lucide-react-native';
import { Fragment } from 'react';
import { Platform, Pressable, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useAuth } from '../auth/AuthProvider';
import { colors, hairline, TAB_BAR_BASE_HEIGHT } from '../theme/tokens';
import { Text } from './Text';

const ABAS: Record<string, { label: string; icon: LucideIcon }> = {
  index: { label: 'Mapa', icon: MapIcon },
  rotas: { label: 'Rotas', icon: Route },
  alertas: { label: 'Alertas', icon: Bell },
  perfil: { label: 'Perfil', icon: CircleUser },
};

type ItemProps = {
  label: string;
  icon: LucideIcon;
  active: boolean;
  onPress: () => void;
};

function TabItem({ label, icon: Icone, active, onPress }: ItemProps) {
  const cor = active ? colors.accent : colors.tabInactive;
  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => [styles.item, pressed && { opacity: 0.6 }]}
      accessibilityRole="tab"
      accessibilityState={{ selected: active }}
      accessibilityLabel={label}
    >
      <Icone size={25} color={cor} strokeWidth={active ? 2 : 1.7} />
      <Text variant="tab" color={cor} style={styles.label}>
        {label}
      </Text>
    </Pressable>
  );
}

/** Tab bar com 5 abas; "Reportar" abre o modal (e exige login). */
export function TabBar({ state, emitter, navigateToTab }: BottomTabBarProps) {
  const insets = useSafeAreaInsets();
  const { usuario } = useAuth();

  const abrirReportar = () => {
    if (usuario) router.push('/reportar');
    else router.push({ pathname: '/login', params: { next: '/reportar' } });
  };

  const conteudo = (
    <View style={[styles.row, { height: TAB_BAR_BASE_HEIGHT }]}>
      {state.routes.map((route, i) => {
        const meta = ABAS[route.name];
        if (!meta) return null;
        const ativa = state.index === i;
        const onPress = () => {
          const evento = emitter.emit({ type: 'tabPress', target: route.key, canPreventDefault: true });
          if (!ativa && !evento.defaultPrevented) navigateToTab(route.key);
        };
        return (
          <Fragment key={route.key}>
            <TabItem label={meta.label} icon={meta.icon} active={ativa} onPress={onPress} />
            {route.name === 'rotas' ? (
              <TabItem label="Reportar" icon={CirclePlus} active={false} onPress={abrirReportar} />
            ) : null}
          </Fragment>
        );
      })}
    </View>
  );

  return (
    <View style={[styles.container, { paddingBottom: insets.bottom }]} accessibilityRole="tablist">
      {Platform.OS === 'ios' ? (
        <BlurView intensity={60} tint="light" style={StyleSheet.absoluteFill} />
      ) : null}
      <View style={[StyleSheet.absoluteFill, styles.tint]} />
      {conteudo}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    borderTopWidth: hairline,
    borderTopColor: 'rgba(0,0,0,0.3)',
    overflow: 'hidden',
  },
  tint: {
    backgroundColor: colors.tabBarBg,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'stretch',
  },
  item: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingTop: 4,
    minHeight: 44,
  },
  label: {
    marginTop: 2,
  },
});
