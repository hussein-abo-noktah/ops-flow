import { store } from '@/app/store/store';

import {
    clearAuth,
    setAuth,
} from '@/modules/auth/store/authSlice';

import {
    refreshSession,
} from '@/modules/auth/api/authApi';

import {
    decodeUserFromJwt,
} from '@/modules/auth/utils/decodeUserFromJwt';

let refreshPromise:
    Promise<string> | null = null;

export function refreshAccessToken():
    Promise<string> {

    if (refreshPromise) {
        return refreshPromise;
    }

    refreshPromise =
        refreshSession()
            .then(auth => {

                const token =
                    auth.accessToken;

                store.dispatch(
                    setAuth({
                        accessToken: token,
                        user:
                            decodeUserFromJwt(token),
                    })
                );

                return token;
            })
            .catch(error => {

                store.dispatch(
                    clearAuth()
                );

                throw error;
            })
            .finally(() => {

                refreshPromise = null;
            });

    return refreshPromise;
}