import {
    Outlet,
} from 'react-router';
import {useAppDispatch, useAppSelector} from "@/app/store/hooks.ts";
import {toggleSidebar} from "@/app/store/slices/uiSlice.ts";




export function AppLayout() {

    const dispatch = useAppDispatch();
    const  sidebarOpen = useAppSelector(state => state.ui.sidebarOpen);
    return (
        <div>

            <button
                onClick={() =>
                    dispatch(
                        toggleSidebar()
                    )
                }
            >
                Toggle
            </button>

            {sidebarOpen && (
                <aside>
                    Sidebar
                </aside>
            )}

            <main>
                <Outlet />
            </main>

        </div>
    );
}