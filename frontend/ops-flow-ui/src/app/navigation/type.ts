export type NavigationIconName = 'overview' | 'incidents' | 'teams' | 'users' | 'audit';

export interface NavigationItem {
    label: string;
    path: string;
    permission?: string;
    icon: NavigationIconName;
}
