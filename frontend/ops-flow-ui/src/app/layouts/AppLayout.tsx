import {useEffect, useRef} from 'react';
import {Outlet, useLocation} from 'react-router';
import {useAppDispatch, useAppSelector} from '@/app/store/hooks';
import {closeMobileSidebar} from '@/app/store/slices/uiSlice';
import {Sidebar} from '@/app/navigation/Sidebar';
import {navigationItems} from '@/app/navigation/navigation';
import {Topbar} from '@/app/navigation/topbar';
import './AppLayout.css';

export function AppLayout() {
    const dispatch = useAppDispatch();
    const location = useLocation();
    const {sidebarOpen, mobileSidebarOpen} = useAppSelector(state => state.ui);
    const dialogRef = useRef<HTMLDialogElement>(null);
    const mainRef = useRef<HTMLElement>(null);
    const currentPage = navigationItems.find(item => item.path === location.pathname);
    const pageTitle = currentPage?.label ?? 'Page not found';

    useEffect(() => {
        document.title = `${pageTitle} | OpsFlow`;
        dispatch(closeMobileSidebar());
        mainRef.current?.focus({preventScroll: true});
    }, [location.pathname, location.search, pageTitle, dispatch]);

    useEffect(() => {
        const dialog = dialogRef.current;
        if (!dialog) return;
        if (mobileSidebarOpen && !dialog.open) dialog.showModal();
        if (!mobileSidebarOpen && dialog.open) dialog.close();
        if (!mobileSidebarOpen) return;
        const previousOverflow = document.body.style.overflow;
        document.body.style.overflow = 'hidden';
        return () => { document.body.style.overflow = previousOverflow; };
    }, [mobileSidebarOpen]);

    useEffect(() => {
        const desktop = window.matchMedia('(min-width: 801px)');
        const handleResize = () => { if (desktop.matches) dispatch(closeMobileSidebar()); };
        desktop.addEventListener('change', handleResize);
        return () => desktop.removeEventListener('change', handleResize);
    }, [dispatch]);

    return <div className={`app-shell${sidebarOpen ? '' : ' sidebar-collapsed'}`}>
        <a className="skip-link" href="#main-content">Skip to content</a>
        <aside id="desktop-sidebar" className="desktop-sidebar" aria-label="Workspace sidebar"><Sidebar collapsed={!sidebarOpen} /></aside>
        <dialog id="mobile-sidebar" ref={dialogRef} className="mobile-drawer" aria-label="Workspace navigation"
            onCancel={() => dispatch(closeMobileSidebar())} onClose={() => dispatch(closeMobileSidebar())}
            onClick={event => { if (event.target === event.currentTarget) dispatch(closeMobileSidebar()); }}>
            <div className="drawer-content"><button className="icon-button drawer-close" aria-label="Close navigation" onClick={() => dispatch(closeMobileSidebar())}>×</button><Sidebar onNavigate={() => dispatch(closeMobileSidebar())} /></div>
        </dialog>
        <div className="shell-body">
            <Topbar pageTitle={pageTitle} />
            <main id="main-content" ref={mainRef} tabIndex={-1} className="shell-main"><Outlet /></main>
            <footer className="shell-footer"><span>OpsFlow</span><span>One workspace. Connected operations.</span></footer>
        </div>
    </div>;
}
