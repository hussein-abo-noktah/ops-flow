
import type {AuthUser} from "@/modules/auth/types/auth.types.ts";
import jwtDecode from "jwt-decode";



interface JwtPayload {
    sub: string;
    org_id: string;
    email?: string;
    authorities?: string[];
}

export function decodeUserFromJwt(
    token: string
): AuthUser {

    const payload =
        jwtDecode<JwtPayload>(token);

    const authorities =
        payload.authorities ?? [];

    return {
        id: payload.sub,
        organizationId: payload.org_id,
        email: payload.email,

        roles:
            authorities.filter(
                authority =>
                    authority.startsWith(
                        'ROLE_'
                    )
            ),

        permissions:
            authorities.filter(
                authority =>
                    !authority.startsWith(
                        'ROLE_'
                    )
            ),
    };
}
