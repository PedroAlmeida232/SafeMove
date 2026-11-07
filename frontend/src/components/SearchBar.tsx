import { Search, X } from 'lucide-react-native';
import { ActivityIndicator, StyleSheet, TextInput, View, type StyleProp, type ViewStyle } from 'react-native';

import { colors, radii, shadows, type as typography } from '../theme/tokens';
import { Avatar } from './Avatar';
import { Touchable } from './Pressable';

type Props = {
  value: string;
  onChangeText: (t: string) => void;
  onSubmit: () => void;
  onClear?: () => void;
  onAvatarPress: () => void;
  initials: string;
  loading?: boolean;
  style?: StyleProp<ViewStyle>;
};

/** Barra de busca flutuante sobre o mapa (altura 48, raio 14). */
export function SearchBar({
  value,
  onChangeText,
  onSubmit,
  onClear,
  onAvatarPress,
  initials,
  loading,
  style,
}: Props) {
  return (
    <View style={[styles.bar, style]}>
      {loading ? (
        <ActivityIndicator size="small" color={colors.tertiary} style={styles.icon} />
      ) : (
        <Search size={18} color={colors.tertiary} strokeWidth={2} style={styles.icon} />
      )}
      <TextInput
        value={value}
        onChangeText={onChangeText}
        onSubmitEditing={onSubmit}
        placeholder="Buscar endereço ou lugar"
        placeholderTextColor={colors.tertiary}
        returnKeyType="search"
        autoCorrect={false}
        style={styles.input}
        accessibilityLabel="Buscar endereço ou lugar"
      />
      {value.length > 0 && onClear ? (
        <Touchable onPress={onClear} hitSlop={10} accessibilityRole="button" accessibilityLabel="Limpar busca" style={styles.clear}>
          <View style={styles.clearCircle}>
            <X size={12} color={colors.white} strokeWidth={3} />
          </View>
        </Touchable>
      ) : null}
      <Touchable
        onPress={onAvatarPress}
        hitSlop={6}
        accessibilityRole="button"
        accessibilityLabel="Abrir perfil"
        style={styles.avatar}
      >
        <Avatar initials={initials} size={32} />
      </Touchable>
    </View>
  );
}

const styles = StyleSheet.create({
  bar: {
    height: 48,
    borderRadius: radii.searchBar,
    backgroundColor: colors.background,
    flexDirection: 'row',
    alignItems: 'center',
    paddingLeft: 14,
    paddingRight: 8,
    boxShadow: shadows.floating,
  },
  icon: {
    marginRight: 8,
    width: 18,
  },
  input: {
    ...typography.body,
    flex: 1,
    height: 48,
    color: colors.label,
    paddingVertical: 0,
  },
  clear: {
    width: 32,
    height: 44,
    alignItems: 'center',
    justifyContent: 'center',
  },
  clearCircle: {
    width: 18,
    height: 18,
    borderRadius: 9,
    backgroundColor: 'rgba(60,60,67,0.3)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatar: {
    marginLeft: 4,
    minWidth: 44,
    minHeight: 44,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
