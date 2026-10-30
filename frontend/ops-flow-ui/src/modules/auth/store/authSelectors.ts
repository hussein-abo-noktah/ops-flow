import type {
    RootState,
} from '@/app/store/store';
import {hasPermission} from '@/modules/auth/utils/permissions.ts';

export const selectCurrentUser =
    (state: RootState) =>
        state.auth.user;

export const selectPermissions =
    (state: RootState) =>
        state.auth.user?.permissions ?? [];

export const selectHasPermission =
    (
        state: RootState,
        permission: string
    ) =>
        hasPermission(state.auth.user, permission);
