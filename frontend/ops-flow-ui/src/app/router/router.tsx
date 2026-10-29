import {
    createBrowserRouter,
} from 'react-router';
import {AppLayout} from "@/app/layouts/AppLayout.tsx";



export const router =
    createBrowserRouter([
        {
            path: '/',
            element: <AppLayout />,
            children: [
                {
                    index: true,
                    element: (
                        <div>
                            Dashboard
                        </div>
                    ),
                },
            ],
        },
    ]);