import {z} from 'zod';


export const loginSchema = z.object({
    email: z.email("Invalid Email").min(1 ,"Email is Required"),
    password: z.string().min(1,"Password is Required")
})

export type LoginFormValues  = z.infer<typeof loginSchema> ;