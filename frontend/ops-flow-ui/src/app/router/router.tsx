import {createBrowserRouter} from 'react-router';
import {AppLayout} from '@/app/layouts/AppLayout';
import {AuthLayout} from '@/app/layouts/AuthLayout';
import {LoginPage} from '@/modules/auth/pages/LoginPage';
import {ProtectedRoute} from '@/app/router/ProtectedRoute.tsx';
import {PermissionRoute} from '@/app/router/PermissionRoute.tsx';
import {navigationItems} from '@/app/navigation/navigation';
import {ModulePage, OverviewPage, StatusPage} from '@/app/views/WorkspacePages';

export const router = createBrowserRouter([
    {element: <AuthLayout />, children: [{path: '/login', element: <LoginPage />}]},
    {element: <ProtectedRoute />, children: [
        {element: <AppLayout />, children: [
            {path: '/', element: <OverviewPage />},
            ...navigationItems.filter(item => item.path !== '/').map(item => {
                const route = {path: item.path, element: <ModulePage item={item} />};
                return item.permission
                    ? {element: <PermissionRoute permission={item.permission} />, children: [route]}
                    : route;
            }),
            {path: '*', element: <StatusPage code="404" title="Page not found" description="This page could not be found. Return to your workspace to continue." />},
        ]},
    ]},
]);
