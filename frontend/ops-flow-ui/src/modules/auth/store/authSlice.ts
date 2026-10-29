import type {AuthUser} from "@/modules/auth/types/auth.types.ts";
import {createSlice, type PayloadAction} from "@reduxjs/toolkit";

interface AuthState {
    user: AuthUser | null;

    accessToken:
        string | null;

    status:
        | 'checking'
        | 'authenticated'
        | 'unauthenticated';
}

const initialState: AuthState = {
    user: null,
    accessToken: null,
    status: 'checking',
};

const authSlice = createSlice({
    name: "auth",
    initialState,
    reducers: {
        setAuth(
            state,
            action: PayloadAction<{
                user: AuthUser;
                accessToken: string;
            }>
        ){
            state.user = action.payload.user;
            state.accessToken = action.payload.accessToken;
            state.status = "authenticated";
        },
        clearAuth(
            state
        ){
            state.user = null;
            state.accessToken = null;
            state.status = "unauthenticated";
        },
        updateAccessToken(
            state,
            action: PayloadAction<string>
        ){
            state.accessToken = action.payload;
        },
    },
})

export const {setAuth, clearAuth, updateAccessToken} = authSlice.actions;
export const authReducer = authSlice.reducer;