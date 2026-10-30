import {Navigate, Outlet} from 'react-router';
import {useAppSelector} from '@/app/store/hooks';

export function AuthLayout() {
    const status = useAppSelector(state => state.auth.status);

    if (status === 'checking') {
        return <div role="status">Loading session...</div>;
    }

    if (status === 'authenticated') {
        return <Navigate to="/" replace />;
    }

    return (
        <div>
            <Outlet />
        </div>
    );
}
