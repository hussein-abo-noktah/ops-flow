import {NavLink} from 'react-router';
import {useAppSelector} from '@/app/store/hooks';
import {selectCurrentUser} from '@/modules/auth/store/authSelectors';
import {getVisibleNavigation} from './navigation';
import {NavigationIcon} from './NavigationIcon';

export function Sidebar({collapsed = false, onNavigate}: {collapsed?: boolean; onNavigate?: () => void}) {
    const user = useAppSelector(selectCurrentUser);
    return <>
        <NavLink to="/" className="shell-brand" aria-label="OpsFlow home" onClick={onNavigate}>
            <span className="shell-brand-mark" aria-hidden="true">↗</span>
            {!collapsed && <span>OpsFlow<span className="brand-caption">OPERATIONS WORKSPACE</span></span>}
        </NavLink>
        <div className="nav-section-label">{collapsed ? '—' : 'WORKSPACE'}</div>
        <nav aria-label="Main navigation">
            {getVisibleNavigation(user).map(item => <NavLink key={item.path} to={item.path} end={item.path === '/'} onClick={onNavigate}
                title={collapsed ? item.label : undefined} aria-label={collapsed ? item.label : undefined}
                className={({isActive}) => `nav-link${isActive ? ' is-active' : ''}`}>
                <NavigationIcon name={item.icon} />{!collapsed && <span>{item.label}</span>}
            </NavLink>)}
        </nav>
        <div className="sidebar-footer">{collapsed ? <span title="Organization workspace">◈</span> : <><span className="workspace-dot" /> Organization workspace<small>Built for better operations.</small></>}</div>
    </>;
}
