import {useAppSelector} from "@/app/store/hooks.ts";
import {selectHasPermission} from '@/modules/auth/store/authSelectors.ts';

export function usePermission( permission :string){
    return useAppSelector(
        state => selectHasPermission(state, permission)
    );
}
