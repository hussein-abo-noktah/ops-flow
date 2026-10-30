import type {AuthUser} from '@/modules/auth/types/auth.types';
import {hasPermission} from '@/modules/auth/utils/permissions';



import type {NavigationItem} from '@/app/navigation/type.ts';
export type {NavigationIconName} from './type';

export const navigationItems:
    NavigationItem[] = [
    {
        label: 'Dashboard',
        path: '/',
        icon: 'overview',
    },

    {
        label: 'Incidents',
        path: '/incidents',
        icon: 'incidents',
        permission: 'incident:view',
    },

    {
        label: 'Teams',
        path: '/teams',
        icon: 'teams',
        permission: 'team:view',
    },

    {
        label: 'Users',
        path: '/users',
        icon: 'users',
        permission: 'user:view',
    },
];

export function canAccessItem(user: AuthUser | null, item: NavigationItem) {
    return hasPermission(user, item.permission);
}

export function getVisibleNavigation(user: AuthUser | null) {
    return navigationItems.filter(item => canAccessItem(user, item));
}
