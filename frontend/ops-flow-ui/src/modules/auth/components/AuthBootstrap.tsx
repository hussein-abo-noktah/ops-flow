import {
    useEffect,
    useState,
    type PropsWithChildren,
} from 'react';

import {
    useAppDispatch,
} from '@/app/store/hooks';

import {refreshAccessToken} from '@/shared/api/tokenRefresh';

import {
    clearAuth,
} from '../store/authSlice';

export function AuthBootstrap({
                                  children,
                              }: PropsWithChildren) {

    const dispatch =
        useAppDispatch();

    const [ready, setReady] =
        useState(false);

    useEffect(() => {

        async function bootstrap() {

            try {

                await refreshAccessToken();

            } catch {

                dispatch(
                    clearAuth()
                );

            } finally {

                setReady(true);
            }
        }

        bootstrap().then();

    }, [dispatch]);

    if (!ready) {
        return (
            <div>
                Loading...
            </div>
        );
    }

    return children;
}
