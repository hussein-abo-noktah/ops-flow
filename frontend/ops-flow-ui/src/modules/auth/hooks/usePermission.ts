import {useAppSelector} from "@/app/store/hooks.ts";

export function usePermission( permission :string){
    return useAppSelector(
        state =>
            state.auth.user
                ?.permissions
                .includes(permission)
            ?? false
    );
}