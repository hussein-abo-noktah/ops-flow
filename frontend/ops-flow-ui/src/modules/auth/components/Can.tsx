import type {
    PropsWithChildren,
} from 'react';

import {
    useAppSelector,
} from '@/app/store/hooks';
import {selectHasPermission} from '@/modules/auth/store/authSelectors.ts';

interface CanProps
    extends PropsWithChildren {

    permission: string;
}

export function Can({
                        permission,
                        children,
                    }: CanProps) {

    const allowed =
        useAppSelector(
            state => selectHasPermission(state, permission)
        );

    if (!allowed) {
        return null;
    }

    return children;
}
