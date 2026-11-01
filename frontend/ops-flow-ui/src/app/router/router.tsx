import {createBrowserRouter} from 'react-router';
import {AppLayout} from '@/app/layouts/AppLayout';
import {AuthLayout} from '@/app/layouts/AuthLayout';
import {LoginPage} from '@/modules/auth/pages/LoginPage';
import {ProtectedRoute} from '@/app/router/ProtectedRoute.tsx';
import {PermissionRoute} from '@/app/router/PermissionRoute.tsx';
import {navigationItems} from '@/app/navigation/navigation';
import {ModulePage, OverviewPage, StatusPage} from '@/app/views/WorkspacePages';
import {IncidentsPage} from '@/modules/incidents/pages/IncidentsPage';
import {IncidentDetailPage} from '@/modules/incidents/pages/IncidentDetailPage';

export const router = createBrowserRouter([
    {element: <AuthLayout/>, children: [{path: '/login', element: <LoginPage/>}]},
    {
        element: <ProtectedRoute/>, children: [
            {
                element: <AppLayout/>, children: [
                    {path: '/', element: <OverviewPage/>},
                    {
                        element: <PermissionRoute permission="incident:view"/>,
                        children: [{path: '/incidents/:id', element: <IncidentDetailPage/>}]
                    },
                    ...navigationItems.filter(item => item.path !== '/').map(item => {
                        const route = {
                            path: item.path,
                            element: item.path === '/incidents' ? <IncidentsPage/> : <ModulePage item={item}/>
                        };
                        return item.permission
                            ? {element: <PermissionRoute permission={item.permission}/>, children: [route]}
                            : route;
                    }),
                    {
                        path: '*',
                        element: <StatusPage code="404" title="Page not found"
                                             description="This page could not be found. Return to your workspace to continue."/>
                    },
                ]
            },
        ]
    },
]);
