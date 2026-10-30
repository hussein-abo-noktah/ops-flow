import {Link} from 'react-router';
import {useAppSelector} from '@/app/store/hooks';
import {selectCurrentUser} from '@/modules/auth/store/authSelectors';
import {getVisibleNavigation} from '@/app/navigation/navigation';
import {NavigationIcon} from '@/app/navigation/NavigationIcon';
import type {NavigationItem} from "@/app/navigation/type.ts";

export function OverviewPage() {
    const user = useAppSelector(selectCurrentUser);
    const modules = getVisibleNavigation(user).filter(item => item.path !== '/');
    return <>
        <div className="page-heading"><p className="eyebrow">YOUR WORKSPACE</p><h1>Operations overview</h1><p>Welcome to OpsFlow. Keep your people and operations connected.</p></div>
        <section className="welcome-panel"><div className="welcome-symbol" aria-hidden="true">↗</div><div><span className="eyebrow">READY WHEN YOU ARE</span><h2>A clear view of what comes next.</h2><p>Choose a workspace below to get started. Your available tools reflect your account permissions.</p></div></section>
        <div className="section-heading"><h2>Explore your workspace</h2><span>{modules.length} available modules</span></div>
        <div className="workspace-grid">{modules.map(item => <Link className="workspace-card" key={item.path} to={item.path}>
            <span className="module-icon"><NavigationIcon name={item.icon} /></span><h2>{item.label}</h2><p>{item.label}</p><span className="card-action">Open workspace <span aria-hidden="true">↗</span></span>
        </Link>)}</div>
    </>;
}

export function ModulePage({item}: {item: NavigationItem}) {
    return <><div className="page-heading"><p className="eyebrow">WORKSPACE</p><h1>{item.label}</h1><p>{item.label}</p></div>
        <section className="empty-panel"><span className="module-icon"><NavigationIcon name={item.icon} /></span><span className="status-badge">COMING NEXT</span><h2>Your {item.label.toLowerCase()} workspace</h2><p>Navigation is ready. This module’s management screens will be added in a future step.</p><Link className="text-link" to="/">Back to overview <span aria-hidden="true">→</span></Link></section></>;
}

export function StatusPage({code, title, description}: {code: string; title: string; description: string}) {
    return <section className="empty-panel" role="status"><p className="eyebrow">{code}</p><h1>{title}</h1><p>{description}</p><Link className="text-link" to="/">Back to overview →</Link></section>;
}
