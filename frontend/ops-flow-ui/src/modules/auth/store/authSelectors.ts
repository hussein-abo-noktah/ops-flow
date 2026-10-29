import type {
    RootState,
} from '@/app/store/store';

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
        state.auth.user
            ?.permissions
            .includes(permission)
        ?? false;