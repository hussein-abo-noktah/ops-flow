import axios from "axios";
import {env} from "@/app/config/config.ts";


export const authClient = axios.create({
    baseURL: env.apiUrl,
    timeout: 15_000,
    headers: {
        'Content-Type': 'application/json',
    },

});

