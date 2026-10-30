import type {AuthUser} from '../types/auth.types';

export function hasPermission(user: AuthUser | null, permission?: string): boolean {
    if (!user) return false;
    return !permission || user.roles.includes('ROLE_PLATFORM_ADMIN') || user.permissions.includes(permission);
}
