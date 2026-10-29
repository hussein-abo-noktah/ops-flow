import type {PropsWithChildren} from "react";
import {Provider} from "react-redux";
import {store} from "@/app/store/store.ts";
import {QueryClientProvider} from "@tanstack/react-query";
import {queryClient} from "@/app/providers/queryClient.ts";
import {AuthBootstrap} from "@/modules/auth/components/AuthBootstrap.tsx";

export function AppProviders({
                                 children,
                             }: PropsWithChildren) {

    return (
        <Provider store={store}>
            <QueryClientProvider
                client={queryClient}
            >
                <AuthBootstrap>
                    {children}
                </AuthBootstrap>
            </QueryClientProvider>
        </Provider>
    );
}