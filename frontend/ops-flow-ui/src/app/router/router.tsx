import {
    createBrowserRouter,
} from 'react-router';
import {AppLayout} from "@/app/layouts/AppLayout.tsx";
import {AuthLayout} from "@/app/layouts/AuthLayout.tsx";
import {LoginPage} from "@/modules/auth/pages/LoginPage.tsx";
import {ProtectedRoute} from "@/app/router/ProtectedRoute.tsx";


export const router =
    createBrowserRouter([
        {
            element: <AuthLayout/>,

            children: [
                {
                    path: '/login',
                    element: <LoginPage/>,
                },
            ],
        },
        {
            element: <ProtectedRoute />,

            children: [
                {
                    element: <AppLayout />,

                    children: [
                        {
                            path: '/',
                            element: (
                                <div>
                                    Dashboard
                                </div>
                            ),
                        },
                    ],
                },
            ],
        },
    ]);