import type {
    PropsWithChildren,
} from 'react';

import {
    useAppSelector,
} from '@/app/store/hooks';

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
            state =>
                state.auth.user
                    ?.permissions
                    .includes(permission)
                ?? false
        );

    if (!allowed) {
        return null;
    }

    return children;
}