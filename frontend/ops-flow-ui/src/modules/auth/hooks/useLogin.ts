import {useNavigate} from "react-router-dom";
import {useAppDispatch} from "@/app/store/hooks.ts";
import {useMutation} from "@tanstack/react-query";
import type {LoginRequest} from "@/modules/auth/types/auth.types.ts";
import {login} from "@/modules/auth/api/authApi.ts";
import {setAuth} from "@/modules/auth/store/authSlice.ts";
import {decodeUserFromJwt} from "@/modules/auth/utils/decodeUserFromJwt.ts";

export function useLogin() {
    const dispatch = useAppDispatch();
    const navigate = useNavigate();


    return useMutation({
        mutationFn: (request: LoginRequest) => login(request),
        onSuccess: (data) => {
            const user = decodeUserFromJwt(
                data.accessToken
            );
            dispatch(setAuth({user, accessToken: data.accessToken}));
            navigate("/", {replace: true});
        },
    })

}
