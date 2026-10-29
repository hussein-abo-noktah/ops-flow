import {
    Navigate,
    Outlet,
} from 'react-router';

import {
    useAppSelector,
} from '@/app/store/hooks';

export function ProtectedRoute() {

    const status =
        useAppSelector(
            state =>
                state.auth.status
        );

    if (status === 'checking') {
        return (
            <div>
                Loading session...
            </div>
        );
    }

    if (
        status ===
        'unauthenticated'
    ) {

        return (
            <Navigate
                to="/login"
                replace
            />
        );
    }

    return <Outlet />;
}