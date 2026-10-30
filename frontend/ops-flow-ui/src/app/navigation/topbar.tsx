import {useState} from 'react';
import {Link, useNavigate} from 'react-router';
import {useAppDispatch, useAppSelector} from '@/app/store/hooks';
import {closeMobileSidebar, openMobileSidebar, toggleSidebar} from '@/app/store/slices/uiSlice';
import {selectCurrentUser} from '@/modules/auth/store/authSelectors';
import {clearAuth} from '@/modules/auth/store/authSlice';
import {logout} from '@/modules/auth/api/authApi';
import {queryClient} from '@/app/providers/queryClient';

interface TopbarProps {
    pageTitle: string;
}

export function Topbar({pageTitle}: TopbarProps) {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();
    const user = useAppSelector(selectCurrentUser);
    const {sidebarOpen, mobileSidebarOpen} = useAppSelector(state => state.ui);
    const [signingOut, setSigningOut] = useState(false);
    const [logoutError, setLogoutError] = useState('');
    const accountName = user?.email || 'Workspace member';

    async function handleLogout() {
        setSigningOut(true);
        setLogoutError('');
        try {
            await logout();
            await queryClient.cancelQueries();
            queryClient.clear();
            dispatch(clearAuth());
            dispatch(closeMobileSidebar());
            navigate('/login', {replace: true});
        } catch {
            setLogoutError('Could not sign out. Please try again.');
        } finally {
            setSigningOut(false);
        }
    }

    return (
        <>
            <header className="shell-header">
                <div className="header-start">
                    <button
                        type="button"
                        className="icon-button desktop-toggle"
                        aria-label={sidebarOpen ? 'Collapse sidebar' : 'Expand sidebar'}
                        aria-expanded={sidebarOpen}
                        aria-controls="desktop-sidebar"
                        onClick={() => dispatch(toggleSidebar())}
                    >
                        <span aria-hidden="true">☰</span>
                    </button>
                    <button
                        type="button"
                        className="icon-button mobile-toggle"
                        aria-label="Open navigation"
                        aria-expanded={mobileSidebarOpen}
                        aria-controls="mobile-sidebar"
                        onClick={() => dispatch(openMobileSidebar())}
                    >
                        <span aria-hidden="true">☰</span>
                    </button>
                    <nav className="breadcrumbs" aria-label="Breadcrumb">
                        <Link to="/">Workspace</Link>
                        <span aria-hidden="true">/</span>
                        <span aria-current="page">{pageTitle}</span>
                    </nav>
                </div>
                <div className="header-account">
                    <span className="account-avatar" aria-hidden="true">
                        {user?.email?.slice(0, 2).toUpperCase() || 'OF'}
                    </span>
                    <div className="account-details">
                        <strong>{accountName}</strong>
                        <small>
                            {user?.roles.includes('ROLE_PLATFORM_ADMIN')
                                ? 'Platform administrator'
                                : 'Organization member'}
                        </small>
                    </div>
                    <button
                        type="button"
                        className="signout-button"
                        disabled={signingOut}
                        onClick={handleLogout}
                    >
                        {signingOut ? 'Signing out…' : 'Sign out'}
                    </button>
                </div>
            </header>
            {logoutError && <p className="shell-error" role="alert">{logoutError}</p>}
        </>
    );
}
