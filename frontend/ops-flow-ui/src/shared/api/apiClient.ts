import axios from "axios";
import {env} from "@/app/config/config.ts";


export const apiClient = axios.create({
    baseURL: env.apiUrl,
    headers: {
        'Content-Type':
            'application/json',
    },

    timeout: 15_000,
})