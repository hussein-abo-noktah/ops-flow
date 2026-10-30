import {Outlet} from 'react-router';
import {useAppSelector} from '@/app/store/hooks';
import {selectHasPermission} from '@/modules/auth/store/authSelectors';
import {StatusPage} from '@/app/views/WorkspacePages';

export function PermissionRoute({permission}: {permission: string}) {
    const allowed = useAppSelector(state => selectHasPermission(state, permission));
    return allowed ? <Outlet /> : <StatusPage code="403" title="Access restricted" description="Your account does not have permission to view this page. Contact your administrator if you need access." />;
}
