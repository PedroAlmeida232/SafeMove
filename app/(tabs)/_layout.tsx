import { Tabs } from 'expo-router';

import { TabBar } from '../../src/components/TabBar';
import { colors } from '../../src/theme/tokens';

export default function TabsLayout() {
  return (
    <Tabs
      tabBar={(props) => <TabBar {...props} />}
      screenOptions={{
        headerShown: false,
        sceneStyle: { backgroundColor: colors.background },
      }}
    >
      <Tabs.Screen name="index" options={{ title: 'Mapa' }} />
      <Tabs.Screen name="rotas" options={{ title: 'Rotas' }} />
      <Tabs.Screen name="alertas" options={{ title: 'Alertas' }} />
      <Tabs.Screen name="perfil" options={{ title: 'Perfil' }} />
    </Tabs>
  );
}
