import {configureStore} from '@reduxjs/toolkit';
import {uiReducer} from "@/app/store/slices/uiSlice.ts";
import {authReducer} from "@/modules/auth/store/authSlice.ts";

export const store = configureStore({
    reducer: {
        ui: uiReducer,
        auth: authReducer,
    },
});


export type RootState = ReturnType<typeof store.getState>;

export type AppDispatch = typeof store.dispatch;